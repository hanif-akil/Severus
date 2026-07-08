package rkr.simplekeyboard.inputmethod.keyboard.internal

import android.content.res.Resources
import android.content.res.TypedArray
import android.util.Xml
import rkr.simplekeyboard.inputmethod.keyboard.Key
import org.xmlpull.v1.XmlPullParser
import rkr.simplekeyboard.inputmethod.R

class KeyboardRow(private val mResources: Resources, private val mParams: KeyboardParams, parser: XmlPullParser, currentY: Float) {
    private var mRowX = 0f
    private var mRowY = currentY
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
    private var mRowHeight = 0f

    private val mDefaultKeyAttr: TypedArray
    private val mRowKeyAttrStack = ArrayList<TypedArray>()

    init {
        val attr = Xml.asAttributeSet(parser)
        val rowKeyAttr = mResources.obtainAttributes(attr, R.styleable.Keyboard_Key)
        mDefaultKeyAttr = rowKeyAttr

        mRowX = mParams.mLeftPadding.toFloat()
        mRowY += mParams.mVerticalGap.toFloat()
        mRowWidth = mParams.mOccupiedWidth - mParams.mLeftPadding - mParams.mRightPadding
        mRowKeyWidth = mParams.mDefaultKeyPaddedWidth
        mRowKeyHeight = rowKeyAttr.getDimension(R.styleable.Keyboard_Key_keyHeight, mParams.mDefaultRowHeight.toFloat())
        mRowLeftPadding = rowKeyAttr.getDimension(R.styleable.Keyboard_Key_keyLeftPadding, mParams.mLeftPadding.toFloat())
        mRowRightPadding = rowKeyAttr.getDimension(R.styleable.Keyboard_Key_keyRightPadding, mParams.mRightPadding.toFloat())
        mRowTopPadding = rowKeyAttr.getDimension(R.styleable.Keyboard_Key_keyTopPadding, mParams.mTopPadding.toFloat())
        mRowBottomPadding = rowKeyAttr.getDimension(R.styleable.Keyboard_Key_keyBottomPadding, mParams.mBottomPadding.toFloat())
        mRowDefaultBackgroundType = rowKeyAttr.getInt(R.styleable.Keyboard_Key_keyBackgroundType, Key.BACKGROUND_TYPE_NORMAL)
        mRowDefaultKeyLabelFlags = rowKeyAttr.getInt(R.styleable.Keyboard_Key_keyLabelFlags, 0)
        mRowHeight = mRowKeyHeight - mRowTopPadding - mRowBottomPadding + mParams.mVerticalGap
    }

    val rowHeight: Float get() = mRowHeight

    fun setCurrentKey(keyAttr: TypedArray, isSpacer: Boolean) {
        if (!isSpacer) {
            val keyWidth = keyAttr.getDimension(R.styleable.Keyboard_Key_keyWidth, mRowKeyWidth)
            if (keyWidth > 0f) {
                mRowKeyWidth = keyWidth
            }
        }
    }

    fun updateXPos(keyAttr: TypedArray) {
        val keyWidth = keyAttr.getDimension(R.styleable.Keyboard_Key_keyWidth, mRowKeyWidth)
        if (keyWidth > 0f) {
            mRowKeyWidth = keyWidth
        }
    }

    fun pushRowAttributes(keyAttr: TypedArray) {
        mRowKeyAttrStack.add(mDefaultKeyAttr)
    }

    fun popRowAttributes() {
        if (mRowKeyAttrStack.isNotEmpty()) {
            val restored = mRowKeyAttrStack.removeAt(mRowKeyAttrStack.size - 1)
            mRowKeyWidth = restored.getDimension(R.styleable.Keyboard_Key_keyWidth, mParams.mDefaultKeyPaddedWidth)
            mRowLeftPadding = restored.getDimension(R.styleable.Keyboard_Key_keyLeftPadding, mParams.mLeftPadding.toFloat())
            mRowRightPadding = restored.getDimension(R.styleable.Keyboard_Key_keyRightPadding, mParams.mRightPadding.toFloat())
            mRowTopPadding = restored.getDimension(R.styleable.Keyboard_Key_keyTopPadding, mParams.mTopPadding.toFloat())
            mRowBottomPadding = restored.getDimension(R.styleable.Keyboard_Key_keyBottomPadding, mParams.mBottomPadding.toFloat())
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
