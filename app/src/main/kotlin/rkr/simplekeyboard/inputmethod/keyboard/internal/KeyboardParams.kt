package rkr.simplekeyboard.inputmethod.keyboard.internal

import rkr.simplekeyboard.inputmethod.keyboard.Key
import rkr.simplekeyboard.inputmethod.keyboard.KeyboardId

open class KeyboardParams {
    var mId: KeyboardId? = null
    var mKeyboardThemeId = 0
    var mGridWidth = 0
    var mGridHeight = 0
    var mOccupiedWidth = 0
    var mOccupiedHeight = 0
    var mBaseWidth = 0
    var mBaseHeight = 0
    var mHorizontalGap = 0f
    var mVerticalGap = 0f
    var mTopPadding = 0
    var mBottomPadding = 0
    var mLeftPadding = 0
    var mRightPadding = 0
    var mDefaultKeyPaddedWidth = 0f
    var mDefaultRowHeight = 0
    var mDefaultKeyWidth = 0f
    var mDefaultKeyHeight = 0f
    var mDefaultKeyBackgroundType = 0
    var mDefaultKeyLabelFlags = 0
    var mKeyLabelScaleX = 1.0f
    var mKeyVisualAttributes: KeyVisualAttributes? = null
    var mMoreKeysTemplate = 0
    var mMaxMoreKeysKeyboardColumn = 0
    var mNoSettingsKey = false
    var mLanguageSwitchKeyEnabled = false
    var mShowMoreKeys = false
    var mShowNumberRow = false
    var mAllowRedundantMoreKeys = false
    var mMostCommonKeyHeight = 0
    var mMostCommonKeyWidth = 0
    var mKeyboardWidth = 0
    var mKeyboardHeight = 0
    var mKeyboardBottomOffset = 0
    var mMode = 0
    var mEditorInfo: android.view.inputmethod.EditorInfo? = null

    val mIconsSet = KeyboardIconsSet()
    val mTextsSet = KeyboardTextsSet()
    val mKeyStyles: KeyStylesSet = KeyStylesSet(mTextsSet)
    val mSortedKeys = ArrayList<Key>()
    val mShiftKeys = ArrayList<Key>()
    val mAltCodeKeysWhileTyping = ArrayList<Key>()

    fun onAddKey(key: Key) {
        mSortedKeys.add(key)
        if (key.isShift) mShiftKeys.add(key)
        if (key.altCodeWhileTyping) mAltCodeKeysWhileTyping.add(key)
    }

    fun removeRedundantMoreKeys() {
        if (mAllowRedundantMoreKeys) return
    }
}
