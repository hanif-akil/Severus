package rkr.simplekeyboard.inputmethod.latin.settings

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.PorterDuff
import android.preference.DialogPreference
import android.util.AttributeSet
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import rkr.simplekeyboard.inputmethod.R

class ColorDialogPreference(context: Context, attrs: AttributeSet) : DialogPreference(context, attrs), SeekBar.OnSeekBarChangeListener {

    interface ValueProxy {
        fun readValue(key: String): Int
        fun writeDefaultValue(key: String)
        fun writeValue(value: Int, key: String)
    }

    private var mValueView: TextView? = null
    private var mSeekBarRed: SeekBar? = null
    private var mSeekBarGreen: SeekBar? = null
    private var mSeekBarBlue: SeekBar? = null
    private var mValueProxy: ValueProxy? = null

    init {
        dialogLayoutResource = R.layout.color_dialog
    }

    fun setInterface(proxy: ValueProxy?) {
        mValueProxy = proxy
    }

    override fun onCreateDialogView(): View {
        val view = super.onCreateDialogView()
        mSeekBarRed = view.findViewById<SeekBar>(R.id.seek_bar_dialog_bar_red).apply {
            max = 255
            setOnSeekBarChangeListener(this@ColorDialogPreference)
            progressDrawable.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)
            thumb.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)
        }
        mSeekBarGreen = view.findViewById<SeekBar>(R.id.seek_bar_dialog_bar_green).apply {
            max = 255
            setOnSeekBarChangeListener(this@ColorDialogPreference)
            thumb.setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN)
            progressDrawable.setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN)
        }
        mSeekBarBlue = view.findViewById<SeekBar>(R.id.seek_bar_dialog_bar_blue).apply {
            max = 255
            setOnSeekBarChangeListener(this@ColorDialogPreference)
            thumb.setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_IN)
            progressDrawable.setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_IN)
        }
        mValueView = view.findViewById(R.id.seek_bar_dialog_value)
        return view
    }

    override fun onBindDialogView(view: View) {
        val color = mValueProxy!!.readValue(key)
        mSeekBarRed?.progress = Color.red(color)
        mSeekBarGreen?.progress = Color.green(color)
        mSeekBarBlue?.progress = Color.blue(color)
        setHeaderText(color)
    }

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        builder.setPositiveButton(android.R.string.ok, this)
            .setNegativeButton(android.R.string.cancel, this)
            .setNeutralButton(R.string.button_default, this)
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        super.onClick(dialog, which)
        if (which == DialogInterface.BUTTON_POSITIVE) {
            super.onClick(dialog, which)
            val value = Color.rgb(mSeekBarRed?.progress ?: 0, mSeekBarGreen?.progress ?: 0, mSeekBarBlue?.progress ?: 0)
            mValueProxy?.writeValue(value, key)
            return
        }
        if (which == DialogInterface.BUTTON_NEUTRAL) {
            super.onClick(dialog, which)
            mValueProxy?.writeDefaultValue(key)
            return
        }
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        val color = Color.rgb(mSeekBarRed?.progress ?: 0, mSeekBarGreen?.progress ?: 0, mSeekBarBlue?.progress ?: 0)
        setHeaderText(color)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {}
    override fun onStopTrackingTouch(seekBar: SeekBar) {}

    private fun setHeaderText(color: Int) {
        mValueView?.text = getValueText(color)
        val bright = Color.red(color) + Color.green(color) + Color.blue(color) > 128 * 3
        mValueView?.setTextColor(if (bright) Color.BLACK else Color.WHITE)
        mValueView?.setBackgroundColor(color)
    }

    private fun getValueText(value: Int): String {
        var temp = Integer.toHexString(value)
        while (temp.length < 8) temp = "0$temp"
        return temp.substring(2).uppercase()
    }
}
