/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2023 Raimondas Rimkus
 * Copyright (C) 2021 wittmane
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

/**
 * Class for describing the position and characteristics of a single key in the keyboard.
 */
open class Key : Comparable<Key> {
    /** The key code (unicode or custom code) that this key generates. */
    val code: Int

    /** Label to display */
    val label: String?
    /** Hint label to display on the key in conjunction with the the label */
    val hintLabel: String?
    /** Flags of the label */
    val labelFlags: Int

    /** Icon to display instead of a label. Icon takes precedence over a label */
    val iconId: Int

    /** Width of the key, excluding the padding */
    val width: Int
    /** Height of the key, excluding the padding */
    val height: Int
    /** Exact theoretical width of the key, excluding the padding */
    val definedWidth: Float
    /** Exact theoretical height of the key, excluding the padding */
    val definedHeight: Float
    /** X coordinate of the top-left corner of the key in the keyboard layout, excluding padding. */
    val x: Int
    /** Y coordinate of the top-left corner of the key in the keyboard layout, excluding padding. */
    val y: Int
    /** Hit bounding box of the key */
    val hitbox = Rect()

    /** More keys. It is guaranteed that this is null or an array of one or more elements */
    val moreKeys: Array<MoreKeySpec>?
    /** More keys column number and flags */
    private val moreKeysColumnAndFlags: Int

    /** Background type that represents different key background visual than normal one. */
    val backgroundType: Int

    private val actionFlags: Int
    private val keyVisualAttributes: KeyVisualAttributes?
    private val optionalAttributes: OptionalAttributes?
    private val hashCode: Int

    /** The current pressed state of this key */
    var isPressed: Boolean = false
        private set

    private class OptionalAttributes(
        /** Text to output when pressed. This can be multiple characters, like ".com" */
        val outputText: String?,
        val altCode: Int
    ) {
        companion object {
            fun newInstance(outputText: String?, altCode: Int): OptionalAttributes? {
                if (outputText == null && altCode == CODE_UNSPECIFIED) {
                    return null
                }
                return OptionalAttributes(outputText, altCode)
            }
        }
    }

    /**
     * Constructor for a key on [MoreKeysKeyboard].
     */
    constructor(
        label: String?,
        iconId: Int,
        code: Int,
        outputText: String?,
        hintLabel: String?,
        labelFlags: Int,
        backgroundType: Int,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        leftPadding: Float,
        rightPadding: Float,
        topPadding: Float,
        bottomPadding: Float
    ) {
        hitbox.set(
            Math.round(x - leftPadding), Math.round(y - topPadding),
            Math.round(x + width + rightPadding), Math.round(y + height + bottomPadding)
        )
        this.x = Math.round(x)
        this.y = Math.round(y)
        this.width = Math.round(x + width) - this.x
        this.height = Math.round(y + height) - this.y
        this.definedWidth = width
        this.definedHeight = height
        this.hintLabel = hintLabel
        this.labelFlags = labelFlags
        this.backgroundType = backgroundType
        actionFlags = ACTION_FLAGS_NO_KEY_PREVIEW
        moreKeys = null
        moreKeysColumnAndFlags = 0
        this.label = label
        optionalAttributes = OptionalAttributes.newInstance(outputText, CODE_UNSPECIFIED)
        this.code = code
        this.iconId = iconId
        keyVisualAttributes = null
        hashCode = computeHashCode(this)
    }

