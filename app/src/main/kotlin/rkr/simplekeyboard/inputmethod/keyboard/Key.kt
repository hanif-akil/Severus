package rkr.simplekeyboard.inputmethod.keyboard

import android.content.res.TypedArray
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.TextUtils
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeyDrawParams
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeySpecParser
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeyStyle
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeyVisualAttributes
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeyboardIconsSet
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeyboardParams
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeyboardRow
import rkr.simplekeyboard.inputmethod.keyboard.internal.MoreKeySpec
import rkr.simplekeyboard.inputmethod.latin.common.Constants
import rkr.simplekeyboard.inputmethod.latin.common.Constants.CODE_OUTPUT_TEXT
import rkr.simplekeyboard.inputmethod.latin.common.Constants.CODE_SHIFT
import rkr.simplekeyboard.inputmethod.latin.common.Constants.CODE_SWITCH_ALPHA_SYMBOL
import rkr.simplekeyboard.inputmethod.latin.common.Constants.CODE_UNSPECIFIED
import rkr.simplekeyboard.inputmethod.latin.common.StringUtils
import java.util.Arrays

open class Key : Comparable<Key> {
    private val mCode: Int
    private val mLabel: String?
    private val mHintLabel: String?
    private val mLabelFlags: Int
    private val mIconId: Int
    private val mWidth: Int
    private val mHeight: Int
    private val mDefinedWidth: Float
    private val mDefinedHeight: Float
    private val mX: Int
    private val mY: Int
    private val mHitbox = Rect()
    private val mMoreKeys: Array<MoreKeySpec>?
    private val mMoreKeysColumnAndFlags: Int
    private val mBackgroundType: Int
    private val mActionFlags: Int
    private val mKeyVisualAttributes: KeyVisualAttributes?
    private val mOptionalAttributes: OptionalAttributes?
    private val mHashCode: Int
    private var mPressed = false

    private class OptionalAttributes private constructor(
        val mOutputText: String?,
        val mAltCode: Int
    ) {
        companion object {
            fun newInstance(outputText: String?, altCode: Int): OptionalAttributes? {
                if (outputText == null && altCode == CODE_UNSPECIFIED) return null
                return OptionalAttributes(outputText, altCode)
            }
        }
    }

    constructor(
        label: String?, iconId: Int, code: Int, outputText: String?,
        hintLabel: String?, labelFlags: Int, backgroundType: Int,
        x: Float, y: Float, width: Float, height: Float,
        leftPadding: Float, rightPadding: Float, topPadding: Float, bottomPadding: Float
    ) {
        mHitbox.set(
            Math.round(x - leftPadding), Math.round(y - topPadding),
            Math.round(x + width + rightPadding), Math.round(y + height + bottomPadding)
        )
        mX = Math.round(x)
        mY = Math.round(y)
        mWidth = Math.round(x + width) - mX
        mHeight = Math.round(y + height) - mY
        mDefinedWidth = width
        mDefinedHeight = height
        mHintLabel = hintLabel
        mLabelFlags = labelFlags
        mBackgroundType = backgroundType
        mActionFlags = ACTION_FLAGS_NO_KEY_PREVIEW
        mMoreKeys = null
        mMoreKeysColumnAndFlags = 0
        mLabel = label
        mOptionalAttributes = OptionalAttributes.newInstance(outputText, CODE_UNSPECIFIED)
        mCode = code
        mIconId = iconId
        mKeyVisualAttributes = null
        mHashCode = computeHashCode()
    }

