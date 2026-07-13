/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright (C) 2024 Raimondas Rimkus
 * Copyright (C) 2024 wittmane
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

import android.app.KeyguardManager
import android.content.Context
import android.content.res.TypedArray
import android.content.res.XmlResourceParser
import android.text.InputType
import android.util.Log
import android.util.SparseArray
import android.util.Xml
import android.view.inputmethod.EditorInfo
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
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

class KeyboardLayoutSet private constructor(
    private val mContext: Context,
    private val mParams: Params
) {

    fun getKeyboard(baseKeyboardLayoutSetElementId: Int): Keyboard {
        val keyboardLayoutSetElementId: Int = when (mParams.mMode) {
            KeyboardId.MODE_PHONE -> {
                if (baseKeyboardLayoutSetElementId == KeyboardId.ELEMENT_SYMBOLS) {
                    KeyboardId.ELEMENT_PHONE_SYMBOLS
                } else {
                    KeyboardId.ELEMENT_PHONE
                }
            }
            KeyboardId.MODE_NUMBER,
            KeyboardId.MODE_DATE,
            KeyboardId.MODE_TIME,
            KeyboardId.MODE_DATETIME -> KeyboardId.ELEMENT_NUMBER
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
        sUniqueKeysCache.setEnabled(id.isAlphabetKeyboard)
        builder.setAllowRedundantMoreKes(elementParams.mAllowRedundantMoreKeys)
        val keyboardXmlId = elementParams.mKeyboardXmlId
        builder.load(keyboardXmlId, id)
        val keyboard = builder.build()
        sKeyboardCache[id] = SoftReference(keyboard)
        if (id.mElementId == KeyboardId.ELEMENT_ALPHABET ||
            id.mElementId == KeyboardId.ELEMENT_ALPHABET_AUTOMATIC_SHIFTED
        ) {
            for (i in sForcibleKeyboardCache.size - 1 downTo 1) {
                sForcibleKeyboardCache[i] = sForcibleKeyboardCache[i - 1]
            }
            sForcibleKeyboardCache[0] = keyboard
            if (DEBUG_CACHE) Log.d(TAG, "forcing caching of keyboard with id=$id")
        }
        if (DEBUG_CACHE) {
            Log.d(TAG, "keyboard cache size=${sKeyboardCache.size}: ${if (ref == null) "LOAD" else "GCed"} id=$id")
        }
        return keyboard
    }

    class Builder(private val mContext: Context, ei: EditorInfo?) {
        private val mResources = mContext.resources
        private val mParams = Params()

        init {
            val editorInfo = ei ?: EMPTY_EDITOR_INFO
            mParams.mMode = getKeyboardMode(editorInfo)
            mParams.mEditorInfo = editorInfo
            val kgMgr = mContext.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            mParams.mNoSettingsKey = kgMgr.isKeyguardLocked
        }

        fun setKeyboardTheme(themeId: Int): Builder {
            mParams.mKeyboardThemeId = themeId
            return this
        }

        fun setKeyboardGeometry(keyboardWidth: Int, keyboardHeight: Int, keyboardBottomOffset: Int): Builder {
            mParams.mKeyboardWidth = keyboardWidth
            mParams.mKeyboardHeight = keyboardHeight
            mParams.mKeyboardBottomOffset = keyboardBottomOffset
            return this
        }

        fun setSubtype(subtype: Subtype): Builder {
            mParams.mSubtype = subtype
            mParams.mKeyboardLayoutSetName = KEYBOARD_LAYOUT_SET_RESOURCE_PREFIX +
                    subtype.getKeyboardLayoutSet()
            return this
        }

        fun setLanguageSwitchKeyEnabled(enabled: Boolean): Builder {
            mParams.mLanguageSwitchKeyEnabled = enabled
            return this
        }

        fun setShowSpecialChars(enabled: Boolean): Builder {
            mParams.mShowMoreKeys = enabled
            return this
        }

        fun setShowNumberRow(enabled: Boolean): Builder {
            mParams.mShowNumberRow = enabled
            return this
        }

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

        @Throws(XmlPullParserException::class, IOException::class)
        private fun parseKeyboardLayoutSet(res: android.content.res.Resources, resId: Int) {
            val parser = res.getXml(resId)
            try {
                while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                    val event = parser.next()
                    if (event == XmlPullParser.START_TAG) {
                        val tag = parser.name
                        if (TAG_KEYBOARD_SET == tag) {
                            parseKeyboardLayoutSetContent(parser)
                        } else {
                            throw XmlParseUtils.IllegalStartTag(parser, tag, TAG_KEYBOARD_SET)
                        }
                    }
                }
            } finally {
                parser.close()
            }
        }

        @Throws(XmlPullParserException::class, IOException::class)
        private fun parseKeyboardLayoutSetContent(parser: XmlPullParser) {
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                val event = parser.next()
                if (event == XmlPullParser.START_TAG) {
                    val tag = parser.name
                    if (TAG_ELEMENT == tag) {
                        parseKeyboardLayoutSetElement(parser)
                    } else {
                        throw XmlParseUtils.IllegalStartTag(parser, tag, TAG_KEYBOARD_SET)
                    }
                } else if (event == XmlPullParser.END_TAG) {
                    val tag = parser.name
                    if (TAG_KEYBOARD_SET == tag) break
                    throw XmlParseUtils.IllegalEndTag(parser, tag, TAG_KEYBOARD_SET)
                }
            }
        }

        @Throws(XmlPullParserException::class, IOException::class)
        private fun parseKeyboardLayoutSetElement(parser: XmlPullParser) {
            val a = mResources.obtainAttributes(
                Xml.asAttributeSet(parser),
                R.styleable.KeyboardLayoutSet_Element
            )
            try {
                XmlParseUtils.checkAttributeExists(
                    a, R.styleable.KeyboardLayoutSet_Element_elementName, "elementName",
                    TAG_ELEMENT, parser
                )
                XmlParseUtils.checkAttributeExists(
                    a, R.styleable.KeyboardLayoutSet_Element_elementKeyboard, "elementKeyboard",
                    TAG_ELEMENT, parser
                )
                XmlParseUtils.checkEndTag(TAG_ELEMENT, parser)

                val elementParams = ElementParams()
                val elementName = a.getInt(R.styleable.KeyboardLayoutSet_Element_elementName, 0)
                elementParams.mKeyboardXmlId = a.getResourceId(
                    R.styleable.KeyboardLayoutSet_Element_elementKeyboard, 0
                )
                elementParams.mAllowRedundantMoreKeys = a.getBoolean(
                    R.styleable.KeyboardLayoutSet_Element_allowRedundantMoreKeys, true
                )
                mParams.mKeyboardLayoutSetElementIdToParamsMap.put(elementName, elementParams)
            } finally {
                a.recycle()
            }
        }

        companion object {
            private val EMPTY_EDITOR_INFO = EditorInfo()

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

    class Params {
        var mKeyboardLayoutSetName: String? = null
        var mMode: Int = 0
        var mEditorInfo: EditorInfo = EditorInfo()
        var mNoSettingsKey: Boolean = false
        var mLanguageSwitchKeyEnabled: Boolean = false
        var mSubtype: Subtype? = null
        var mKeyboardThemeId: Int = 0
        var mKeyboardWidth: Int = 0
        var mKeyboardHeight: Int = 0
        var mKeyboardBottomOffset: Int = 0
        var mShowMoreKeys: Boolean = false
        var mShowNumberRow: Boolean = false
        val mKeyboardLayoutSetElementIdToParamsMap = SparseArray<ElementParams>()
    }

    class KeyboardLayoutSetException(cause: Throwable, val mKeyboardId: KeyboardId) : RuntimeException(cause)

    private class ElementParams {
        var mKeyboardXmlId: Int = 0
        var mAllowRedundantMoreKeys: Boolean = true
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

        @JvmStatic
        fun onKeyboardThemeChanged() {
            clearKeyboardCache()
        }

        private fun clearKeyboardCache() {
            sKeyboardCache.clear()
            sUniqueKeysCache.clear()
            Arrays.fill(sForcibleKeyboardCache, null)
        }

        private fun getXmlId(resources: android.content.res.Resources, keyboardLayoutSetName: String): Int {
            val packageName = resources.getResourcePackageName(R.xml.keyboard_layout_set_qwerty)
            return resources.getIdentifier(keyboardLayoutSetName, "xml", packageName)
        }
    }
}
