/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2017 Raimondas Rimkus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rkr.simplekeyboard.inputmethod.latin.settings

import android.content.Context
import android.preference.Preference
import android.util.AttributeSet
import android.view.View
import android.widget.RadioButton
import rkr.simplekeyboard.inputmethod.R

open class RadioButtonPreference : Preference {

    interface OnRadioButtonClickedListener {
        fun onRadioButtonClicked(preference: RadioButtonPreference)
    }

    private var mIsSelected = false
    private var mRadioButton: RadioButton? = null
    private var mListener: OnRadioButtonClickedListener? = null

    private val mClickListener = View.OnClickListener { callListenerOnRadioButtonClicked() }

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, android.R.attr.preferenceStyle)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        widgetLayoutResource = R.layout.radio_button_preference_widget
    }

    fun setOnRadioButtonClickedListener(listener: OnRadioButtonClickedListener) {
        mListener = listener
    }

    internal fun callListenerOnRadioButtonClicked() {
        mListener?.onRadioButtonClicked(this)
    }

    override fun onBindView(view: View) {
        super.onBindView(view)
        mRadioButton = view.findViewById<View>(R.id.radio_button) as RadioButton
        mRadioButton!!.isChecked = mIsSelected
        mRadioButton!!.setOnClickListener(mClickListener)
        view.setOnClickListener(mClickListener)
    }

    fun setSelected(selected: Boolean) {
        if (selected == mIsSelected) return
        mIsSelected = selected
        mRadioButton?.isChecked = selected
        notifyChanged()
    }
}