    constructor(
        keySpec: String?, keyAttr: TypedArray,
        style: KeyStyle, params: KeyboardParams,
        row: KeyboardRow
    ) {
        row.setCurrentKey(keyAttr, isSpacer)

        mDefinedWidth = row.getKeyWidth()
        mDefinedHeight = row.getKeyHeight()

        val keyLeft = row.getKeyX()
        val keyTop = row.getKeyY()
        val keyRight = keyLeft + mDefinedWidth
        val keyBottom = keyTop + mDefinedHeight

        val leftPadding = row.getKeyLeftPadding()
        val topPadding = row.getKeyTopPadding()
        val rightPadding = row.getKeyRightPadding()
        val bottomPadding = row.getKeyBottomPadding()

        mHitbox.set(
            Math.round(keyLeft - leftPadding), Math.round(keyTop - topPadding),
            Math.round(keyRight + rightPadding), Math.round(keyBottom + bottomPadding)
        )
        mX = Math.round(keyLeft)
        mY = Math.round(keyTop)
        mWidth = Math.round(keyRight) - mX
        mHeight = Math.round(keyBottom) - mY

        mBackgroundType = style.getInt(
            keyAttr, R.styleable.Keyboard_Key_backgroundType, row.getDefaultBackgroundType()
        )

        mLabelFlags = style.getFlags(keyAttr, R.styleable.Keyboard_Key_keyLabelFlags) or
                row.getDefaultKeyLabelFlags()
        val needsToUpcase = needsToUpcase(mLabelFlags, params.mId!!.mElementId)
        val localeForUpcasing = params.mId!!.getLocale()!!
        var actionFlags = style.getFlags(keyAttr, R.styleable.Keyboard_Key_keyActionFlags)
        var moreKeys = style.getStringArray(keyAttr, R.styleable.Keyboard_Key_moreKeys)

        var moreKeysColumnAndFlags = MORE_KEYS_MODE_MAX_COLUMN_WITH_AUTO_ORDER or
                style.getInt(keyAttr, R.styleable.Keyboard_Key_maxMoreKeysColumn, params.mMaxMoreKeysKeyboardColumn)
        var value: Int
        value = MoreKeySpec.getIntValue(moreKeys, MORE_KEYS_AUTO_COLUMN_ORDER, -1)
        if (value > 0) {
            moreKeysColumnAndFlags = MORE_KEYS_MODE_FIXED_COLUMN_WITH_AUTO_ORDER or
                    (value and MORE_KEYS_COLUMN_NUMBER_MASK)
        }
        value = MoreKeySpec.getIntValue(moreKeys, MORE_KEYS_FIXED_COLUMN_ORDER, -1)
        if (value > 0) {
            moreKeysColumnAndFlags = MORE_KEYS_MODE_FIXED_COLUMN_WITH_FIXED_ORDER or
                    (value and MORE_KEYS_COLUMN_NUMBER_MASK)
        }
        if (MoreKeySpec.getBooleanValue(moreKeys, MORE_KEYS_HAS_LABELS)) {
            moreKeysColumnAndFlags = moreKeysColumnAndFlags or MORE_KEYS_FLAGS_HAS_LABELS
        }
        if (MoreKeySpec.getBooleanValue(moreKeys, MORE_KEYS_NO_PANEL_AUTO_MORE_KEY)) {
            moreKeysColumnAndFlags = moreKeysColumnAndFlags or MORE_KEYS_FLAGS_NO_PANEL_AUTO_MORE_KEY
        }
        mMoreKeysColumnAndFlags = moreKeysColumnAndFlags

        val additionalMoreKeys: Array<String>? = if ((mLabelFlags and LABEL_FLAGS_DISABLE_ADDITIONAL_MORE_KEYS) != 0) {
            null
        } else {
            style.getStringArray(keyAttr, R.styleable.Keyboard_Key_additionalMoreKeys)
        }
        moreKeys = MoreKeySpec.insertAdditionalMoreKeys(moreKeys, additionalMoreKeys)
        if (moreKeys != null) {
            actionFlags = actionFlags or ACTION_FLAGS_ENABLE_LONG_PRESS
            mMoreKeys = Array(moreKeys.size) { i ->
                MoreKeySpec(moreKeys[i], needsToUpcase, localeForUpcasing)
            }
        } else {
            mMoreKeys = null
        }
        mActionFlags = actionFlags

        mIconId = KeySpecParser.getIconId(keySpec)

        val code = KeySpecParser.getCode(keySpec)
        if ((mLabelFlags and LABEL_FLAGS_FROM_CUSTOM_ACTION_LABEL) != 0) {
            mLabel = params.mId!!.mCustomActionLabel
        } else if (code >= Character.MIN_SUPPLEMENTARY_CODE_POINT) {
            mLabel = StringBuilder().appendCodePoint(code).toString()
        } else {
            val label = KeySpecParser.getLabel(keySpec)
            mLabel = if (needsToUpcase) StringUtils.toTitleCaseOfKeyLabel(label, localeForUpcasing) else label
        }
        if ((mLabelFlags and LABEL_FLAGS_DISABLE_HINT_LABEL) != 0) {
            mHintLabel = null
        } else {
            val hintLabel = style.getString(keyAttr, R.styleable.Keyboard_Key_keyHintLabel)
            mHintLabel = if (needsToUpcase) StringUtils.toTitleCaseOfKeyLabel(hintLabel, localeForUpcasing) else hintLabel
        }
        var outputText = KeySpecParser.getOutputText(keySpec)
        if (needsToUpcase) {
            outputText = StringUtils.toTitleCaseOfKeyLabel(outputText, localeForUpcasing)
        }
        if (code == CODE_UNSPECIFIED && TextUtils.isEmpty(outputText) && !TextUtils.isEmpty(mLabel)) {
            if (StringUtils.codePointCount(mLabel) == 1) {
                if (hasShiftedLetterHint() && isShiftedLetterActivated()) {
                    mCode = mHintLabel!!.codePointAt(0)
                } else {
                    mCode = mLabel!!.codePointAt(0)
                }
            } else {
                outputText = mLabel
                mCode = CODE_OUTPUT_TEXT
            }
        } else if (code == CODE_UNSPECIFIED && outputText != null) {
            if (StringUtils.codePointCount(outputText) == 1) {
                mCode = outputText.codePointAt(0)
                outputText = null
            } else {
                mCode = CODE_OUTPUT_TEXT
            }
        } else {
            mCode = if (needsToUpcase) StringUtils.toTitleCaseOfKeyCode(code, localeForUpcasing) else code
        }
        val altCodeInAttr = KeySpecParser.parseCode(
            style.getString(keyAttr, R.styleable.Keyboard_Key_altCode), CODE_UNSPECIFIED
        )
        val altCode = if (needsToUpcase) StringUtils.toTitleCaseOfKeyCode(altCodeInAttr, localeForUpcasing) else altCodeInAttr
        mOptionalAttributes = OptionalAttributes.newInstance(outputText, altCode)
        mKeyVisualAttributes = KeyVisualAttributes.newInstance(keyAttr)
        mHashCode = computeHashCode()
    }

