package rkr.simplekeyboard.inputmethod.keyboard.internal

import android.content.res.TypedArray
import rkr.simplekeyboard.inputmethod.R

class KeyboardRow(private val mParams: KeyboardParams, keyAttr: TypedArray) {
    private var mRowX = 0f
    private var mRowY = 0f
    private var mRowWidth = 0
    private var mRowKeyWidth = 0f
    private var mRowKeyHeight = 0f
    private var mRowLeftPadding = 0f
    private var mRowRightPadding = 0f
    private var mRowTopPadding = 0f
    private var mRowBottomPadding = 0f
    private var mRowDefaultBackgroundType = 0
    private var mRowDefaultKeyLabelFlags = 0
    private var mRowKeys = 0
    private var mRowFixedWidth = 0f
    private var mRowHasActionKey = false

    init {
        mRowX = mParams.mLeftPadding.toFloat()
        mRowY += mParams.mVerticalGap.toFloat()
        mRowWidth = mParams.mKeyboardWidth - mParams.mLeftPadding - mParams.mRightPadding
        mRowKeyWidth = mParams.mDefaultKeyWidth
        mRowKeyHeight = keyAttr.getDimension(R.styleable.Keyboard_Key_keyHeight, mParams.mDefaultKeyHeight)
        mRowLeftPadding = keyAttr.getDimension(R.styleable.Keyboard_Key_keyLeftPadding, mParams.mLeftPadding.toFloat())
        mRowRightPadding = keyAttr.getDimension(R.styleable.Keyboard_Key_keyRightPadding, mParams.mRightPadding.toFloat())
        mRowTopPadding = keyAttr.getDimension(R.styleable.Keyboard_Key_keyTopPadding, mParams.mTopPadding.toFloat())
        mRowBottomPadding = keyAttr.getDimension(R.styleable.Keyboard_Key_keyBottomPadding, mParams.mBottomPadding.toFloat())
        mRowDefaultBackgroundType = keyAttr.getInt(R.styleable.Keyboard_Key_keyBackgroundType, Key.BACKGROUND_TYPE_NORMAL)
        mRowDefaultKeyLabelFlags = keyAttr.getInt(R.styleable.Keyboard_Key_keyLabelFlags, 0)
    }

    fun setCurrentKey(keyAttr: TypedArray, isSpacer: Boolean) {
        val keyWidth = keyAttr.getDimension(R.styleable.Keyboard_Key_keyWidth, mRowKeyWidth)
        if (keyWidth > 0f) {
            mRowKeyWidth = keyWidth
        }
    }

    fun getKeyX(): Float = mRowX

    fun getKeyY(): Float = mRowY

    fun getKeyWidth(): Float = mRowKeyWidth

    fun getKeyHeight(): Float = mRowKeyHeight

    fun getKeyLeftPadding(): Float = mRowLeftPadding

    fun getKeyRightPadding(): Float = mRowRightPadding

    fun getKeyTopPadding(): Float = mRowTopPadding

    fun getKeyBottomPadding(): Float = mRowBottomPadding

    fun getDefaultBackgroundType(): Int = mRowDefaultBackgroundType

    fun getDefaultKeyLabelFlags(): Int = mRowDefaultKeyLabelFlags
}
