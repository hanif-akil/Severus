package rkr.simplekeyboard.inputmethod.keyboard.internal

import rkr.simplekeyboard.inputmethod.keyboard.Keyboard
import rkr.simplekeyboard.inputmethod.keyboard.KeyboardId
import rkr.simplekeyboard.inputmethod.keyboard.KeyboardTheme
import rkr.simplekeyboard.inputmethod.latin.utils.ResourceUtils

class KeyboardParams {
    var mId: KeyboardId? = null
    var mKeyboardThemeId = 0
    var mKeyboardWidth = 0
    var mKeyboardHeight = 0
    var mKeyboardBottomOffset = 0
    var mMode = 0
    var mEditorInfo: android.view.inputmethod.EditorInfo? = null
    var mNoSettingsKey = false
    var mLanguageSwitchKeyEnabled = false
    var mShowMoreKeys = false
    var mShowNumberRow = false
    var mMaxMoreKeysKeyboardColumn = 0
    var mTopPadding = 0
    var mBottomPadding = 0
    var mLeftPadding = 0
    var mRightPadding = 0
    var mVerticalGap = 0
    var mKeyLabelScaleX = 1.0f
    var mDefaultKeyLabelFlags = 0
    var mDefaultKeyWidth = 0f
    var mDefaultKeyHeight = 0f
    var mDefaultKeyBackgroundType = 0
}