    protected constructor(key: Key) : this(key, key.mMoreKeys)

    private constructor(key: Key, moreKeys: Array<MoreKeySpec>?) {
        mCode = key.mCode
        mLabel = key.mLabel
        mHintLabel = key.mHintLabel
        mLabelFlags = key.mLabelFlags
        mIconId = key.mIconId
        mWidth = key.mWidth
        mHeight = key.mHeight
        mDefinedWidth = key.mDefinedWidth
        mDefinedHeight = key.mDefinedHeight
        mX = key.mX
        mY = key.mY
        mHitbox.set(key.mHitbox)
        mMoreKeys = moreKeys
        mMoreKeysColumnAndFlags = key.mMoreKeysColumnAndFlags
        mBackgroundType = key.mBackgroundType
        mActionFlags = key.mActionFlags
        mKeyVisualAttributes = key.mKeyVisualAttributes
        mOptionalAttributes = key.mOptionalAttributes
        mHashCode = key.mHashCode
        mPressed = key.mPressed
    }

    private fun computeHashCode(): Int {
        return Arrays.hashCode(
            arrayOf(
                mX, mY, mWidth, mHeight, mCode, mLabel, mHintLabel,
                mIconId, mBackgroundType, Arrays.hashCode(mMoreKeys),
                getOutputText(), mActionFlags, mLabelFlags
            )
        )
    }

