package rkr.simplekeyboard.inputmethod.event

import rkr.simplekeyboard.inputmethod.latin.settings.SettingsValues
import kotlin.math.max

class InputTransaction(val mSettingsValues: SettingsValues) {
    companion object {
        const val SHIFT_NO_UPDATE = 0
        const val SHIFT_UPDATE_NOW = 1
        const val SHIFT_UPDATE_LATER = 2
    }

    private var mRequiredShiftUpdate = SHIFT_NO_UPDATE

    fun requireShiftUpdate(updateType: Int) {
        mRequiredShiftUpdate = max(mRequiredShiftUpdate, updateType)
    }

    fun getRequiredShiftUpdate(): Int = mRequiredShiftUpdate
}
