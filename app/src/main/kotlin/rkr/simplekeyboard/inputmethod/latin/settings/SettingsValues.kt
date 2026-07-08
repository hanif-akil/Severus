package rkr.simplekeyboard.inputmethod.latin.settings

import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.view.inputmethod.EditorInfo
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.latin.InputAttributes

open class SettingsValues(prefs: SharedPreferences, res: Resources, val mInputAttributes: InputAttributes) {
    val mSpacingAndPunctuations = SpacingAndPunctuations(res)
    val mHasHardwareKeyboard: Boolean = Settings.readHasHardwareKeyboard(res.configuration)
    val mDisplayOrientation: Int = res.configuration.orientation
    val mAutoCap: Boolean = prefs.getBoolean(Settings.PREF_AUTO_CAP, true)
    val mVibrateOn: Boolean = Settings.readVibrationEnabled(prefs, res)
    val mSoundOn: Boolean = Settings.readKeypressSoundEnabled(prefs, res)
    val mKeyPreviewPopupOn: Boolean = Settings.readKeyPreviewPopupEnabled(prefs, res)
    val mUseOnScreen: Boolean = Settings.readUseOnScreenKeyboard(prefs)
    val mShowsLanguageSwitchKey: Boolean = Settings.readShowLanguageSwitchKey(prefs)
    val mImeSwitchEnabled: Boolean = Settings.readEnableImeSwitch(prefs)
    val mKeyLongpressTimeout: Int = Settings.readKeyLongpressTimeout(prefs, res)
    val mShowSpecialChars: Boolean = Settings.readShowSpecialChars(prefs)
    val mShowNumberRow: Boolean = Settings.readShowNumberRow(prefs)
    val mSpaceSwipeEnabled: Boolean = Settings.readSpaceSwipeEnabled(prefs)
    val mDeleteSwipeEnabled: Boolean = Settings.readDeleteSwipeEnabled(prefs)
    val mKeypressSoundVolume: Float = Settings.readKeypressSoundVolume(prefs)
    val mKeyPreviewPopupDismissDelay: Int = res.getInteger(R.integer.config_key_preview_linger_timeout)
    val mKeyboardHeightScale: Float = Settings.readKeyboardHeight(prefs, DEFAULT_SIZE_SCALE)
    val mBottomOffsetPortrait: Int = Settings.readBottomOffsetPortrait(prefs)

    fun isWordSeparator(code: Int): Boolean = mSpacingAndPunctuations.isWordSeparator(code)
    fun isLanguageSwitchKeyDisabled(): Boolean = !mShowsLanguageSwitchKey
    fun isSameInputType(editorInfo: EditorInfo): Boolean = mInputAttributes.isSameInputType(editorInfo)
    fun hasSameOrientation(configuration: Configuration): Boolean = mDisplayOrientation == configuration.orientation

    companion object {
        const val DEFAULT_SIZE_SCALE = 1.0f
    }
}