    private fun equalsInternal(o: Key): Boolean {
        if (this === o) return true
        return o.mX == mX &&
                o.mY == mY &&
                o.mWidth == mWidth &&
                o.mHeight == mHeight &&
                o.mCode == mCode &&
                TextUtils.equals(o.mLabel, mLabel) &&
                TextUtils.equals(o.mHintLabel, mHintLabel) &&
                o.mIconId == mIconId &&
                o.mBackgroundType == mBackgroundType &&
                Arrays.equals(o.mMoreKeys, mMoreKeys) &&
                TextUtils.equals(o.getOutputText(), getOutputText()) &&
                o.mActionFlags == mActionFlags &&
                o.mLabelFlags == mLabelFlags
    }

    override fun compareTo(other: Key): Int {
        if (equalsInternal(other)) return 0
        return if (mHashCode > other.mHashCode) 1 else -1
    }

    override fun hashCode(): Int = mHashCode

    override fun equals(other: Any?): Boolean {
        return other is Key && equalsInternal(other)
    }

    override fun toString(): String {
        return "${toShortString()} $x,$y ${width}x$height"
    }

    fun toShortString(): String {
        val code = getCode()
        return if (code == Constants.CODE_OUTPUT_TEXT) getOutputText() ?: "" else Constants.printableCode(code)
    }

    fun getCode(): Int = mCode

    fun getLabel(): String? = mLabel

    fun getHintLabel(): String? = mHintLabel

    fun getMoreKeys(): Array<MoreKeySpec>? = mMoreKeys

    fun setHitboxRightEdge(right: Int) {
        mHitbox.right = right
    }

    val isSpacer: Boolean
        get() = this is Spacer

    val isShift: Boolean
        get() = mCode == CODE_SHIFT

    val isModifier: Boolean
        get() = mCode == CODE_SHIFT || mCode == CODE_SWITCH_ALPHA_SYMBOL

    val isRepeatable: Boolean
        get() = mActionFlags and ACTION_FLAGS_IS_REPEATABLE != 0

    val noKeyPreview: Boolean
        get() = mActionFlags and ACTION_FLAGS_NO_KEY_PREVIEW != 0

    val altCodeWhileTyping: Boolean
        get() = mActionFlags and ACTION_FLAGS_ALT_CODE_WHILE_TYPING != 0

    val isLongPressEnabled: Boolean
        get() = (mActionFlags and ACTION_FLAGS_ENABLE_LONG_PRESS != 0) &&
                (mLabelFlags and LABEL_FLAGS_SHIFTED_LETTER_ACTIVATED == 0)

    fun selectTypeface(params: KeyDrawParams): Typeface = when (mLabelFlags and LABEL_FLAGS_FONT_MASK) {
        LABEL_FLAGS_FONT_NORMAL -> Typeface.DEFAULT
        LABEL_FLAGS_FONT_MONO_SPACE -> Typeface.MONOSPACE
        else -> params.mTypeface
    }

    fun selectTextSize(params: KeyDrawParams): Int = when (mLabelFlags and LABEL_FLAGS_FOLLOW_KEY_TEXT_RATIO_MASK) {
        LABEL_FLAGS_FOLLOW_KEY_LETTER_RATIO -> params.mLetterSize
        LABEL_FLAGS_FOLLOW_KEY_LARGE_LETTER_RATIO -> params.mLargeLetterSize
        LABEL_FLAGS_FOLLOW_KEY_LABEL_RATIO -> params.mLabelSize
        LABEL_FLAGS_FOLLOW_KEY_HINT_LABEL_RATIO -> params.mHintLabelSize
        else -> if (StringUtils.codePointCount(mLabel) == 1) params.mLetterSize else params.mLabelSize
    }

    fun selectTextColor(params: KeyDrawParams): Int {
        if ((mLabelFlags and LABEL_FLAGS_FOLLOW_FUNCTIONAL_TEXT_COLOR) != 0) return params.mFunctionalTextColor
        return if (isShiftedLetterActivated()) params.mTextInactivatedColor else params.mTextColor
    }

    fun selectHintTextSize(params: KeyDrawParams): Int {
        if (hasHintLabel()) return params.mHintLabelSize
        if (hasShiftedLetterHint()) return params.mShiftedLetterHintSize
        return params.mHintLetterSize
    }

