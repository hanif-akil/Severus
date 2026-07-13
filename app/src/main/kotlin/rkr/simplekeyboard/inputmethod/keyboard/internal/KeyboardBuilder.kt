/*
 * Copyright (C) 2012 The Android Open Source Project
 * Copyright (C) 2024 wittmane
 * Copyright (C) 2024 Raimondas Rimkus
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

package rkr.simplekeyboard.inputmethod.keyboard.internal

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.content.res.XmlResourceParser
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.util.Locale
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.keyboard.Key
import rkr.simplekeyboard.inputmethod.keyboard.Keyboard
import rkr.simplekeyboard.inputmethod.keyboard.KeyboardId
import rkr.simplekeyboard.inputmethod.keyboard.KeyboardTheme
import rkr.simplekeyboard.inputmethod.latin.common.StringUtils
import rkr.simplekeyboard.inputmethod.latin.utils.ResourceUtils
import rkr.simplekeyboard.inputmethod.latin.utils.XmlParseUtils
import rkr.simplekeyboard.inputmethod.latin.utils.XmlParseUtils.ParseException

open class KeyboardBuilder<KP : KeyboardParams>(protected val mContext: Context, protected val mParams: KP) {
    private val mResources: Resources = mContext.resources
    private var mCurrentY = 0f
    private var mCurrentRow: KeyboardRow? = null
    private var mPreviousKeyInRow: Key? = null
    private var mKeyboardDefined = false
    private var mIndent = 0

    init {
        mParams.mGridWidth = mResources.getInteger(R.integer.config_keyboard_grid_width)
        mParams.mGridHeight = mResources.getInteger(R.integer.config_keyboard_grid_height)
    }

    fun setAllowRedundantMoreKes(enabled: Boolean) {
        mParams.mAllowRedundantMoreKeys = enabled
    }

    fun load(xmlId: Int, id: KeyboardId): KeyboardBuilder<KP> {
        mParams.mId = id
        val parser = mResources.getXml(xmlId)
        try {
            parseKeyboard(parser, false)
            if (!mKeyboardDefined) throw ParseException("No $TAG_KEYBOARD tag was found")
        } catch (e: XmlPullParserException) {
            Log.w(BUILDER_TAG, "keyboard XML parse error", e)
            throw IllegalArgumentException(e.message, e)
        } catch (e: IOException) {
            Log.w(BUILDER_TAG, "keyboard XML parse error", e)
            throw RuntimeException(e.message, e)
        } finally {
            parser.close()
        }
        return this
    }

    fun build(): Keyboard = Keyboard(mParams)

    private fun startTag(format: String, vararg args: Any) {
        Log.d(BUILDER_TAG, String.format(spaces(++mIndent * 2) + format, *args))
    }

    private fun endTag(format: String, vararg args: Any) {
        Log.d(BUILDER_TAG, String.format(spaces(mIndent-- * 2) + format, *args))
    }

    private fun startEndTag(format: String, vararg args: Any) {
        Log.d(BUILDER_TAG, String.format(spaces(++mIndent * 2) + format, *args))
        mIndent--
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseKeyboard(parser: XmlPullParser, skip: Boolean) {
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            val event = parser.next()
            if (event == XmlPullParser.START_TAG) {
                val tag = parser.name
                if (TAG_KEYBOARD == tag) {
                    if (DEBUG) startTag("<%s> %s%s", TAG_KEYBOARD, mParams.mId, if (skip) " skipped" else "")
                    if (!skip) {
                        if (mKeyboardDefined) throw ParseException("Only one $TAG_KEYBOARD tag can be defined", parser)
                        mKeyboardDefined = true
                        parseKeyboardAttributes(parser)
                    }
                    parseKeyboardContent(parser, skip)
                } else if (TAG_SWITCH == tag) {
                    parseSwitchKeyboard(parser, skip)
                } else {
                    throw XmlParseUtils.IllegalStartTag(parser, tag, TAG_KEYBOARD)
                }
            } else if (event == XmlPullParser.END_TAG) {
                val tag = parser.name
                if (DEBUG) endTag("</%s>", tag)
                if (TAG_CASE == tag || TAG_DEFAULT == tag) return
                throw XmlParseUtils.IllegalEndTag(parser, tag, TAG_ROW)
            }
        }
    }

    private fun parseKeyboardAttributes(parser: XmlPullParser) {
        val attr = Xml.asAttributeSet(parser)
        val keyboardAttr = mContext.obtainStyledAttributes(attr, R.styleable.Keyboard, R.attr.keyboardStyle, R.style.Keyboard)
        val keyAttr = mResources.obtainAttributes(attr, R.styleable.Keyboard_Key)
        try {
            val params = mParams
            val height = params.mId!!.mHeight
            val width = params.mId!!.mWidth
            val bottomOffset = params.mId!!.mBottomOffset
            val bonusHeight = keyboardAttr.getFraction(R.styleable.Keyboard_bonusHeight, height, height, 0).toInt()
            params.mOccupiedHeight = height + bonusHeight + bottomOffset
            params.mOccupiedWidth = width
            params.mTopPadding = ResourceUtils.getDimensionOrFraction(keyboardAttr, R.styleable.Keyboard_keyboardTopPadding, height, 0f)
            params.mBottomPadding = ResourceUtils.getDimensionOrFraction(keyboardAttr, R.styleable.Keyboard_keyboardBottomPadding, height, 0f)
            params.mLeftPadding = ResourceUtils.getDimensionOrFraction(keyboardAttr, R.styleable.Keyboard_keyboardLeftPadding, width, 0f)
            params.mRightPadding = ResourceUtils.getDimensionOrFraction(keyboardAttr, R.styleable.Keyboard_keyboardRightPadding, width, 0f)
            params.mHorizontalGap = keyboardAttr.getFraction(R.styleable.Keyboard_horizontalGap, width, width, 0f)
            val baseWidth = params.mOccupiedWidth - params.mLeftPadding - params.mRightPadding + params.mHorizontalGap
            params.mBaseWidth = baseWidth
            params.mDefaultKeyPaddedWidth = ResourceUtils.getFraction(keyAttr, R.styleable.Keyboard_Key_keyWidth, baseWidth, baseWidth / DEFAULT_KEYBOARD_COLUMNS)
            params.mVerticalGap = keyboardAttr.getFraction(R.styleable.Keyboard_verticalGap, height, height, 0f)
            val baseHeight = params.mOccupiedHeight - params.mTopPadding - params.mBottomPadding + params.mVerticalGap - bottomOffset
            params.mBaseHeight = baseHeight
            params.mDefaultRowHeight = ResourceUtils.getDimensionOrFraction(keyboardAttr, R.styleable.Keyboard_rowHeight, baseHeight, baseHeight / DEFAULT_KEYBOARD_ROWS)
            params.mKeyVisualAttributes = KeyVisualAttributes.newInstance(keyAttr)
            params.mMoreKeysTemplate = keyboardAttr.getResourceId(R.styleable.Keyboard_moreKeysTemplate, 0)
            params.mMaxMoreKeysKeyboardColumn = keyAttr.getInt(R.styleable.Keyboard_Key_maxMoreKeysColumn, 5)
            params.mIconsSet.loadIcons(mResources, mContext.theme)
            params.mTextsSet.setLocale(params.mId!!.getLocale(), mContext)
        } finally {
            keyAttr.recycle()
            keyboardAttr.recycle()
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseKeyboardContent(parser: XmlPullParser, skip: Boolean) {
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            val event = parser.next()
            if (event == XmlPullParser.START_TAG) {
                val tag = parser.name
                when {
                    TAG_ROW == tag -> {
                        val row = parseRowAttributes(parser)
                        if (DEBUG) startTag("<%s>%s", TAG_ROW, if (skip) " skipped" else "")
                        if (!skip) startRow(row)
                        parseRowContent(parser, row, skip)
                    }
                    TAG_INCLUDE == tag -> parseIncludeKeyboardContent(parser, skip)
                    TAG_SWITCH == tag -> parseSwitchKeyboardContent(parser, skip)
                    TAG_KEY_STYLE == tag -> parseKeyStyle(parser, skip)
                    else -> throw XmlParseUtils.IllegalStartTag(parser, tag, TAG_ROW)
                }
            } else if (event == XmlPullParser.END_TAG) {
                val tag = parser.name
                if (DEBUG) endTag("</%s>", tag)
                if (TAG_KEYBOARD == tag) {
                    endKeyboard()
                    return
                }
                if (TAG_CASE == tag || TAG_DEFAULT == tag || TAG_MERGE == tag) return
                throw XmlParseUtils.IllegalEndTag(parser, tag, TAG_ROW)
            }
        }
    }

    @Throws(XmlPullParserException::class)
    private fun parseRowAttributes(parser: XmlPullParser): KeyboardRow {
        val attr = Xml.asAttributeSet(parser)
        val keyboardAttr = mResources.obtainAttributes(attr, R.styleable.Keyboard)
        try {
            if (keyboardAttr.hasValue(R.styleable.Keyboard_horizontalGap)) throw XmlParseUtils.IllegalAttribute(parser, TAG_ROW, "horizontalGap")
            if (keyboardAttr.hasValue(R.styleable.Keyboard_verticalGap)) throw XmlParseUtils.IllegalAttribute(parser, TAG_ROW, "verticalGap")
            return KeyboardRow(mResources, mParams, parser, mCurrentY)
        } finally {
            keyboardAttr.recycle()
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseRowContent(parser: XmlPullParser, row: KeyboardRow, skip: Boolean) {
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            val event = parser.next()
            if (event == XmlPullParser.START_TAG) {
                val tag = parser.name
                when {
                    TAG_KEY == tag -> parseKey(parser, row, skip)
                    TAG_SPACER == tag -> parseSpacer(parser, row, skip)
                    TAG_INCLUDE == tag -> parseIncludeRowContent(parser, row, skip)
                    TAG_SWITCH == tag -> parseSwitchRowContent(parser, row, skip)
                    TAG_KEY_STYLE == tag -> parseKeyStyle(parser, skip)
                    else -> throw XmlParseUtils.IllegalStartTag(parser, tag, TAG_ROW)
                }
            } else if (event == XmlPullParser.END_TAG) {
                val tag = parser.name
                if (DEBUG) endTag("</%s>", tag)
                if (TAG_ROW == tag) {
                    if (!skip) endRow(row)
                    return
                }
                if (TAG_CASE == tag || TAG_DEFAULT == tag || TAG_MERGE == tag) return
                throw XmlParseUtils.IllegalEndTag(parser, tag, TAG_ROW)
            }
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseKey(parser: XmlPullParser, row: KeyboardRow, skip: Boolean) {
        if (skip) {
            XmlParseUtils.checkEndTag(TAG_KEY, parser)
            if (DEBUG) startEndTag("<%s /> skipped", TAG_KEY)
            return
        }
        val keyAttr = mResources.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.Keyboard_Key)
        val keyStyle = mParams.mKeyStyles.getKeyStyle(keyAttr, parser)
        val keySpec = keyStyle.getString(keyAttr, R.styleable.Keyboard_Key_keySpec)
        if (TextUtils.isEmpty(keySpec)) throw ParseException("Empty keySpec", parser)
        val key = Key(keySpec, keyAttr, keyStyle, mParams, row)
        keyAttr.recycle()
        if (DEBUG) startEndTag("<%s %s moreKeys=%s />", TAG_KEY, key, key.moreKeys?.contentToString())
        XmlParseUtils.checkEndTag(TAG_KEY, parser)
        endKey(key, row)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseSpacer(parser: XmlPullParser, row: KeyboardRow, skip: Boolean) {
        if (skip) {
            XmlParseUtils.checkEndTag(TAG_SPACER, parser)
            if (DEBUG) startEndTag("<%s /> skipped", TAG_SPACER)
            return
        }
        val keyAttr = mResources.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.Keyboard_Key)
        val keyStyle = mParams.mKeyStyles.getKeyStyle(keyAttr, parser)
        val spacer = Key.Spacer(keyAttr, keyStyle, mParams, row)
        keyAttr.recycle()
        if (DEBUG) startEndTag("<%s />", TAG_SPACER)
        XmlParseUtils.checkEndTag(TAG_SPACER, parser)
        endKey(spacer, row)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseIncludeKeyboardContent(parser: XmlPullParser, skip: Boolean) {
        parseIncludeInternal(parser, null, skip)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseIncludeRowContent(parser: XmlPullParser, row: KeyboardRow, skip: Boolean) {
        parseIncludeInternal(parser, row, skip)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseIncludeInternal(parser: XmlPullParser, row: KeyboardRow?, skip: Boolean) {
        if (skip) {
            XmlParseUtils.checkEndTag(TAG_INCLUDE, parser)
            if (DEBUG) startEndTag("</%s> skipped", TAG_INCLUDE)
            return
        }
        val attr = Xml.asAttributeSet(parser)
        val keyboardAttr = mResources.obtainAttributes(attr, R.styleable.Keyboard_Include)
        val includeAttr = mResources.obtainAttributes(attr, R.styleable.Keyboard)
        mParams.mDefaultRowHeight = ResourceUtils.getDimensionOrFraction(includeAttr, R.styleable.Keyboard_rowHeight, mParams.mBaseHeight, mParams.mDefaultRowHeight)
        val keyAttr = mResources.obtainAttributes(attr, R.styleable.Keyboard_Key)
        var keyboardLayout = 0
        try {
            XmlParseUtils.checkAttributeExists(keyboardAttr, R.styleable.Keyboard_Include_keyboardLayout, "keyboardLayout", TAG_INCLUDE, parser)
            keyboardLayout = keyboardAttr.getResourceId(R.styleable.Keyboard_Include_keyboardLayout, 0)
            if (row != null) {
                row.updateXPos(keyAttr)
                row.pushRowAttributes(keyAttr)
            }
        } finally {
            keyboardAttr.recycle()
            keyAttr.recycle()
            includeAttr.recycle()
        }
        XmlParseUtils.checkEndTag(TAG_INCLUDE, parser)
        if (DEBUG) startEndTag("<%s keyboardLayout=%s />", TAG_INCLUDE, mResources.getResourceEntryName(keyboardLayout))
        val parserForInclude = mResources.getXml(keyboardLayout)
        try {
            parseMerge(parserForInclude, row, skip)
        } finally {
            row?.popRowAttributes()
            parserForInclude.close()
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseMerge(parser: XmlPullParser, row: KeyboardRow?, skip: Boolean) {
        if (DEBUG) startTag("<%s>", TAG_MERGE)
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            val event = parser.next()
            if (event == XmlPullParser.START_TAG) {
                val tag = parser.name
                if (TAG_MERGE == tag) {
                    if (row == null) parseKeyboardContent(parser, skip)
                    else parseRowContent(parser, row, skip)
                    return
                }
                throw ParseException("Included keyboard layout must have <merge> root element", parser)
            }
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseSwitchKeyboard(parser: XmlPullParser, skip: Boolean) {
        parseSwitchInternal(parser, true, null, skip)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseSwitchKeyboardContent(parser: XmlPullParser, skip: Boolean) {
        parseSwitchInternal(parser, false, null, skip)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseSwitchRowContent(parser: XmlPullParser, row: KeyboardRow, skip: Boolean) {
        parseSwitchInternal(parser, false, row, skip)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseSwitchInternal(parser: XmlPullParser, parseKeyboard: Boolean, row: KeyboardRow?, skip: Boolean) {
        if (DEBUG) startTag("<%s> %s", TAG_SWITCH, mParams.mId)
        var selected = false
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            val event = parser.next()
            if (event == XmlPullParser.START_TAG) {
                val tag = parser.name
                when {
                    TAG_CASE == tag -> selected = selected or parseCase(parser, parseKeyboard, row, selected || skip)
                    TAG_DEFAULT == tag -> selected = selected or parseDefault(parser, parseKeyboard, row, selected || skip)
                    else -> throw XmlParseUtils.IllegalStartTag(parser, tag, TAG_SWITCH)
                }
            } else if (event == XmlPullParser.END_TAG) {
                val tag = parser.name
                if (TAG_SWITCH == tag) {
                    if (DEBUG) endTag("</%s>", TAG_SWITCH)
                    return
                }
                throw XmlParseUtils.IllegalEndTag(parser, tag, TAG_SWITCH)
            }
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseCase(parser: XmlPullParser, parseKeyboard: Boolean, row: KeyboardRow?, skip: Boolean): Boolean {
        val selected = parseCaseCondition(parser)
        if (parseKeyboard) parseKeyboard(parser, !selected || skip)
        else if (row == null) parseKeyboardContent(parser, !selected || skip)
        else parseRowContent(parser, row, !selected || skip)
        return selected
    }

    private fun parseCaseCondition(parser: XmlPullParser): Boolean {
        val id = mParams.mId ?: return true
        val attr = Xml.asAttributeSet(parser)
        val caseAttr = mResources.obtainAttributes(attr, R.styleable.Keyboard_Case)
        if (DEBUG) startTag("<%s>", TAG_CASE)
        try {
            val keyboardLayoutSetMatched = matchString(caseAttr, R.styleable.Keyboard_Case_keyboardLayoutSet, id.mSubtype.keyboardLayoutSet)
            val keyboardLayoutSetElementMatched = matchTypedValue(caseAttr, R.styleable.Keyboard_Case_keyboardLayoutSetElement, id.mElementId, KeyboardId.elementIdToName(id.mElementId))
            val keyboardThemeMatched = matchTypedValue(caseAttr, R.styleable.Keyboard_Case_keyboardTheme, id.mThemeId, KeyboardTheme.getKeyboardThemeName(id.mThemeId))
            val modeMatched = matchTypedValue(caseAttr, R.styleable.Keyboard_Case_mode, id.mMode, KeyboardId.modeName(id.mMode))
            val navigateNextMatched = matchBoolean(caseAttr, R.styleable.Keyboard_Case_navigateNext, id.navigateNext())
            val navigatePreviousMatched = matchBoolean(caseAttr, R.styleable.Keyboard_Case_navigatePrevious, id.navigatePrevious())
            val passwordInputMatched = matchBoolean(caseAttr, R.styleable.Keyboard_Case_passwordInput, id.passwordInput())
            val clobberSettingsKeyMatched = matchBoolean(caseAttr, R.styleable.Keyboard_Case_clobberSettingsKey, id.mClobberSettingsKey)
            val languageSwitchKeyEnabledMatched = matchBoolean(caseAttr, R.styleable.Keyboard_Case_languageSwitchKeyEnabled, id.mLanguageSwitchKeyEnabled)
            val isMultiLineMatched = matchBoolean(caseAttr, R.styleable.Keyboard_Case_isMultiLine, id.isMultiLine())
            val imeActionMatched = matchInteger(caseAttr, R.styleable.Keyboard_Case_imeAction, id.imeAction())
            val isIconDefinedMatched = isIconDefined(caseAttr, R.styleable.Keyboard_Case_isIconDefined, mParams.mIconsSet)
            val locale = id.getLocale()
            val localeCodeMatched = matchLocaleCodes(caseAttr, locale)
            val languageCodeMatched = matchLanguageCodes(caseAttr, locale)
            val countryCodeMatched = matchCountryCodes(caseAttr, locale)
            val showMoreKeysMatched = matchBoolean(caseAttr, R.styleable.Keyboard_Case_showExtraChars, id.mShowMoreKeys)
            val showNumberRowMatched = matchBoolean(caseAttr, R.styleable.Keyboard_Case_showNumberRow, id.mShowNumberRow)
            return keyboardLayoutSetMatched && keyboardLayoutSetElementMatched
                    && keyboardThemeMatched && modeMatched && navigateNextMatched
                    && navigatePreviousMatched && passwordInputMatched
                    && languageSwitchKeyEnabledMatched && clobberSettingsKeyMatched
                    && isMultiLineMatched && imeActionMatched && isIconDefinedMatched
                    && localeCodeMatched && languageCodeMatched && countryCodeMatched
                    && showMoreKeysMatched && showNumberRowMatched
        } finally {
            caseAttr.recycle()
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseDefault(parser: XmlPullParser, parseKeyboard: Boolean, row: KeyboardRow?, skip: Boolean): Boolean {
        if (DEBUG) startTag("<%s>", TAG_DEFAULT)
        if (parseKeyboard) parseKeyboard(parser, skip)
        else if (row == null) parseKeyboardContent(parser, skip)
        else parseRowContent(parser, row, skip)
        return true
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseKeyStyle(parser: XmlPullParser, skip: Boolean) {
        val attr = Xml.asAttributeSet(parser)
        val keyStyleAttr = mResources.obtainAttributes(attr, R.styleable.Keyboard_KeyStyle)
        val keyAttrs = mResources.obtainAttributes(attr, R.styleable.Keyboard_Key)
        try {
            if (!keyStyleAttr.hasValue(R.styleable.Keyboard_KeyStyle_styleName)) {
                throw ParseException("<$TAG_KEY_STYLE/> needs styleName attribute", parser)
            }
            if (DEBUG) startEndTag("<%s styleName=%s />%s", TAG_KEY_STYLE, keyStyleAttr.getString(R.styleable.Keyboard_KeyStyle_styleName), if (skip) " skipped" else "")
            if (!skip) mParams.mKeyStyles.parseKeyStyleAttributes(keyStyleAttr, keyAttrs, parser)
        } finally {
            keyStyleAttr.recycle()
            keyAttrs.recycle()
        }
        XmlParseUtils.checkEndTag(TAG_KEY_STYLE, parser)
    }

    private fun startRow(row: KeyboardRow) {
        mCurrentRow = row
        mPreviousKeyInRow = null
    }

    private fun endRow(row: KeyboardRow) {
        if (mCurrentRow == null) throw RuntimeException("orphan end row tag")
        if (mPreviousKeyInRow != null && !mPreviousKeyInRow!!.isSpacer) {
            setKeyHitboxRightEdge(mPreviousKeyInRow!!, mParams.mOccupiedWidth.toFloat())
            mPreviousKeyInRow = null
        }
        mCurrentY += row.getRowHeight()
        mCurrentRow = null
    }

    private fun endKey(key: Key, row: KeyboardRow) {
        mParams.onAddKey(key)
        if (mPreviousKeyInRow != null && !mPreviousKeyInRow!!.isSpacer) {
            setKeyHitboxRightEdge(mPreviousKeyInRow!!, row.getKeyX() - row.getKeyLeftPadding())
        }
        mPreviousKeyInRow = key
    }

    private fun setKeyHitboxRightEdge(key: Key, xPos: Float) {
        val keyRight = (key.x + key.width).toInt()
        val padding = (xPos - keyRight).toInt()
        key.setHitboxRightEdge(padding + keyRight)
    }

    private fun endKeyboard() {
        mParams.removeRedundantMoreKeys()
    }

    companion object {
        private const val BUILDER_TAG = "Keyboard.Builder"
        private const val DEBUG = false

        const val TAG_KEY_STYLE = "key-style"
        private const val TAG_KEYBOARD = "Keyboard"
        private const val TAG_ROW = "Row"
        private const val TAG_KEY = "Key"
        private const val TAG_SPACER = "Spacer"
        private const val TAG_INCLUDE = "include"
        private const val TAG_MERGE = "merge"
        private const val TAG_SWITCH = "switch"
        private const val TAG_CASE = "case"
        private const val TAG_DEFAULT = "default"

        private const val DEFAULT_KEYBOARD_COLUMNS = 10
        private const val DEFAULT_KEYBOARD_ROWS = 4

        private const val SPACES = "                                             "

        private fun spaces(count: Int): String {
            return if (count < SPACES.length) SPACES.substring(0, count) else SPACES
        }

        private fun matchLocaleCodes(caseAttr: TypedArray, locale: Locale): Boolean {
            return matchString(caseAttr, R.styleable.Keyboard_Case_localeCode, locale.toString())
        }

        private fun matchLanguageCodes(caseAttr: TypedArray, locale: Locale): Boolean {
            return matchString(caseAttr, R.styleable.Keyboard_Case_languageCode, locale.language)
        }

        private fun matchCountryCodes(caseAttr: TypedArray, locale: Locale): Boolean {
            return matchString(caseAttr, R.styleable.Keyboard_Case_countryCode, locale.country)
        }

        private fun matchInteger(a: TypedArray, index: Int, value: Int): Boolean {
            return !a.hasValue(index) || a.getInt(index, 0) == value
        }

        private fun matchBoolean(a: TypedArray, index: Int, value: Boolean): Boolean {
            return !a.hasValue(index) || a.getBoolean(index, false) == value
        }

        private fun matchString(a: TypedArray, index: Int, value: String): Boolean {
            return !a.hasValue(index) || StringUtils.containsInArray(value, a.getString(index)!!.split("\\|".toRegex()).toTypedArray())
        }

        private fun matchTypedValue(a: TypedArray, index: Int, intValue: Int, strValue: String): Boolean {
            val v = a.peekValue(index) ?: return true
            if (ResourceUtils.isIntegerValue(v)) return intValue == a.getInt(index, 0)
            if (ResourceUtils.isStringValue(v)) return StringUtils.containsInArray(strValue, a.getString(index)!!.split("\\|".toRegex()).toTypedArray())
            return false
        }

        private fun isIconDefined(a: TypedArray, index: Int, iconsSet: KeyboardIconsSet): Boolean {
            if (!a.hasValue(index)) return true
            val iconName = a.getString(index)
            val iconId = KeyboardIconsSet.getIconId(iconName!!)
            return iconsSet.getIconDrawable(iconId) != null
        }
    }
}
