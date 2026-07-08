package rkr.simplekeyboard.inputmethod.keyboard.internal

import android.content.Context
import android.content.res.Resources
import android.text.TextUtils
import rkr.simplekeyboard.inputmethod.latin.common.Constants
import java.util.Locale

class KeyboardTextsSet {
    private var mResources: Resources? = null
    private var mResourcePackageName: String? = null
    private var mTextsTable: Array<String>? = null

    fun setLocale(locale: Locale, context: Context) {
        val res = context.resources
        val resourcePackageName = res.getResourcePackageName(context.applicationInfo.labelRes)
        setLocale(locale, res, resourcePackageName)
    }

    fun setLocale(locale: Locale, res: Resources, resourcePackageName: String) {
        mResources = res
        mResourcePackageName = resourcePackageName
        mTextsTable = KeyboardTextsTable.getTextsTable(locale)
    }

    fun getText(name: String): String? {
        return KeyboardTextsTable.getText(name, mTextsTable!!)
    }

    fun resolveTextReference(rawText: String?): String? {
        if (TextUtils.isEmpty(rawText)) return null
        var level = 0
        var text = rawText
        do {
            level++
            if (level >= MAX_REFERENCE_INDIRECTION) {
                throw RuntimeException("Too many $PREFIX_TEXT or $PREFIX_RESOURCE reference indirection: $text")
            }
            val prefixLength = PREFIX_TEXT.length
            val size = text!!.length
            if (size < prefixLength) break

            var sb: StringBuilder? = null
            var pos = 0
            while (pos < size) {
                val c = text[pos]
                when {
                    text.startsWith(PREFIX_TEXT, pos) -> {
                        if (sb == null) sb = StringBuilder(text.substring(0, pos))
                        pos = expandReference(text, pos, PREFIX_TEXT, sb)
                    }
                    text.startsWith(PREFIX_RESOURCE, pos) -> {
                        if (sb == null) sb = StringBuilder(text.substring(0, pos))
                        pos = expandReference(text, pos, PREFIX_RESOURCE, sb)
                    }
                    c == BACKSLASH -> {
                        sb?.append(text.substring(pos, minOf(pos + 2, size)))
                        pos++
                    }
                    sb != null -> sb.append(c)
                }
                pos++
            }
            if (sb != null) text = sb.toString()
        } while (true)
        return if (TextUtils.isEmpty(text)) null else text
    }

    private fun expandReference(text: String, pos: Int, prefix: String, sb: StringBuilder): Int {
        val prefixLength = prefix.length
        val end = searchTextNameEnd(text, pos + prefixLength)
        val name = text.substring(pos + prefixLength, end)
        if (prefix == PREFIX_TEXT) {
            sb.append(getText(name))
        } else {
            val resId = mResources!!.getIdentifier(name, "string", mResourcePackageName)
            sb.append(mResources!!.getString(resId))
        }
        return end - 1
    }

    companion object {
        const val PREFIX_TEXT = "!text/"
        private const val PREFIX_RESOURCE = "!string/"
        private val BACKSLASH = Constants.CODE_BACKSLASH.toChar()
        private const val MAX_REFERENCE_INDIRECTION = 10

        private fun searchTextNameEnd(text: String, start: Int): Int {
            val size = text.length
            for (pos in start until size) {
                val c = text[pos]
                if ((c in 'a'..'z') || c == '_' || (c in '0'..'9')) continue
                return pos
            }
            return size
        }
    }
}
