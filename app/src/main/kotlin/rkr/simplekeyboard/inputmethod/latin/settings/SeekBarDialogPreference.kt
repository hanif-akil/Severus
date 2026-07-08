package rkr.simplekeyboard.inputmethod.latin.settings

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.preference.DialogPreference
import android.util.AttributeSet
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import rkr.simplekeyboard.inputmethod.R

class SeekBarDialogPreference(context: Context, attrs: AttributeSet) : DialogPreference(context, attrs), SeekBar.OnSeekBarChangeListener {

    interface ValueProxy {
        fun readValue(key: String): Int
        fun readDefaultValue(key: String): Int
        fun writeValue(value: Int, key: String)
        fun writeDefaultValue(key: String)
        fun getValueText(value: Int): String
        fun feedbackValue(value: Int)
    }

    private val mMaxValue: Int
    private val mMinValue: Int
    private val mStepValue: Int
    private var mValueView: TextView? = null
    private var mSeekBar: SeekBar? = null
    private var mValueProxy: ValueProxy? = null

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.SeekBarDialogPreference, 0, 0)
        mMaxValue = a.getInt(R.styleable.SeekBarDialogPreference_maxValue, 0)
        mMinValue = a.getInt(R.styleable.SeekBarDialogPreference_minValue, 0)
        mStepValue = a.getInt(R.styleable.SeekBarDialogPreference_stepValue, 0)
        a.recycle()
        dialogLayoutResource = R.layout.seek_bar_dialog
    }

    fun setInterface(proxy: ValueProxy?) {
        mValueProxy = proxy
        val value = mValueProxy!!.readValue(key)
        summary = mValueProxy!!.getValueText(value)
    }

    override fun onCreateDialogView(): View {
        val view = super.onCreateDialogView()
        mSeekBar = view.findViewById(R.id.seek_bar_dialog_bar)
        mSeekBar?.max = mMaxValue - mMinValue
        mSeekBar?.setOnSeekBarChangeListener(this)
        mValueView = view.findViewById(R.id.seek_bar_dialog_value)
        return view
    }

    private fun getProgressFromValue(value: Int): Int = value - mMinValue
    private fun getValueFromProgress(progress: Int): Int = progress + mMinValue

    private fun clipValue(value: Int): Int {
        val clippedValue = minOf(mMaxValue, maxOf(mMinValue, value))
        return if (mStepValue <= 1) clippedValue else clippedValue - (clippedValue % mStepValue)
    }

    private fun getClippedValueFromProgress(progress: Int): Int = clipValue(getValueFromProgress(progress))

    override fun onBindDialogView(view: View) {
        val value = mValueProxy!!.readValue(key)
        mValueView?.text = mValueProxy!!.getValueText(value)
        mSeekBar?.progress = getProgressFromValue(clipValue(value))
    }

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        builder.setPositiveButton(android.R.string.ok, this)
            .setNegativeButton(android.R.string.cancel, this)
            .setNeutralButton(R.string.button_default, this)
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        super.onClick(dialog, which)
        if (which == DialogInterface.BUTTON_NEUTRAL) {
            val value = mValueProxy!!.readDefaultValue(key)
            summary = mValueProxy!!.getValueText(value)
            mValueProxy!!.writeDefaultValue(key)
            return
        }
        if (which == DialogInterface.BUTTON_POSITIVE) {
            val value = getClippedValueFromProgress(mSeekBar?.progress ?: 0)
            summary = mValueProxy!!.getValueText(value)
            mValueProxy!!.writeValue(value, key)
            return
        }
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        val value = getClippedValueFromProgress(progress)
        mValueView?.text = mValueProxy!!.getValueText(value)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {}
    override fun onStopTrackingTouch(seekBar: SeekBar) {
        mValueProxy?.feedbackValue(getClippedValueFromProgress(seekBar.progress))
    }
}
