package rkr.simplekeyboard.inputmethod.keyboard.internal

import android.content.res.TypedArray
import android.util.Log
import android.util.SparseArray
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.latin.utils.XmlParseUtils

class KeyStylesSet(private val mTextsSet: KeyboardTextsSet) {
    private val mStyles = HashMap<String, KeyStyle>()
    private val mEmptyKeyStyle = EmptyKeyStyle(mTextsSet)

    init {
        mStyles[EMPTY_STYLE_NAME] = mEmptyKeyStyle
    }

    private class EmptyKeyStyle(textsSet: KeyboardTextsSet) : KeyStyle(textsSet) {
        override fun getStringArray(a: TypedArray, index: Int): Array<String>? = parseStringArray(a, index)
        override fun getString(a: TypedArray, index: Int): String? = parseString(a, index)
        override fun getInt(a: TypedArray, index: Int, defaultValue: Int): Int = a.getInt(index, defaultValue)
        override fun getFlags(a: TypedArray, index: Int): Int = a.getInt(index, 0)
    }

    private class DeclaredKeyStyle(
        private val mParentStyleName: String,
        textsSet: KeyboardTextsSet,
        private val mStyles: HashMap<String, KeyStyle>
    ) : KeyStyle(textsSet) {
        private val mStyleAttributes = SparseArray<Any>()

        override fun getStringArray(a: TypedArray, index: Int): Array<String>? {
            if (a.hasValue(index)) return parseStringArray(a, index)
            val value = mStyleAttributes.get(index) as? Array<*> as? Array<String>
            if (value != null) return value.copyOf()
            val parentStyle = mStyles[mParentStyleName]!!
            return parentStyle.getStringArray(a, index)
        }

        override fun getString(a: TypedArray, index: Int): String? {
            if (a.hasValue(index)) return parseString(a, index)
            val value = mStyleAttributes.get(index) as? String
            if (value != null) return value
            val parentStyle = mStyles[mParentStyleName]!!
            return parentStyle.getString(a, index)
        }

        override fun getInt(a: TypedArray, index: Int, defaultValue: Int): Int {
            if (a.hasValue(index)) return a.getInt(index, defaultValue)
            val value = mStyleAttributes.get(index) as? Int
            if (value != null) return value
            val parentStyle = mStyles[mParentStyleName]!!
            return parentStyle.getInt(a, index, defaultValue)
        }

        override fun getFlags(a: TypedArray, index: Int): Int {
            val parentFlags = mStyles[mParentStyleName]!!.getFlags(a, index)
            val value = mStyleAttributes.get(index) as? Int
            val styleFlags = value ?: 0
            val flags = a.getInt(index, 0)
            return flags or styleFlags or parentFlags
        }

        fun readKeyAttributes(keyAttr: TypedArray) {
            readString(keyAttr, R.styleable.Keyboard_Key_altCode)
            readString(keyAttr, R.styleable.Keyboard_Key_keySpec)
            readString(keyAttr, R.styleable.Keyboard_Key_keyHintLabel)
            readStringArray(keyAttr, R.styleable.Keyboard_Key_moreKeys)
            readStringArray(keyAttr, R.styleable.Keyboard_Key_additionalMoreKeys)
            readFlags(keyAttr, R.styleable.Keyboard_Key_keyLabelFlags)
            readInt(keyAttr, R.styleable.Keyboard_Key_maxMoreKeysColumn)
            readInt(keyAttr, R.styleable.Keyboard_Key_backgroundType)
            readFlags(keyAttr, R.styleable.Keyboard_Key_keyActionFlags)
        }

        private fun readString(a: TypedArray, index: Int) {
            if (a.hasValue(index)) mStyleAttributes.put(index, parseString(a, index))
        }

        private fun readInt(a: TypedArray, index: Int) {
            if (a.hasValue(index)) mStyleAttributes.put(index, a.getInt(index, 0))
        }

        private fun readFlags(a: TypedArray, index: Int) {
            if (a.hasValue(index)) {
                val value = mStyleAttributes.get(index) as? Int ?: 0
                mStyleAttributes.put(index, a.getInt(index, 0) or value)
            }
        }

        private fun readStringArray(a: TypedArray, index: Int) {
            if (a.hasValue(index)) mStyleAttributes.put(index, parseStringArray(a, index))
        }
    }

    @Throws(XmlPullParserException::class)
    fun parseKeyStyleAttributes(keyStyleAttr: TypedArray, keyAttrs: TypedArray, parser: XmlPullParser) {
        val styleName = keyStyleAttr.getString(R.styleable.Keyboard_KeyStyle_styleName)
            ?: throw XmlParseUtils.ParseException("${KeyboardBuilder.TAG_KEY_STYLE} has no styleName attribute", parser)
        if (DEBUG) {
            Log.d(TAG, String.format("<%s styleName=%s />", KeyboardBuilder.TAG_KEY_STYLE, styleName))
            if (mStyles.containsKey(styleName)) {
                Log.d(TAG, "${KeyboardBuilder.TAG_KEY_STYLE} $styleName is overridden at ${parser.positionDescription}")
            }
        }
        val parentStyleInAttr = keyStyleAttr.getString(R.styleable.Keyboard_KeyStyle_parentStyle)
        if (parentStyleInAttr != null && !mStyles.containsKey(parentStyleInAttr)) {
            throw XmlParseUtils.ParseException("Unknown parentStyle $parentStyleInAttr", parser)
        }
        val parentStyleName = parentStyleInAttr ?: EMPTY_STYLE_NAME
        val style = DeclaredKeyStyle(parentStyleName, mTextsSet, mStyles)
        style.readKeyAttributes(keyAttrs)
        mStyles[styleName] = style
    }

    @Throws(XmlParseUtils.ParseException::class)
    fun getKeyStyle(keyAttr: TypedArray, parser: XmlPullParser): KeyStyle {
        val styleName = keyAttr.getString(R.styleable.Keyboard_Key_keyStyle)
            ?: return mEmptyKeyStyle
        return mStyles[styleName] ?: throw XmlParseUtils.ParseException("Unknown key style: $styleName", parser)
    }

    companion object {
        private const val TAG = "KeyStylesSet"
        private const val DEBUG = false
        private const val EMPTY_STYLE_NAME = "<empty>"
    }
}
