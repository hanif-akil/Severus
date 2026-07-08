package rkr.simplekeyboard.inputmethod.keyboard.internal

import android.content.Context
import android.content.res.TypedArray
import android.content.res.XmlResourceParser
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.keyboard.Key
import rkr.simplekeyboard.inputmethod.keyboard.Keyboard
import rkr.simplekeyboard.inputmethod.keyboard.KeyboardId
import rkr.simplekeyboard.inputmethod.keyboard.KeyboardTheme
import rkr.simplekeyboard.inputmethod.latin.common.StringUtils
import rkr.simplekeyboard.inputmethod.latin.utils.ResourceUtils
import rkr.simplekeyboard.inputmethod.latin.utils.XmlParseUtils

open class KeyboardBuilder<KP : KeyboardParams>(val mContext: Context, protected val mParams: KP) {
    private var mCurrentY = 0f
    private var mCurrentRow: KeyboardRow? = null
    private var mPreviousKeyInRow: Key? = null
    private var mKeyboardDefined = false

    init {
        val res = mContext.resources
        mParams.mGridWidth = res.getInteger(R.integer.config_keyboard_grid_width)
        mParams.mGridHeight = res.getInteger(R.integer.config_keyboard_grid_height)
    }

    fun setAllowRedundantMoreKes(enabled: Boolean) { mParams.mAllowRedundantMoreKeys = enabled }

    fun load(xmlId: Int, id: KeyboardId): KeyboardBuilder<KP> {
        mParams.mId = id
        val parser = mResources.getXml(xmlId)
        try { parseKeyboard(parser, false); if (!mKeyboardDefined) throw XmlParseUtils.ParseException("No $TAG_KEYBOARD tag was found") }
        catch (e: Exception) { throw if (e is XmlParseUtils.ParseException) e else IllegalArgumentException(e.message, e) }
        finally { parser.close() }
        return this
    }

    private val mResources get() = mContext.resources

    open fun build(): Keyboard = Keyboard(mParams)