    fun selectHintTextColor(params: KeyDrawParams): Int {
        if (hasHintLabel()) return params.mHintLabelColor
        if (hasShiftedLetterHint()) {
            return if (isShiftedLetterActivated()) params.mShiftedLetterHintActivatedColor else params.mShiftedLetterHintInactivatedColor
        }
        return params.mHintLetterColor
    }

    fun getPreviewLabel(): String? = if (isShiftedLetterActivated()) mHintLabel else mLabel

    private fun previewHasLetterSize(): Boolean {
        return (mLabelFlags and LABEL_FLAGS_FOLLOW_KEY_LETTER_RATIO) != 0 ||
                StringUtils.codePointCount(getPreviewLabel()) == 1
    }

    fun selectPreviewTextSize(params: KeyDrawParams): Int {
        return if (previewHasLetterSize()) params.mPreviewTextSize else params.mLetterSize
    }

    fun selectPreviewTypeface(params: KeyDrawParams): Typeface {
        return if (previewHasLetterSize()) selectTypeface(params) else Typeface.DEFAULT_BOLD
    }

    fun isAlignHintLabelToBottom(defaultFlags: Int): Boolean {
        return (mLabelFlags or defaultFlags) and LABEL_FLAGS_ALIGN_HINT_LABEL_TO_BOTTOM != 0
    }

    fun isAlignIconToBottom(): Boolean = mLabelFlags and LABEL_FLAGS_ALIGN_ICON_TO_BOTTOM != 0

    fun isAlignLabelOffCenter(): Boolean = mLabelFlags and LABEL_FLAGS_ALIGN_LABEL_OFF_CENTER != 0

    fun hasShiftedLetterHint(): Boolean {
        return (mLabelFlags and LABEL_FLAGS_HAS_SHIFTED_LETTER_HINT) != 0 && !TextUtils.isEmpty(mHintLabel)
    }

    fun hasHintLabel(): Boolean = (mLabelFlags and LABEL_FLAGS_HAS_HINT_LABEL) != 0

    fun needsAutoXScale(): Boolean = (mLabelFlags and LABEL_FLAGS_AUTO_X_SCALE) != 0

    fun needsAutoScale(): Boolean = (mLabelFlags and LABEL_FLAGS_AUTO_SCALE) == LABEL_FLAGS_AUTO_SCALE

    private fun isShiftedLetterActivated(): Boolean {
        return (mLabelFlags and LABEL_FLAGS_SHIFTED_LETTER_ACTIVATED) != 0 && !TextUtils.isEmpty(mHintLabel)
    }

    fun getMoreKeysColumnNumber(): Int = mMoreKeysColumnAndFlags and MORE_KEYS_COLUMN_NUMBER_MASK

    fun isMoreKeysFixedColumn(): Boolean = mMoreKeysColumnAndFlags and MORE_KEYS_FLAGS_FIXED_COLUMN != 0

    fun isMoreKeysFixedOrder(): Boolean = mMoreKeysColumnAndFlags and MORE_KEYS_FLAGS_FIXED_ORDER != 0

    fun hasLabelsInMoreKeys(): Boolean = mMoreKeysColumnAndFlags and MORE_KEYS_FLAGS_HAS_LABELS != 0

    fun getMoreKeyLabelFlags(): Int {
        val labelSizeFlag = if (hasLabelsInMoreKeys()) LABEL_FLAGS_FOLLOW_KEY_LABEL_RATIO else LABEL_FLAGS_FOLLOW_KEY_LETTER_RATIO
        return labelSizeFlag or LABEL_FLAGS_AUTO_X_SCALE
    }

    fun hasNoPanelAutoMoreKey(): Boolean = mMoreKeysColumnAndFlags and MORE_KEYS_FLAGS_NO_PANEL_AUTO_MORE_KEY != 0

    fun getOutputText(): String? = mOptionalAttributes?.mOutputText

    fun getAltCode(): Int = mOptionalAttributes?.mAltCode ?: CODE_UNSPECIFIED

    fun getIconId(): Int = mIconId

    fun getIcon(iconSet: KeyboardIconsSet, alpha: Int): Drawable? {
        val icon = iconSet.getIconDrawable(getIconId())
        icon?.alpha = alpha
        return icon
    }

