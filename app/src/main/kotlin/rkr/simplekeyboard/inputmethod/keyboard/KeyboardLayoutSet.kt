package rkr.simplekeyboard.inputmethod.keyboard

import android.app.KeyguardManager
import android.content.Context
import android.content.res.Resources
import android.text.InputType
import android.util.Log
import android.util.SparseArray
import android.util.Xml
import android.view.inputmethod.EditorInfo
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeyboardBuilder
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeyboardParams
import rkr.simplekeyboard.inputmethod.keyboard.internal.UniqueKeysCache
import rkr.simplekeyboard.inputmethod.latin.Subtype
import rkr.simplekeyboard.inputmethod.latin.utils.InputTypeUtils
import rkr.simplekeyboard.inputmethod.latin.utils.XmlParseUtils
import java.io.IOException
import java.lang.ref.SoftReference
import java.util.Arrays
import java.util.HashMap

class KeyboardLayoutSet private constructor(private val mContext: Context, private val mParams: Params) {

    class KeyboardLayoutSetException(cause: Throwable, val mKeyboardId: KeyboardId) : RuntimeException(cause)

    private class ElementParams {
        var mKeyboardXmlId = 0
        var mAllowRedundantMoreKeys = false
    }

    class Params {
        var mKeyboardLayoutSetName: String? = null
        var mMode = 0
        var mEditorInfo: EditorInfo? = null
        var mNoSettingsKey = false
        var mLanguageSwitchKeyEnabled = false
        var mSubtype: Subtype? = null
        var mKeyboardThemeId = 0
        var mKeyboardWidth = 0
        var mKeyboardHeight = 0
        var mKeyboardBottomOffset = 0
        var mShowMoreKeys = false
        var mShowNumberRow = false
        val mKeyboardLayoutSetElementIdToParamsMap = SparseArray<ElementParams>()
    }

    fun getKeyboard(baseKeyboardLayoutSetElementId: Int): Keyboard {
        val keyboardLayoutSetElementId = when (mParams.mMode) {
            KeyboardId.MODE_PHONE -> if (baseKeyboardLayoutSetElementId == KeyboardId.ELEMENT_SYMBOLS) KeyboardId.ELEMENT_PHONE_SYMBOLS else KeyboardId.ELEMENT_PHONE
            KeyboardId.MODE_NUMBER, KeyboardId.MODE_DATE, KeyboardId.MODE_TIME, KeyboardId.MODE_DATETIME -> KeyboardId.ELEMENT_NUMBER
            else -> baseKeyboardLayoutSetElementId
        }
        var elementParams = mParams.mKeyboardLayoutSetElementIdToParamsMap.get(keyboardLayoutSetElementId)
        if (elementParams == null) {
            elementParams = mParams.mKeyboardLayoutSetElementIdToParamsMap.get(KeyboardId.ELEMENT_ALPHABET)
        }
        val id = KeyboardId(keyboardLayoutSetElementId, mParams)
        return getKeyboard(elementParams!!, id)
    }

    private fun getKeyboard(elementParams: ElementParams, id: KeyboardId): Keyboard {
        val ref = sKeyboardCache[id]
        val cachedKeyboard = ref?.get()
        if (cachedKeyboard != null) {
            if (DEBUG_CACHE) Log.d(TAG, "keyboard cache size=${sKeyboardCache.size}: HIT  id=$id")
            return cachedKeyboard
        }

        val builder = KeyboardBuilder(mContext, KeyboardParams(sUniqueKeysCache))
        sUniqueKeysCache.setEnabled(id.isAlphabetKeyboard())
        builder.setAllowRedundantMoreKes(elementParams.mAllowRedundantMoreKeys)
        builder.load(elementParams.mKeyboardXmlId, id)
        val keyboard = builder.build()
        sKeyboardCache[id] = SoftReference(keyboard)
        if (id.mElementId == KeyboardId.ELEMENT_ALPHABET || id.mElementId == KeyboardId.ELEMENT_ALPHABET_AUTOMATIC_SHIFTED) {
            for (i in sForcibleKeyboardCache.size - 1 downTo 1) {
                sForcibleKeyboardCache[i] = sForcibleKeyboardCache[i - 1]
            }
            sForcibleKeyboardCache[0] = keyboard
        }
        return keyboard
    }

    class Builder(private val mContext: Context, ei: EditorInfo?) {
        private val mResources: Resources = mContext.resources
        private val mParams = Params()

        init {
            val editorInfo = ei ?: EMPTY_EDITOR_INFO
            mParams.mMode = getKeyboardMode(editorInfo)
            mParams.mEditorInfo = editorInfo
            val kgMgr = mContext.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            mParams.mNoSettingsKey = kgMgr.isKeyguardLocked
        }

        fun setKeyboardTheme(themeId: Int): Builder { mParams.mKeyboardThemeId = themeId; return this }
        fun setKeyboardGeometry(keyboardWidth: Int, keyboardHeight: Int, keyboardBottomOffset: Int): Builder {
            mParams.mKeyboardWidth = keyboardWidth; mParams.mKeyboardHeight = keyboardHeight; mParams.mKeyboardBottomOffset = keyboardBottomOffset; return this
        }
        fun setSubtype(subtype: Subtype): Builder { mParams.mSubtype = subtype; mParams.mKeyboardLayoutSetName = KEYBOARD_LAYOUT_SET_RESOURCE_PREFIX + subtype.keyboardLayoutSet; return this }
        fun setLanguageSwitchKeyEnabled(enabled: Boolean): Builder { mParams.mLanguageSwitchKeyEnabled = enabled; return this }
        fun setShowSpecialChars(enabled: Boolean): Builder { mParams.mShowMoreKeys = enabled; return this }
        fun setShowNumberRow(enabled: Boolean): Builder { mParams.mShowNumberRow = enabled; return this }

