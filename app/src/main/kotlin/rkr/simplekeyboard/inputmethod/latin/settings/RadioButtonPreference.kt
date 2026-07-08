package rkr.simplekeyboard.inputmethod.latin.settings

import android.content.Context
import android.preference.Preference
import android.util.AttributeSet
import android.view.View
import android.widget.RadioButton
import rkr.simplekeyboard.inputmethod.R

class RadioButtonPreference @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = android.R.attr.preferenceStyle
) : Preference(context, attrs, defStyleAttr) {

    interface OnRadioButtonClickedListener {
        fun onRadioButtonClicked(preference: RadioButtonPreference)
    }

    private var mIsSelected = false
    private var mRadioButton: RadioButton? = null
    private var mListener: OnRadioButtonClickedListener? = null
    private val mClickListener = View.OnClickListener { callListenerOnRadioButtonClicked() }

    init {
        widgetLayoutResource = R.layout.radio_button_preference_widget
    }

    fun setOnRadioButtonClickedListener(listener: OnRadioButtonClickedListener?) {
        mListener = listener
    }

    fun callListenerOnRadioButtonClicked() {
        mListener?.onRadioButtonClicked(this)
    }

    override fun onBindView(view: View) {
        super.onBindView(view)
        mRadioButton = view.findViewById(R.id.radio_button)
        mRadioButton?.isChecked = mIsSelected
        mRadioButton?.setOnClickListener(mClickListener)
        view.setOnClickListener(mClickListener)
    }

    fun setSelected(selected: Boolean) {
        if (selected == mIsSelected) return
        mIsSelected = selected
        mRadioButton?.isChecked = selected
        notifyChanged()
    }
}