    fun getPreviewIcon(iconSet: KeyboardIconsSet): Drawable? = iconSet.getIconDrawable(getIconId())

    val width: Int get() = mWidth

    val height: Int get() = mHeight

    fun getDefinedWidth(): Float = mDefinedWidth

    fun getDefinedHeight(): Float = mDefinedHeight

    val x: Int get() = mX

    val y: Int get() = mY

    val visualAttributes: KeyVisualAttributes? get() = mKeyVisualAttributes

    val topPadding: Int get() = mY - mHitbox.top

    val bottomPadding: Int get() = mHitbox.bottom - mY - mHeight

    val leftPadding: Int get() = mX - mHitbox.left

    val rightPadding: Int get() = mHitbox.right - mX - mWidth

    fun onPressed() { mPressed = true }

    fun onReleased() { mPressed = false }

    fun isOnKey(x: Int, y: Int): Boolean = mHitbox.contains(x, y)

    fun squaredDistanceToHitboxEdge(x: Int, y: Int): Int {
        val left = mHitbox.left
        val right = mHitbox.right - 1
        val top = mHitbox.top
        val bottom = mHitbox.bottom - 1
        val edgeX = if (x < left) left else minOf(x, right)
        val edgeY = if (y < top) top else minOf(y, bottom)
        val dx = x - edgeX
        val dy = y - edgeY
        return dx * dx + dy * dy
    }

    class KeyBackgroundState private constructor(private val mReleasedState: IntArray, private val mPressedState: IntArray = intArrayOf()) {
        fun getState(pressed: Boolean): IntArray = if (pressed) mPressedState else mReleasedState

        companion object {
            val STATES = arrayOf(
                KeyBackgroundState(intArrayOf(android.R.attr.state_empty)),
                KeyBackgroundState(intArrayOf()),
                KeyBackgroundState(intArrayOf()),
                KeyBackgroundState(intArrayOf(android.R.attr.state_checkable)),
                KeyBackgroundState(intArrayOf(android.R.attr.state_checkable, android.R.attr.state_checked)),
                KeyBackgroundState(intArrayOf(android.R.attr.state_active)),
                KeyBackgroundState(intArrayOf())
            )
        }
    }

    fun selectBackgroundDrawable(
        keyBackground: Drawable,
        functionalKeyBackground: Drawable,
        spacebarBackground: Drawable
    ): Drawable {
        val background = when (mBackgroundType) {
            BACKGROUND_TYPE_FUNCTIONAL -> functionalKeyBackground
            BACKGROUND_TYPE_SPACEBAR -> spacebarBackground
            else -> keyBackground
        }
        val state = KeyBackgroundState.STATES[mBackgroundType].getState(mPressed)
        background.state = state
        return background
    }

    class Spacer(
        keyAttr: TypedArray,
        keyStyle: KeyStyle,
        params: KeyboardParams,
        row: KeyboardRow
    ) : Key(null, keyAttr, keyStyle, params, row)