    /**
     * Create a key with the given top-left coordinate and extract its attributes from a key
     * specification string, Key attribute array, key style, and etc.
     */
    constructor(
        keySpec: String?,
        keyAttr: TypedArray,
        style: KeyStyle,
        params: KeyboardParams,
        row: KeyboardRow
    ) {
        // Update the row to work with the new key
        row.setCurrentKey(keyAttr, isSpacer)

        definedWidth = row.getKeyWidth()
        definedHeight = row.getKeyHeight()

        val keyLeft = row.getKeyX()
        val keyTop = row.getKeyY()
        val keyRight = keyLeft + definedWidth
        val keyBottom = keyTop + definedHeight

        val leftPadding = row.getKeyLeftPadding()
        val topPadding = row.getKeyTopPadding()
        val rightPadding = row.getKeyRightPadding()
        val bottomPadding = row.getKeyBottomPadding()

        hitbox.set(
            Math.round(keyLeft - leftPadding), Math.round(keyTop - topPadding),
            Math.round(keyRight + rightPadding), Math.round(keyBottom + bottomPadding)
        )
        this.x = Math.round(keyLeft)
        this.y = Math.round(keyTop)
        this.width = Math.round(keyRight) - this.x
        this.height = Math.round(keyBottom) - this.y

        backgroundType = style.getInt(
            keyAttr,
            R.styleable.Keyboard_Key_backgroundType, row.getDefaultBackgroundType()
        )

        var labelFlags = style.getFlags(keyAttr, R.styleable.Keyboard_Key_keyLabelFlags) or
                row.getDefaultKeyLabelFlags()
        val needsToUpcase = needsToUpcase(labelFlags, params.mId.mElementId)
        val localeForUpcasing = params.mId.getLocale()
        var actionFlags = style.getFlags(keyAttr, R.styleable.Keyboard_Key_keyActionFlags)
        var moreKeysArray = style.getStringArray(keyAttr, R.styleable.Keyboard_Key_moreKeys)

        // Get maximum column order number and set a relevant mode value.
        var moreKeysColumnAndFlags = MORE_KEYS_MODE_MAX_COLUMN_WITH_AUTO_ORDER or
                style.getInt(
                    keyAttr, R.styleable.Keyboard_Key_maxMoreKeysColumn,
                    params.mMaxMoreKeysKeyboardColumn
                )
        var value: Int
        if (MoreKeySpec.getIntValue(moreKeysArray, MORE_KEYS_AUTO_COLUMN_ORDER, -1).also { value = it } > 0) {
            // Override with fixed column order number and set a relevant mode value.
            moreKeysColumnAndFlags = MORE_KEYS_MODE_FIXED_COLUMN_WITH_AUTO_ORDER or
                    (value and MORE_KEYS_COLUMN_NUMBER_MASK)
        }
        if (MoreKeySpec.getIntValue(moreKeysArray, MORE_KEYS_FIXED_COLUMN_ORDER, -1).also { value = it } > 0) {
            // Override with fixed column order number and set a relevant mode value.
            moreKeysColumnAndFlags = MORE_KEYS_MODE_FIXED_COLUMN_WITH_FIXED_ORDER or
                    (value and MORE_KEYS_COLUMN_NUMBER_MASK)
        }
        if (MoreKeySpec.getBooleanValue(moreKeysArray, MORE_KEYS_HAS_LABELS)) {
            moreKeysColumnAndFlags = moreKeysColumnAndFlags or MORE_KEYS_FLAGS_HAS_LABELS
        }
        if (MoreKeySpec.getBooleanValue(moreKeysArray, MORE_KEYS_NO_PANEL_AUTO_MORE_KEY)) {
            moreKeysColumnAndFlags = moreKeysColumnAndFlags or MORE_KEYS_FLAGS_NO_PANEL_AUTO_MORE_KEY
        }
        this.moreKeysColumnAndFlags = moreKeysColumnAndFlags

        val additionalMoreKeys: Array<String?>? = if ((labelFlags and LABEL_FLAGS_DISABLE_ADDITIONAL_MORE_KEYS) != 0) {
            null
        } else {
            style.getStringArray(keyAttr, R.styleable.Keyboard_Key_additionalMoreKeys)
        }
        moreKeysArray = MoreKeySpec.insertAdditionalMoreKeys(moreKeysArray, additionalMoreKeys)
        if (moreKeysArray != null) {
            actionFlags = actionFlags or ACTION_FLAGS_ENABLE_LONG_PRESS
            this.moreKeys = Array(moreKeysArray.size) { i ->
                MoreKeySpec(moreKeysArray[i]!!, needsToUpcase, localeForUpcasing)
            }
        } else {
            this.moreKeys = null
        }
        this.actionFlags = actionFlags

        iconId = KeySpecParser.getIconId(keySpec)

        val codeValue = KeySpecParser.getCode(keySpec)
        if ((labelFlags and LABEL_FLAGS_FROM_CUSTOM_ACTION_LABEL) != 0) {
            label = params.mId.mCustomActionLabel
        } else if (codeValue >= Character.MIN_SUPPLEMENTARY_CODE_POINT) {
            label = StringBuilder().appendCodePoint(codeValue).toString()
        } else {
            val labelText = KeySpecParser.getLabel(keySpec)
            label = if (needsToUpcase) {
                StringUtils.toTitleCaseOfKeyLabel(labelText, localeForUpcasing)
            } else {
                labelText
            }
        }
        if ((labelFlags and LABEL_FLAGS_DISABLE_HINT_LABEL) != 0) {
            hintLabel = null
        } else {
            val hintLabelText = style.getString(keyAttr, R.styleable.Keyboard_Key_keyHintLabel)
            hintLabel = if (needsToUpcase) {
                StringUtils.toTitleCaseOfKeyLabel(hintLabelText, localeForUpcasing)
            } else {
                hintLabelText
            }
        }
        var outputText = KeySpecParser.getOutputText(keySpec)
        if (needsToUpcase) {
            outputText = StringUtils.toTitleCaseOfKeyLabel(outputText, localeForUpcasing)
        }
        // Choose the first letter of the label as primary code if not specified.
        if (codeValue == CODE_UNSPECIFIED && TextUtils.isEmpty(outputText) &&
            !TextUtils.isEmpty(label)
        ) {
            if (StringUtils.codePointCount(label) == 1) {
                if (hasShiftedLetterHint() && isShiftedLetterActivated()) {
                    this.code = hintLabel!!.codePointAt(0)
                } else {
                    this.code = label.codePointAt(0)
                }
            } else {
                outputText = label
                this.code = CODE_OUTPUT_TEXT
            }
        } else if (codeValue == CODE_UNSPECIFIED && outputText != null) {
            if (StringUtils.codePointCount(outputText) == 1) {
                this.code = outputText.codePointAt(0)
                outputText = null
            } else {
                this.code = CODE_OUTPUT_TEXT
            }
        } else {
            this.code = if (needsToUpcase) StringUtils.toTitleCaseOfKeyCode(codeValue, localeForUpcasing) else codeValue
        }
        val altCodeInAttr = KeySpecParser.parseCode(
            style.getString(keyAttr, R.styleable.Keyboard_Key_altCode), CODE_UNSPECIFIED
        )
        val altCode = if (needsToUpcase) {
            StringUtils.toTitleCaseOfKeyCode(altCodeInAttr, localeForUpcasing)
        } else {
            altCodeInAttr
        }
        optionalAttributes = OptionalAttributes.newInstance(outputText, altCode)
        keyVisualAttributes = KeyVisualAttributes.newInstance(keyAttr)
        this.hashCode = computeHashCode(this)
    }

