package rkr.simplekeyboard.inputmethod.latin

import android.text.InputType
import android.util.Log
import android.view.inputmethod.EditorInfo
import rkr.simplekeyboard.inputmethod.latin.utils.InputTypeUtils

class InputAttributes(editorInfo: EditorInfo?, isFullscreenMode: Boolean) {
    val mTargetApplicationPackageName: String? = editorInfo?.packageName
    val mInputTypeNoAutoCorrect: Boolean
    val mIsPasswordField: Boolean
    val mShouldShowSuggestions: Boolean
    val mApplicationSpecifiedCompletionOn: Boolean
    val mShouldInsertSpacesAutomatically: Boolean
    private val mInputType: Int

    init {
        val inputType = editorInfo?.inputType ?: 0
        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        mInputType = inputType
        mIsPasswordField = InputTypeUtils.isPasswordInputType(inputType) || InputTypeUtils.isVisiblePasswordInputType(inputType)

        if (inputClass != InputType.TYPE_CLASS_TEXT) {
            mShouldShowSuggestions = false
            mInputTypeNoAutoCorrect = false
            mApplicationSpecifiedCompletionOn = false
            mShouldInsertSpacesAutomatically = false
        } else {
            val variation = inputType and InputType.TYPE_MASK_VARIATION
            val flagNoSuggestions = inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS != 0
            val flagMultiLine = inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE != 0
            val flagAutoCorrect = inputType and InputType.TYPE_TEXT_FLAG_AUTO_CORRECT != 0
            val flagAutoComplete = inputType and InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE != 0
            val shouldSuppressSuggestions = mIsPasswordField || InputTypeUtils.isEmailVariation(variation) ||
                    InputType.TYPE_TEXT_VARIATION_URI == variation ||
                    InputType.TYPE_TEXT_VARIATION_FILTER == variation ||
                    flagNoSuggestions || flagAutoComplete
            mShouldShowSuggestions = !shouldSuppressSuggestions
            mShouldInsertSpacesAutomatically = InputTypeUtils.isAutoSpaceFriendlyType(inputType)
            mInputTypeNoAutoCorrect = (variation == InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT && !flagAutoCorrect) ||
                    flagNoSuggestions || (!flagAutoCorrect && !flagMultiLine)
            mApplicationSpecifiedCompletionOn = flagAutoComplete && isFullscreenMode
        }
    }

    fun isSameInputType(editorInfo: EditorInfo): Boolean = editorInfo.inputType == mInputType

    override fun toString(): String {
        return String.format(
            "%s: inputType=0x%08x%s%s%s%s%s targetApp=%s\n", javaClass.simpleName,
            mInputType,
            if (mInputTypeNoAutoCorrect) " noAutoCorrect" else "",
            if (mIsPasswordField) " password" else "",
            if (mShouldShowSuggestions) " shouldShowSuggestions" else "",
            if (mApplicationSpecifiedCompletionOn) " appSpecified" else "",
            if (mShouldInsertSpacesAutomatically) " insertSpaces" else "",
            mTargetApplicationPackageName
        )
    }
}