    companion object {
        private const val LABEL_FLAGS_ALIGN_HINT_LABEL_TO_BOTTOM = 0x02
        private const val LABEL_FLAGS_ALIGN_ICON_TO_BOTTOM = 0x04
        private const val LABEL_FLAGS_ALIGN_LABEL_OFF_CENTER = 0x08
        private const val LABEL_FLAGS_FONT_MASK = 0x30
        private const val LABEL_FLAGS_FONT_NORMAL = 0x10
        private const val LABEL_FLAGS_FONT_MONO_SPACE = 0x20
        private const val LABEL_FLAGS_FONT_DEFAULT = 0x30
        private const val LABEL_FLAGS_FOLLOW_KEY_TEXT_RATIO_MASK = 0x1C0
        private const val LABEL_FLAGS_FOLLOW_KEY_LARGE_LETTER_RATIO = 0x40
        private const val LABEL_FLAGS_FOLLOW_KEY_LETTER_RATIO = 0x80
        private const val LABEL_FLAGS_FOLLOW_KEY_LABEL_RATIO = 0xC0
        private const val LABEL_FLAGS_FOLLOW_KEY_HINT_LABEL_RATIO = 0x140
        private const val LABEL_FLAGS_HAS_SHIFTED_LETTER_HINT = 0x400
        private const val LABEL_FLAGS_HAS_HINT_LABEL = 0x800
        private const val LABEL_FLAGS_AUTO_X_SCALE = 0x4000
        private const val LABEL_FLAGS_AUTO_Y_SCALE = 0x8000
        private const val LABEL_FLAGS_AUTO_SCALE = LABEL_FLAGS_AUTO_X_SCALE or LABEL_FLAGS_AUTO_Y_SCALE
        private const val LABEL_FLAGS_PRESERVE_CASE = 0x10000
        private const val LABEL_FLAGS_SHIFTED_LETTER_ACTIVATED = 0x20000
        private const val LABEL_FLAGS_FROM_CUSTOM_ACTION_LABEL = 0x40000
        private const val LABEL_FLAGS_FOLLOW_FUNCTIONAL_TEXT_COLOR = 0x80000
        private const val LABEL_FLAGS_DISABLE_HINT_LABEL = 0x40000000.toInt()
        private const val LABEL_FLAGS_DISABLE_ADDITIONAL_MORE_KEYS = 0x80000000.toInt()
        private const val MORE_KEYS_COLUMN_NUMBER_MASK = 0x000000ff
        private const val MORE_KEYS_FLAGS_FIXED_COLUMN = 0x00000100
        private const val MORE_KEYS_FLAGS_FIXED_ORDER = 0x00000200
        private const val MORE_KEYS_MODE_MAX_COLUMN_WITH_AUTO_ORDER = 0
        private const val MORE_KEYS_MODE_FIXED_COLUMN_WITH_AUTO_ORDER = MORE_KEYS_FLAGS_FIXED_COLUMN
        private const val MORE_KEYS_MODE_FIXED_COLUMN_WITH_FIXED_ORDER = MORE_KEYS_FLAGS_FIXED_COLUMN or MORE_KEYS_FLAGS_FIXED_ORDER
        private const val MORE_KEYS_FLAGS_HAS_LABELS = 0x40000000
        private const val MORE_KEYS_FLAGS_NO_PANEL_AUTO_MORE_KEY = 0x10000000
        private const val MORE_KEYS_AUTO_COLUMN_ORDER = "!autoColumnOrder!"
        private const val MORE_KEYS_FIXED_COLUMN_ORDER = "!fixedColumnOrder!"
        private const val MORE_KEYS_HAS_LABELS = "!hasLabels!"
        private const val MORE_KEYS_NO_PANEL_AUTO_MORE_KEY = "!noPanelAutoMoreKey!"
        const val BACKGROUND_TYPE_NORMAL = 1
        const val BACKGROUND_TYPE_FUNCTIONAL = 2
        const val BACKGROUND_TYPE_SPACEBAR = 6
        private const val ACTION_FLAGS_IS_REPEATABLE = 0x01
        private const val ACTION_FLAGS_NO_KEY_PREVIEW = 0x02
        private const val ACTION_FLAGS_ALT_CODE_WHILE_TYPING = 0x04
        private const val ACTION_FLAGS_ENABLE_LONG_PRESS = 0x08

        fun removeRedundantMoreKeys(key: Key, lettersOnBaseLayout: MoreKeySpec.LettersOnBaseLayout): Key {
            val moreKeys = key.getMoreKeys()
            val filteredMoreKeys = MoreKeySpec.removeRedundantMoreKeys(moreKeys, lettersOnBaseLayout)
            return if (filteredMoreKeys === moreKeys) key else Key(key, filteredMoreKeys)
        }

        private fun needsToUpcase(labelFlags: Int, keyboardElementId: Int): Boolean {
            if (labelFlags and LABEL_FLAGS_PRESERVE_CASE != 0) return false
            return when (keyboardElementId) {
                KeyboardId.ELEMENT_ALPHABET_MANUAL_SHIFTED,
                KeyboardId.ELEMENT_ALPHABET_AUTOMATIC_SHIFTED,
                KeyboardId.ELEMENT_ALPHABET_SHIFT_LOCKED -> true
                else -> false
            }
        }
    }
}