    /**
     * Copy constructor for DynamicGridKeyboard.GridKey.
     */
    protected constructor(key: Key) : this(key, key.moreKeys)

    private constructor(key: Key, moreKeys: Array<MoreKeySpec>?) {
        code = key.code
        label = key.label
        hintLabel = key.hintLabel
        labelFlags = key.labelFlags
        iconId = key.iconId
        width = key.width
        height = key.height
        definedWidth = key.definedWidth
        definedHeight = key.definedHeight
        x = key.x
        y = key.y
        hitbox.set(key.hitbox)
        this.moreKeys = moreKeys
        moreKeysColumnAndFlags = key.moreKeysColumnAndFlags
        backgroundType = key.backgroundType
        actionFlags = key.actionFlags
        keyVisualAttributes = key.keyVisualAttributes
        optionalAttributes = key.optionalAttributes
        hashCode = key.hashCode
        isPressed = key.isPressed
    }

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
        private const val MORE_KEYS_MODE_FIXED_COLUMN_WITH_FIXED_ORDER =
            MORE_KEYS_FLAGS_FIXED_COLUMN or MORE_KEYS_FLAGS_FIXED_ORDER
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

        @JvmStatic
        fun removeRedundantMoreKeys(
            key: Key,
            lettersOnBaseLayout: MoreKeySpec.LettersOnBaseLayout
        ): Key {
            val moreKeys = key.moreKeys
            val filteredMoreKeys = MoreKeySpec.removeRedundantMoreKeys(
                moreKeys, lettersOnBaseLayout
            )
            return if (filteredMoreKeys === moreKeys) key else Key(key, filteredMoreKeys)
        }