    private fun parseKeyboard(parser: XmlPullParser, skip: Boolean) {
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            val event = parser.next()
            if (event == XmlPullParser.START_TAG) {
                val tag = parser.name
                if (tag == TAG_KEYBOARD) {
                    if (!skip) { if (mKeyboardDefined) throw XmlParseUtils.ParseException("Only one $TAG_KEYBOARD tag can be defined", parser); mKeyboardDefined = true; parseKeyboardAttributes(parser) }
                    parseKeyboardContent(parser, skip)
                } else if (tag == TAG_SWITCH) { parseSwitchKeyboard(parser, skip) }
                else throw XmlParseUtils.IllegalStartTag(parser, tag, TAG_KEYBOARD)
            } else if (event == XmlPullParser.END_TAG) {
                val tag = parser.name
                if (tag == TAG_CASE || tag == TAG_DEFAULT) return
                throw XmlParseUtils.IllegalEndTag(parser, tag, TAG_ROW)
            }
        }
    }

    private fun parseKeyboardAttributes(parser: XmlPullParser) {
        val attr = Xml.asAttributeSet(parser)
        val keyboardAttr = mContext.obtainStyledAttributes(attr, R.styleable.Keyboard, R.attr.keyboardStyle, R.style.Keyboard)
        val keyAttr = mResources.obtainAttributes(attr, R.styleable.Keyboard_Key)
        try {
            val params = mParams; val id = params.mId!!
            val height = id.mHeight; val width = id.mWidth; val bottomOffset = id.mBottomOffset
            val bonusHeight = keyboardAttr.getFraction(R.styleable.Keyboard_bonusHeight, height, height, 0f).toInt()
            params.mOccupiedHeight = height + bonusHeight + bottomOffset; params.mOccupiedWidth = width
            params.mTopPadding = ResourceUtils.getDimensionOrFraction(keyboardAttr, R.styleable.Keyboard_keyboardTopPadding, height, 0f).toInt()
            params.mBottomPadding = ResourceUtils.getDimensionOrFraction(keyboardAttr, R.styleable.Keyboard_keyboardBottomPadding, height, 0f).toInt()
            params.mLeftPadding = ResourceUtils.getDimensionOrFraction(keyboardAttr, R.styleable.Keyboard_keyboardLeftPadding, width, 0f).toInt()
            params.mRightPadding = ResourceUtils.getDimensionOrFraction(keyboardAttr, R.styleable.Keyboard_keyboardRightPadding, width, 0f).toInt()
            params.mHorizontalGap = keyboardAttr.getFraction(R.styleable.Keyboard_horizontalGap, width, width, 0f)
            val baseWidth = params.mOccupiedWidth - params.mLeftPadding - params.mRightPadding + params.mHorizontalGap; params.mBaseWidth = baseWidth.toInt()
            params.mDefaultKeyPaddedWidth = ResourceUtils.getFraction(keyAttr, R.styleable.Keyboard_Key_keyWidth, baseWidth.toFloat(), (baseWidth / DEFAULT_KEYBOARD_COLUMNS).toFloat())
            params.mVerticalGap = keyboardAttr.getFraction(R.styleable.Keyboard_verticalGap, height, height, 0f)
            val baseHeight = params.mOccupiedHeight - params.mTopPadding - params.mBottomPadding + params.mVerticalGap - bottomOffset; params.mBaseHeight = baseHeight.toInt()
            params.mDefaultRowHeight = ResourceUtils.getDimensionOrFraction(keyboardAttr, R.styleable.Keyboard_rowHeight, baseHeight, (baseHeight / DEFAULT_KEYBOARD_ROWS).toFloat()).toInt()
            params.mKeyVisualAttributes = KeyVisualAttributes.newInstance(keyAttr)
            params.mMoreKeysTemplate = keyboardAttr.getResourceId(R.styleable.Keyboard_moreKeysTemplate, 0)
            params.mMaxMoreKeysKeyboardColumn = keyAttr.getInt(R.styleable.Keyboard_Key_maxMoreKeysColumn, 5)
            params.mIconsSet.loadIcons(mResources, mContext.theme)
            params.mTextsSet.setLocale(id.getLocale()!!, mContext)
        } finally { keyAttr.recycle(); keyboardAttr.recycle() }
    }

    private fun parseKeyboardContent(parser: XmlPullParser, skip: Boolean) {
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            val event = parser.next()
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    TAG_ROW -> { val row = parseRowAttributes(parser); if (!skip) startRow(row); parseRowContent(parser, row, skip) }
                    TAG_INCLUDE -> parseIncludeKeyboardContent(parser, skip)
                    TAG_SWITCH -> parseSwitchKeyboardContent(parser, skip)
                    TAG_KEY_STYLE -> parseKeyStyle(parser, skip)
                    else -> throw XmlParseUtils.IllegalStartTag(parser, parser.name, TAG_ROW)
                }
            } else if (event == XmlPullParser.END_TAG) {
                if (parser.name == TAG_KEYBOARD) { endKeyboard(); return }
                if (parser.name == TAG_CASE || parser.name == TAG_DEFAULT || parser.name == TAG_MERGE) return
                throw XmlParseUtils.IllegalEndTag(parser, parser.name, TAG_ROW)
            }
        }
    }

    private fun parseRowAttributes(parser: XmlPullParser): KeyboardRow {
        val attr = Xml.asAttributeSet(parser)
        val keyboardAttr = mResources.obtainAttributes(attr, R.styleable.Keyboard)
        try {
            if (keyboardAttr.hasValue(R.styleable.Keyboard_horizontalGap)) throw XmlParseUtils.IllegalAttribute(parser, TAG_ROW, "horizontalGap")
            if (keyboardAttr.hasValue(R.styleable.Keyboard_verticalGap)) throw XmlParseUtils.IllegalAttribute(parser, TAG_ROW, "verticalGap")
            return KeyboardRow(mResources, mParams, parser, mCurrentY)
        } finally { keyboardAttr.recycle() }
    }

    private fun parseRowContent(parser: XmlPullParser, row: KeyboardRow, skip: Boolean) {
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            val event = parser.next()
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    TAG_KEY -> parseKey(parser, row, skip)
                    TAG_SPACER -> parseSpacer(parser, row, skip)
                    TAG_INCLUDE -> parseIncludeRowContent(parser, row, skip)
                    TAG_SWITCH -> parseSwitchRowContent(parser, row, skip)
                    TAG_KEY_STYLE -> parseKeyStyle(parser, skip)
                    else -> throw XmlParseUtils.IllegalStartTag(parser, parser.name, TAG_ROW)
                }
            } else if (event == XmlPullParser.END_TAG) {
                if (parser.name == TAG_ROW) { if (!skip) endRow(row); return }
                if (parser.name == TAG_CASE || parser.name == TAG_DEFAULT || parser.name == TAG_MERGE) return
                throw XmlParseUtils.IllegalEndTag(parser, parser.name, TAG_ROW)
            }
        }
    }

    private fun parseKey(parser: XmlPullParser, row: KeyboardRow, skip: Boolean) {
        if (skip) { XmlParseUtils.checkEndTag(TAG_KEY, parser); return }
        val keyAttr = mResources.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.Keyboard_Key)
        val keyStyle = mParams.mKeyStyles.getKeyStyle(keyAttr, parser)
        val keySpec = keyStyle.getString(keyAttr, R.styleable.Keyboard_Key_keySpec)
        if (TextUtils.isEmpty(keySpec)) throw XmlParseUtils.ParseException("Empty keySpec", parser)
        val key = Key(keySpec, keyAttr, keyStyle, mParams, row); keyAttr.recycle()
        XmlParseUtils.checkEndTag(TAG_KEY, parser); endKey(key, row)
    }

    private fun parseSpacer(parser: XmlPullParser, row: KeyboardRow, skip: Boolean) {
        if (skip) { XmlParseUtils.checkEndTag(TAG_SPACER, parser); return }
        val keyAttr = mResources.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.Keyboard_Key)
        val keyStyle = mParams.mKeyStyles.getKeyStyle(keyAttr, parser)
        val spacer = Key.Spacer(keyAttr, keyStyle, mParams, row); keyAttr.recycle()
        XmlParseUtils.checkEndTag(TAG_SPACER, parser); endKey(spacer, row)
    }

    private fun parseIncludeKeyboardContent(parser: XmlPullParser, skip: Boolean) { parseIncludeInternal(parser, null, skip) }
    private fun parseIncludeRowContent(parser: XmlPullParser, row: KeyboardRow, skip: Boolean) { parseIncludeInternal(parser, row, skip) }

    private fun parseIncludeInternal(parser: XmlPullParser, row: KeyboardRow?, skip: Boolean) {
        if (skip) { XmlParseUtils.checkEndTag(TAG_INCLUDE, parser); return }
        val attr = Xml.asAttributeSet(parser)
        val keyboardAttr = mResources.obtainAttributes(attr, R.styleable.Keyboard_Include)
        val includeAttr = mResources.obtainAttributes(attr, R.styleable.Keyboard)
        mParams.mDefaultRowHeight = ResourceUtils.getDimensionOrFraction(includeAttr, R.styleable.Keyboard_rowHeight, mParams.mBaseHeight, mParams.mDefaultRowHeight.toFloat()).toInt()
        val keyAttr = mResources.obtainAttributes(attr, R.styleable.Keyboard_Key)
        var keyboardLayout = 0
        try {
            XmlParseUtils.checkAttributeExists(keyboardAttr, R.styleable.Keyboard_Include_keyboardLayout, "keyboardLayout", TAG_INCLUDE, parser)
            keyboardLayout = keyboardAttr.getResourceId(R.styleable.Keyboard_Include_keyboardLayout, 0)
            if (row != null) { row.updateXPos(keyAttr); row.pushRowAttributes(keyAttr) }
        } finally { keyboardAttr.recycle(); keyAttr.recycle(); includeAttr.recycle() }
        XmlParseUtils.checkEndTag(TAG_INCLUDE, parser)
        val parserForInclude = mResources.getXml(keyboardLayout)
        try { parseMerge(parserForInclude, row, skip) }
        finally { row?.popRowAttributes(); parserForInclude.close() }
    }

    private fun parseMerge(parser: XmlPullParser, row: KeyboardRow?, skip: Boolean) {
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            val event = parser.next()
            if (event == XmlPullParser.START_TAG) {
                if (parser.name == TAG_MERGE) { if (row == null) parseKeyboardContent(parser, skip) else parseRowContent(parser, row, skip); return }
                throw XmlParseUtils.ParseException("Included keyboard layout must have <merge> root element", parser)
            }
        }
    }

    private fun parseSwitchKeyboard(parser: XmlPullParser, skip: Boolean) { parseSwitchInternal(parser, true, null, skip) }
    private fun parseSwitchKeyboardContent(parser: XmlPullParser, skip: Boolean) { parseSwitchInternal(parser, false, null, skip) }
    private fun parseSwitchRowContent(parser: XmlPullParser, row: KeyboardRow, skip: Boolean) { parseSwitchInternal(parser, false, row, skip) }

    private fun parseSwitchInternal(parser: XmlPullParser, parseKeyboard: Boolean, row: KeyboardRow?, skip: Boolean) {
        var selected = false
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            val event = parser.next()
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    TAG_CASE -> selected = selected or parseCase(parser, parseKeyboard, row, selected || skip)
                    TAG_DEFAULT -> selected = selected or parseDefault(parser, parseKeyboard, row, selected || skip)
                    else -> throw XmlParseUtils.IllegalStartTag(parser, parser.name, TAG_SWITCH)
                }
            } else if (event == XmlPullParser.END_TAG) {
                if (parser.name == TAG_SWITCH) return
                throw XmlParseUtils.IllegalEndTag(parser, parser.name, TAG_SWITCH)
            }
        }
    }

    private fun parseCase(parser: XmlPullParser, parseKeyboard: Boolean, row: KeyboardRow?, skip: Boolean): Boolean {
        val selected = parseCaseCondition(parser)
        if (parseKeyboard) parseKeyboard(parser, !selected || skip) else if (row == null) parseKeyboardContent(parser, !selected || skip) else parseRowContent(parser, row, !selected || skip)
        return selected
    }

    private fun parseCaseCondition(parser: XmlPullParser): Boolean {
        val id = mParams.mId ?: return true
        val attr = Xml.asAttributeSet(parser)
        val caseAttr = mResources.obtainAttributes(attr, R.styleable.Keyboard_Case)
        try {
            val locale = id.getLocale()
            return matchString(caseAttr, R.styleable.Keyboard_Case_keyboardLayoutSet, id.mSubtype.keyboardLayoutSet) &&
                    matchTypedValue(caseAttr, R.styleable.Keyboard_Case_keyboardLayoutSetElement, id.mElementId, KeyboardId.elementIdToName(id.mElementId)) &&
                    matchTypedValue(caseAttr, R.styleable.Keyboard_Case_keyboardTheme, id.mThemeId, KeyboardTheme.getKeyboardThemeName(id.mThemeId)) &&
                    matchTypedValue(caseAttr, R.styleable.Keyboard_Case_mode, id.mMode, KeyboardId.modeName(id.mMode)) &&
                    matchBoolean(caseAttr, R.styleable.Keyboard_Case_navigateNext, id.navigateNext()) &&
                    matchBoolean(caseAttr, R.styleable.Keyboard_Case_navigatePrevious, id.navigatePrevious()) &&
                    matchBoolean(caseAttr, R.styleable.Keyboard_Case_passwordInput, id.passwordInput()) &&
                    matchBoolean(caseAttr, R.styleable.Keyboard_Case_clobberSettingsKey, id.mClobberSettingsKey) &&
                    matchBoolean(caseAttr, R.styleable.Keyboard_Case_languageSwitchKeyEnabled, id.mLanguageSwitchKeyEnabled) &&
                    matchBoolean(caseAttr, R.styleable.Keyboard_Case_isMultiLine, id.isMultiLine()) &&
                    matchInteger(caseAttr, R.styleable.Keyboard_Case_imeAction, id.imeAction()) &&
                    isIconDefined(caseAttr, R.styleable.Keyboard_Case_isIconDefined, mParams.mIconsSet) &&
                    matchLocaleCodes(caseAttr, locale) && matchLanguageCodes(caseAttr, locale) && matchCountryCodes(caseAttr, locale) &&
                    matchBoolean(caseAttr, R.styleable.Keyboard_Case_showExtraChars, id.mShowMoreKeys) &&
                    matchBoolean(caseAttr, R.styleable.Keyboard_Case_showNumberRow, id.mShowNumberRow)
        } finally { caseAttr.recycle() }
    }

    private fun parseDefault(parser: XmlPullParser, parseKeyboard: Boolean, row: KeyboardRow?, skip: Boolean): Boolean {
        if (parseKeyboard) parseKeyboard(parser, skip) else if (row == null) parseKeyboardContent(parser, skip) else parseRowContent(parser, row, skip)
        return true
    }

    private fun parseKeyStyle(parser: XmlPullParser, skip: Boolean) {
        val attr = Xml.asAttributeSet(parser)
        val keyStyleAttr = mResources.obtainAttributes(attr, R.styleable.Keyboard_KeyStyle)
        val keyAttrs = mResources.obtainAttributes(attr, R.styleable.Keyboard_Key)
        try { if (!skip) mParams.mKeyStyles.parseKeyStyleAttributes(keyStyleAttr, keyAttrs, parser) }
        finally { keyStyleAttr.recycle(); keyAttrs.recycle() }
        XmlParseUtils.checkEndTag(TAG_KEY_STYLE, parser)
    }

    private fun startRow(row: KeyboardRow) { mCurrentRow = row; mPreviousKeyInRow = null }
    private fun endRow(row: KeyboardRow) { if (mPreviousKeyInRow != null && !mPreviousKeyInRow!!.isSpacer) { setKeyHitboxRightEdge(mPreviousKeyInRow!!, mParams.mOccupiedWidth.toFloat()); mPreviousKeyInRow = null }; mCurrentY += row.rowHeight; mCurrentRow = null }
    private fun endKey(key: Key, row: KeyboardRow) { mParams.onAddKey(key); if (mPreviousKeyInRow != null && !mPreviousKeyInRow!!.isSpacer) { setKeyHitboxRightEdge(mPreviousKeyInRow!!, row.getKeyX() - row.getKeyLeftPadding()) }; mPreviousKeyInRow = key }
    private fun setKeyHitboxRightEdge(key: Key, xPos: Float) { val keyRight = key.getX() + key.width; val padding = xPos - keyRight; key.setHitboxRightEdge(Math.round(padding) + keyRight) }
    private fun endKeyboard() { mParams.removeRedundantMoreKeys() }

    companion object {
        private const val BUILDER_TAG = "Keyboard.Builder"
        private const val TAG_KEYBOARD = "Keyboard"
        private const val TAG_ROW = "Row"
        private const val TAG_KEY = "Key"
        private const val TAG_SPACER = "Spacer"
        private const val TAG_INCLUDE = "include"
        private const val TAG_MERGE = "merge"
        private const val TAG_SWITCH = "switch"
        private const val TAG_CASE = "case"
        private const val TAG_DEFAULT = "default"
        const val TAG_KEY_STYLE = "key-style"
        private const val DEFAULT_KEYBOARD_COLUMNS = 10
        private const val DEFAULT_KEYBOARD_ROWS = 4
        private fun matchString(a: TypedArray, index: Int, value: String?): Boolean = !a.hasValue(index) || StringUtils.containsInArray(value ?: "", (a.getString(index) ?: "").split("\\|".toRegex()).toTypedArray())
        private fun matchInteger(a: TypedArray, index: Int, value: Int): Boolean = !a.hasValue(index) || a.getInt(index, 0) == value
        private fun matchBoolean(a: TypedArray, index: Int, value: Boolean): Boolean = !a.hasValue(index) || a.getBoolean(index, false) == value
        private fun matchTypedValue(a: TypedArray, index: Int, intValue: Int, strValue: String?): Boolean { val v = a.peekValue(index) ?: return true; return if (ResourceUtils.isIntegerValue(v)) intValue == a.getInt(index, 0) else if (ResourceUtils.isStringValue(v)) StringUtils.containsInArray(strValue ?: "", (a.getString(index) ?: "").split("\\|".toRegex()).toTypedArray()) else false }
        private fun matchLocaleCodes(caseAttr: TypedArray, locale: java.util.Locale?): Boolean = matchString(caseAttr, R.styleable.Keyboard_Case_localeCode, locale?.toString())
        private fun matchLanguageCodes(caseAttr: TypedArray, locale: java.util.Locale?): Boolean = matchString(caseAttr, R.styleable.Keyboard_Case_languageCode, locale?.language)
        private fun matchCountryCodes(caseAttr: TypedArray, locale: java.util.Locale?): Boolean = matchString(caseAttr, R.styleable.Keyboard_Case_countryCode, locale?.country)
        private fun isIconDefined(a: TypedArray, index: Int, iconsSet: KeyboardIconsSet): Boolean { if (!a.hasValue(index)) return true; val iconName = a.getString(index) ?: return true; return iconsSet.getIconDrawable(KeyboardIconsSet.getIconId(iconName)) != null }
    }
}
