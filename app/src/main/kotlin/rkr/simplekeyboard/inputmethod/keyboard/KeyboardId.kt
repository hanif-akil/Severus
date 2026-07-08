package rkr.simplekeyboard.inputmethod.keyboard

import android.text.InputType
import android.text.TextUtils
import android.view.inputmethod.EditorInfo
import rkr.simplekeyboard.inputmethod.compat.EditorInfoCompatUtils
import rkr.simplekeyboard.inputmethod.latin.Subtype
import rkr.simplekeyboard.inputmethod.latin.utils.InputTypeUtils
import java.util.Arrays
import java.util.Locale

internal class KeyboardId(
    val mElementId: Int,
    params: KeyboardLayoutSet.Params
) {
    val mSubtype: Subtype = params.mSubtype!!
    val mThemeId: Int = params.mKeyboardThemeId
    val mWidth: Int = params.mKeyboardWidth
    val mHeight: Int = params.mKeyboardHeight
    val mBottomOffset: Int = params.mKeyboardBottomOffset
    val mMode: Int = params.mMode
    val mEditorInfo: EditorInfo = params.mEditorInfo!!
    val mClobberSettingsKey: Boolean = params.mNoSettingsKey
    val mLanguageSwitchKeyEnabled: Boolean = params.mLanguageSwitchKeyEnabled
    val mCustomActionLabel: String? = if (params.mEditorInfo?.actionLabel != null) {
        params.mEditorInfo!!.actionLabel.toString()
    } else null
    val mShowMoreKeys: Boolean = params.mShowMoreKeys
    val mShowNumberRow: Boolean = params.mShowNumberRow

    private val mHashCode: Int = computeHashCode()

    private fun computeHashCode(): Int {
        return Arrays.hashCode(
            arrayOf(
                mElementId, mMode, mWidth, mHeight, mBottomOffset,
                passwordInput(), mClobberSettingsKey, mLanguageSwitchKeyEnabled,
                isMultiLine(), imeAction(), mCustomActionLabel,
                navigateNext(), navigatePrevious(), mSubtype, mThemeId
            )
        )
    }

    private fun equals(other: KeyboardId): Boolean {
        if (other === this) return true
        return other.mElementId == mElementId &&
                other.mMode == mMode &&
                other.mWidth == mWidth &&
                other.mHeight == mHeight &&
                other.mBottomOffset == mBottomOffset &&
                other.passwordInput() == passwordInput() &&
                other.mClobberSettingsKey == mClobberSettingsKey &&
                other.mLanguageSwitchKeyEnabled == mLanguageSwitchKeyEnabled &&
                other.isMultiLine() == isMultiLine() &&
                other.imeAction() == imeAction() &&
                TextUtils.equals(other.mCustomActionLabel, mCustomActionLabel) &&
                other.navigateNext() == navigateNext() &&
                other.navigatePrevious() == navigatePrevious() &&
                other.mSubtype == mSubtype &&
                other.mThemeId == mThemeId
    }

    fun isAlphabetKeyboard(): Boolean = isAlphabetKeyboard(mElementId)

    fun navigateNext(): Boolean {
        return (mEditorInfo.imeOptions and EditorInfo.IME_FLAG_NAVIGATE_NEXT) != 0 ||
                imeAction() == EditorInfo.IME_ACTION_NEXT
    }

    fun navigatePrevious(): Boolean {
        return (mEditorInfo.imeOptions and EditorInfo.IME_FLAG_NAVIGATE_PREVIOUS) != 0 ||
                imeAction() == EditorInfo.IME_ACTION_PREVIOUS
    }

    fun passwordInput(): Boolean {
        val inputType = mEditorInfo.inputType
        return InputTypeUtils.isPasswordInputType(inputType) ||
                InputTypeUtils.isVisiblePasswordInputType(inputType)
    }

    fun isMultiLine(): Boolean {
        return (mEditorInfo.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
    }

    fun imeAction(): Int = InputTypeUtils.getImeOptionsActionIdFromEditorInfo(mEditorInfo)

    fun getLocale(): Locale? = mSubtype.localeObject

    override fun equals(other: Any?): Boolean {
        return other is KeyboardId && equals(other)
    }

    override fun hashCode(): Int = mHashCode

    override fun toString(): String {
        return String.format(
            Locale.ROOT, "[%s %s:%s %dx%d +%d %s %s%s%s%s%s%s %s]",
            elementIdToName(mElementId),
            mSubtype.locale,
            mSubtype.keyboardLayoutSet,
            mWidth, mHeight, mBottomOffset,
            modeName(mMode),
            actionName(imeAction()),
            if (navigateNext()) " navigateNext" else "",
            if (navigatePrevious()) " navigatePrevious" else "",
            if (mClobberSettingsKey) " clobberSettingsKey" else "",
            if (passwordInput()) " passwordInput" else "",
            if (mLanguageSwitchKeyEnabled) " languageSwitchKeyEnabled" else "",
            if (isMultiLine()) " isMultiLine" else "",
            KeyboardTheme.getKeyboardThemeName(mThemeId)
        )
    }

    companion object {
        const val MODE_TEXT = 0
        const val MODE_URL = 1
        const val MODE_EMAIL = 2
        const val MODE_IM = 3
        const val MODE_PHONE = 4
        const val MODE_NUMBER = 5
        const val MODE_DATE = 6
        const val MODE_TIME = 7
        const val MODE_DATETIME = 8

        const val ELEMENT_ALPHABET = 0
        const val ELEMENT_ALPHABET_MANUAL_SHIFTED = 1
        const val ELEMENT_ALPHABET_AUTOMATIC_SHIFTED = 2
        const val ELEMENT_ALPHABET_SHIFT_LOCKED = 3
        const val ELEMENT_SYMBOLS = 5
        const val ELEMENT_SYMBOLS_SHIFTED = 6
        const val ELEMENT_PHONE = 7
        const val ELEMENT_PHONE_SYMBOLS = 8
        const val ELEMENT_NUMBER = 9

        private fun isAlphabetKeyboard(elementId: Int): Boolean = elementId < ELEMENT_SYMBOLS

        fun equivalentEditorInfoForKeyboard(a: EditorInfo?, b: EditorInfo?): Boolean {
            if (a == null && b == null) return true
            if (a == null || b == null) return false
            return a.inputType == b.inputType &&
                    a.imeOptions == b.imeOptions &&
                    TextUtils.equals(a.privateImeOptions, b.privateImeOptions)
        }

        fun elementIdToName(elementId: Int): String? = when (elementId) {
            ELEMENT_ALPHABET -> "alphabet"
            ELEMENT_ALPHABET_MANUAL_SHIFTED -> "alphabetManualShifted"
            ELEMENT_ALPHABET_AUTOMATIC_SHIFTED -> "alphabetAutomaticShifted"
            ELEMENT_ALPHABET_SHIFT_LOCKED -> "alphabetShiftLocked"
            ELEMENT_SYMBOLS -> "symbols"
            ELEMENT_SYMBOLS_SHIFTED -> "symbolsShifted"
            ELEMENT_PHONE -> "phone"
            ELEMENT_PHONE_SYMBOLS -> "phoneSymbols"
            ELEMENT_NUMBER -> "number"
            else -> null
        }

        fun modeName(mode: Int): String? = when (mode) {
            MODE_TEXT -> "text"
            MODE_URL -> "url"
            MODE_EMAIL -> "email"
            MODE_IM -> "im"
            MODE_PHONE -> "phone"
            MODE_NUMBER -> "number"
            MODE_DATE -> "date"
            MODE_TIME -> "time"
            MODE_DATETIME -> "datetime"
            else -> null
        }

        fun actionName(actionId: Int): String {
            return if (actionId == InputTypeUtils.IME_ACTION_CUSTOM_LABEL) "actionCustomLabel"
            else EditorInfoCompatUtils.imeActionName(actionId)
        }
    }
}