        private fun needsToUpcase(labelFlags: Int, keyboardElementId: Int): Boolean {
            if ((labelFlags and LABEL_FLAGS_PRESERVE_CASE) != 0) return false
            return when (keyboardElementId) {
                KeyboardId.ELEMENT_ALPHABET_MANUAL_SHIFTED,
                KeyboardId.ELEMENT_ALPHABET_AUTOMATIC_SHIFTED,
                KeyboardId.ELEMENT_ALPHABET_SHIFT_LOCKED -> true
                else -> false
            }
        }

        private fun computeHashCode(key: Key): Int {
            return Arrays.hashCode(
                arrayOf(
                    key.x,
                    key.y,
                    key.width,
                    key.height,
                    key.code,
                    key.label,
                    key.hintLabel,
                    key.iconId,
                    key.backgroundType,
                    Arrays.hashCode(key.moreKeys),
                    key.outputText,
                    key.actionFlags,
                    key.labelFlags,
                )
            )
        }
    }

    private fun equalsInternal(o: Key): Boolean {
        if (this === o) return true
        return o.x == x &&
                o.y == y &&
                o.width == width &&
                o.height == height &&
                o.code == code &&
                TextUtils.equals(o.label, label) &&
                TextUtils.equals(o.hintLabel, hintLabel) &&
                o.iconId == iconId &&
                o.backgroundType == backgroundType &&
                Arrays.equals(o.moreKeys, moreKeys) &&
                TextUtils.equals(o.outputText, outputText) &&
                o.actionFlags == actionFlags &&
                o.labelFlags == labelFlags
    }

    override fun compareTo(o: Key): Int {
        if (equalsInternal(o)) return 0
        if (hashCode > o.hashCode) return 1
        return -1
    }

    override fun hashCode(): Int = hashCode

    override fun equals(other: Any?): Boolean {
        return other is Key && equalsInternal(other)
    }

    override fun toString(): String {
        return "${toShortString()} $x,$y ${width}x$height"
    }

    fun toShortString(): String {
        val codeValue = code
        return if (codeValue == Constants.CODE_OUTPUT_TEXT) {
            outputText ?: ""
        } else {
            Constants.printableCode(codeValue)
        }
    }

    fun setHitboxRightEdge(right: Int) {
        hitbox.right = right
    }

    val isSpacer: Boolean
        get() = this is Spacer

    val isShift: Boolean
        get() = code == CODE_SHIFT

    val isModifier: Boolean
        get() = code == CODE_SHIFT || code == CODE_SWITCH_ALPHA_SYMBOL

    val isRepeatable: Boolean
        get() = (actionFlags and ACTION_FLAGS_IS_REPEATABLE) != 0

    val noKeyPreview: Boolean
        get() = (actionFlags and ACTION_FLAGS_NO_KEY_PREVIEW) != 0

    val isAltCodeWhileTyping: Boolean
        get() = (actionFlags and ACTION_FLAGS_ALT_CODE_WHILE_TYPING) != 0

    val isLongPressEnabled: Boolean
        get() = (actionFlags and ACTION_FLAGS_ENABLE_LONG_PRESS) != 0 &&
                (labelFlags and LABEL_FLAGS_SHIFTED_LETTER_ACTIVATED) == 0

    val visualAttributes: KeyVisualAttributes?
        get() = keyVisualAttributes

    fun selectTypeface(params: KeyDrawParams): Typeface {
        return when (labelFlags and LABEL_FLAGS_FONT_MASK) {
            LABEL_FLAGS_FONT_NORMAL -> Typeface.DEFAULT
            LABEL_FLAGS_FONT_MONO_SPACE -> Typeface.MONOSPACE
            LABEL_FLAGS_FONT_DEFAULT -> params.mTypeface
            else -> params.mTypeface
        }
    }

    fun selectTextSize(params: KeyDrawParams): Int {
        return when (labelFlags and LABEL_FLAGS_FOLLOW_KEY_TEXT_RATIO_MASK) {
            LABEL_FLAGS_FOLLOW_KEY_LETTER_RATIO -> params.mLetterSize
            LABEL_FLAGS_FOLLOW_KEY_LARGE_LETTER_RATIO -> params.mLargeLetterSize
            LABEL_FLAGS_FOLLOW_KEY_LABEL_RATIO -> params.mLabelSize
            LABEL_FLAGS_FOLLOW_KEY_HINT_LABEL_RATIO -> params.mHintLabelSize
            else -> if (StringUtils.codePointCount(label) == 1) params.mLetterSize else params.mLabelSize
        }
    }

    fun selectTextColor(params: KeyDrawParams): Int {
        if ((labelFlags and LABEL_FLAGS_FOLLOW_FUNCTIONAL_TEXT_COLOR) != 0) {
            return params.mFunctionalTextColor
        }
        return if (isShiftedLetterActivated) params.mTextInactivatedColor else params.mTextColor
    }

    fun selectHintTextSize(params: KeyDrawParams): Int {
        if (hasHintLabel) {
            return params.mHintLabelSize
        }
        if (hasShiftedLetterHint) {
            return params.mShiftedLetterHintSize
        }
        return params.mHintLetterSize
    }

    fun selectHintTextColor(params: KeyDrawParams): Int {
        if (hasHintLabel) {
            return params.mHintLabelColor
        }
        if (hasShiftedLetterHint) {
            return if (isShiftedLetterActivated) params.mShiftedLetterHintActivatedColor
            else params.mShiftedLetterHintInactivatedColor
        }
        return params.mHintLetterColor
    }

    val previewLabel: String?
        get() = if (isShiftedLetterActivated) hintLabel else label

    private fun previewHasLetterSize(): Boolean {
        return (labelFlags and LABEL_FLAGS_FOLLOW_KEY_LETTER_RATIO) != 0 ||
                StringUtils.codePointCount(previewLabel) == 1
    }

    fun selectPreviewTextSize(params: KeyDrawParams): Int {
        return if (previewHasLetterSize()) params.mPreviewTextSize else params.mLetterSize
    }

    fun selectPreviewTypeface(params: KeyDrawParams): Typeface {
        return if (previewHasLetterSize()) selectTypeface(params) else Typeface.DEFAULT_BOLD
    }

    fun isAlignHintLabelToBottom(defaultFlags: Int): Boolean {
        return ((labelFlags or defaultFlags) and LABEL_FLAGS_ALIGN_HINT_LABEL_TO_BOTTOM) != 0
    }

    val isAlignIconToBottom: Boolean
        get() = (labelFlags and LABEL_FLAGS_ALIGN_ICON_TO_BOTTOM) != 0

    val isAlignLabelOffCenter: Boolean
        get() = (labelFlags and LABEL_FLAGS_ALIGN_LABEL_OFF_CENTER) != 0

    val hasShiftedLetterHint: Boolean
        get() = (labelFlags and LABEL_FLAGS_HAS_SHIFTED_LETTER_HINT) != 0 &&
                !TextUtils.isEmpty(hintLabel)

    val hasHintLabel: Boolean
        get() = (labelFlags and LABEL_FLAGS_HAS_HINT_LABEL) != 0

    val needsAutoXScale: Boolean
        get() = (labelFlags and LABEL_FLAGS_AUTO_X_SCALE) != 0

    val needsAutoScale: Boolean
        get() = (labelFlags and LABEL_FLAGS_AUTO_SCALE) == LABEL_FLAGS_AUTO_SCALE

    private val isShiftedLetterActivated: Boolean
        get() = (labelFlags and LABEL_FLAGS_SHIFTED_LETTER_ACTIVATED) != 0 &&
                !TextUtils.isEmpty(hintLabel)

    val moreKeysColumnNumber: Int
        get() = moreKeysColumnAndFlags and MORE_KEYS_COLUMN_NUMBER_MASK

    val isMoreKeysFixedColumn: Boolean
        get() = (moreKeysColumnAndFlags and MORE_KEYS_FLAGS_FIXED_COLUMN) != 0

    val isMoreKeysFixedOrder: Boolean
        get() = (moreKeysColumnAndFlags and MORE_KEYS_FLAGS_FIXED_ORDER) != 0

    val hasLabelsInMoreKeys: Boolean
        get() = (moreKeysColumnAndFlags and MORE_KEYS_FLAGS_HAS_LABELS) != 0

    val moreKeyLabelFlags: Int
        get() {
            val labelSizeFlag = if (hasLabelsInMoreKeys) {
                LABEL_FLAGS_FOLLOW_KEY_LABEL_RATIO
            } else {
                LABEL_FLAGS_FOLLOW_KEY_LETTER_RATIO
            }
            return labelSizeFlag or LABEL_FLAGS_AUTO_X_SCALE
        }

    val hasNoPanelAutoMoreKey: Boolean
        get() = (moreKeysColumnAndFlags and MORE_KEYS_FLAGS_NO_PANEL_AUTO_MORE_KEY) != 0

    val outputText: String?
        get() = optionalAttributes?.outputText

    val altCode: Int
        get() = optionalAttributes?.altCode ?: CODE_UNSPECIFIED

    fun getIcon(iconSet: KeyboardIconsSet, alpha: Int): Drawable? {
        val icon = iconSet.getIconDrawable(iconId)
        icon?.alpha = alpha
        return icon
    }

    fun getPreviewIcon(iconSet: KeyboardIconsSet): Drawable? {
        return iconSet.getIconDrawable(iconId)
    }

    fun getTopPadding(): Int = y - hitbox.top

    fun getBottomPadding(): Int = hitbox.bottom - y - height

    fun getLeftPadding(): Int = x - hitbox.left

    fun getRightPadding(): Int = hitbox.right - x - width

    /**
     * Informs the key that it has been pressed, in case it needs to change its appearance or
     * state.
     */
    fun onPressed() {
        isPressed = true
    }

    /**
     * Informs the key that it has been released, in case it needs to change its appearance or
     * state.
     */
    fun onReleased() {
        isPressed = false
    }

    fun isOnKey(x: Int, y: Int): Boolean {
        return hitbox.contains(x, y)
    }

    fun squaredDistanceToHitboxEdge(x: Int, y: Int): Int {
        val left = hitbox.left
        // The hit box right is exclusive
        val right = hitbox.right - 1
        val top = hitbox.top
        // The hit box bottom is exclusive
        val bottom = hitbox.bottom - 1
        val edgeX = if (x < left) left else Math.min(x, right)
        val edgeY = if (y < top) top else Math.min(y, bottom)
        val dx = x - edgeX
        val dy = y - edgeY
        return dx * dx + dy * dy
    }

    class KeyBackgroundState private constructor(private val releasedState: IntArray, private val pressedState: IntArray) {
        fun getState(pressed: Boolean): IntArray {
            return if (pressed) pressedState else releasedState
        }

        companion object {
            @JvmField
            val STATES: Array<KeyBackgroundState> = arrayOf(
                // 0: BACKGROUND_TYPE_EMPTY
                KeyBackgroundState(intArrayOf(android.R.attr.state_empty)),
                // 1: BACKGROUND_TYPE_NORMAL
                KeyBackgroundState(intArrayOf()),
                // 2: BACKGROUND_TYPE_FUNCTIONAL
                KeyBackgroundState(intArrayOf()),
                // 3: BACKGROUND_TYPE_STICKY_OFF
                KeyBackgroundState(intArrayOf(android.R.attr.state_checkable)),
                // 4: BACKGROUND_TYPE_STICKY_ON
                KeyBackgroundState(intArrayOf(android.R.attr.state_checkable, android.R.attr.state_checked)),
                // 5: BACKGROUND_TYPE_ACTION
                KeyBackgroundState(intArrayOf(android.R.attr.state_active)),
                // 6: BACKGROUND_TYPE_SPACEBAR
                KeyBackgroundState(intArrayOf()),
            )

            private fun KeyBackgroundState(vararg attrs: Int): KeyBackgroundState {
                val releasedState = attrs
                val pressedState = attrs.copyOf(attrs.size + 1)
                pressedState[attrs.size] = android.R.attr.state_pressed
                return KeyBackgroundState(releasedState, pressedState)
            }
        }
    }

    fun selectBackgroundDrawable(
        keyBackground: Drawable,
        functionalKeyBackground: Drawable,
        spacebarBackground: Drawable
    ): Drawable {
        val background = when (backgroundType) {
            BACKGROUND_TYPE_FUNCTIONAL -> functionalKeyBackground
            BACKGROUND_TYPE_SPACEBAR -> spacebarBackground
            else -> keyBackground
        }
        val state = KeyBackgroundState.STATES[backgroundType].getState(isPressed)
        background.state = state
        return background
    }

    class Spacer : Key {
        constructor(
            keyAttr: TypedArray,
            keyStyle: KeyStyle,
            params: KeyboardParams,
            row: KeyboardRow
        ) : super(null, keyAttr, keyStyle, params, row)
    }
}