        fun build(): KeyboardLayoutSet {
            if (mParams.mSubtype == null) throw RuntimeException("KeyboardLayoutSet subtype is not specified")
            val xmlId = getXmlId(mResources, mParams.mKeyboardLayoutSetName!!)
            try {
                parseKeyboardLayoutSet(mResources, xmlId)
            } catch (e: Exception) {
                throw RuntimeException(e.message + " in " + mParams.mKeyboardLayoutSetName, e)
            }
            return KeyboardLayoutSet(mContext, mParams)
        }

        private fun parseKeyboardLayoutSet(res: Resources, resId: Int) {
            val parser = res.getXml(resId)
            try {
                while (parser.eventType != android.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                    val event = parser.next()
                    if (event == android.xmlpull.v1.XmlPullParser.START_TAG) {
                        val tag = parser.name
                        if (TAG_KEYBOARD_SET == tag) parseKeyboardLayoutSetContent(parser)
                        else throw XmlParseUtils.IllegalStartTag(parser, tag, TAG_KEYBOARD_SET)
                    }
                }
            } finally { parser.close() }
        }

        private fun parseKeyboardLayoutSetContent(parser: android.xmlpull.v1.XmlPullParser) {
            while (parser.eventType != android.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                val event = parser.next()
                if (event == android.xmlpull.v1.XmlPullParser.START_TAG) {
                    if (TAG_ELEMENT == parser.name) parseKeyboardLayoutSetElement(parser)
                    else throw XmlParseUtils.IllegalStartTag(parser, parser.name, TAG_KEYBOARD_SET)
                } else if (event == android.xmlpull.v1.XmlPullParser.END_TAG) {
                    if (TAG_KEYBOARD_SET == parser.name) break
                    throw XmlParseUtils.IllegalEndTag(parser, parser.name, TAG_KEYBOARD_SET)
                }
            }
        }

        private fun parseKeyboardLayoutSetElement(parser: android.xmlpull.v1.XmlPullParser) {
            val a = mResources.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.KeyboardLayoutSet_Element)
            try {
                XmlParseUtils.checkAttributeExists(a, R.styleable.KeyboardLayoutSet_Element_elementName, "elementName", TAG_ELEMENT, parser)
                XmlParseUtils.checkAttributeExists(a, R.styleable.KeyboardLayoutSet_Element_elementKeyboard, "elementKeyboard", TAG_ELEMENT, parser)
                XmlParseUtils.checkEndTag(TAG_ELEMENT, parser)
                val elementParams = ElementParams()
                elementParams.mKeyboardXmlId = a.getResourceId(R.styleable.KeyboardLayoutSet_Element_elementKeyboard, 0)
                elementParams.mAllowRedundantMoreKeys = a.getBoolean(R.styleable.KeyboardLayoutSet_Element_allowRedundantMoreKeys, true)
                val elementName = a.getInt(R.styleable.KeyboardLayoutSet_Element_elementName, 0)
                mParams.mKeyboardLayoutSetElementIdToParamsMap.put(elementName, elementParams)
            } finally { a.recycle() }
        }
    }

    companion object {
        private const val TAG = "KeyboardLayoutSet"
        private const val DEBUG_CACHE = false
        private const val TAG_KEYBOARD_SET = "KeyboardLayoutSet"
        private const val TAG_ELEMENT = "Element"
        private const val KEYBOARD_LAYOUT_SET_RESOURCE_PREFIX = "keyboard_layout_set_"
        private const val FORCIBLE_CACHE_SIZE = 4
        private val sForcibleKeyboardCache = arrayOfNulls<Keyboard>(FORCIBLE_CACHE_SIZE)
        private val sKeyboardCache = HashMap<KeyboardId, SoftReference<Keyboard>>()
        private val sUniqueKeysCache = UniqueKeysCache.newInstance()
        private val EMPTY_EDITOR_INFO = EditorInfo()

        fun onKeyboardThemeChanged() {
            sKeyboardCache.clear()
            sUniqueKeysCache.clear()
            Arrays.fill(sForcibleKeyboardCache, null)
        }

        private fun getXmlId(resources: Resources, keyboardLayoutSetName: String): Int {
            val packageName = resources.getResourcePackageName(R.xml.keyboard_layout_set_qwerty)
            return resources.getIdentifier(keyboardLayoutSetName, "xml", packageName)
        }

        private fun getKeyboardMode(editorInfo: EditorInfo): Int {
            val inputType = editorInfo.inputType
            val variation = inputType and InputType.TYPE_MASK_VARIATION
            return when (inputType and InputType.TYPE_MASK_CLASS) {
                InputType.TYPE_CLASS_NUMBER -> KeyboardId.MODE_NUMBER
                InputType.TYPE_CLASS_DATETIME -> when (variation) {
                    InputType.TYPE_DATETIME_VARIATION_DATE -> KeyboardId.MODE_DATE
                    InputType.TYPE_DATETIME_VARIATION_TIME -> KeyboardId.MODE_TIME
                    else -> KeyboardId.MODE_DATETIME
                }
                InputType.TYPE_CLASS_PHONE -> KeyboardId.MODE_PHONE
                InputType.TYPE_CLASS_TEXT -> when {
                    InputTypeUtils.isEmailVariation(variation) -> KeyboardId.MODE_EMAIL
                    variation == InputType.TYPE_TEXT_VARIATION_URI -> KeyboardId.MODE_URL
                    variation == InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE -> KeyboardId.MODE_IM
                    else -> KeyboardId.MODE_TEXT
                }
                else -> KeyboardId.MODE_TEXT
            }
        }
    }
}
