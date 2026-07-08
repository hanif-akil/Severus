package rkr.simplekeyboard.inputmethod.keyboard.internal

import android.content.res.TypedArray

abstract class KeyStyle(protected val mTextsSet: KeyboardTextsSet) {
    abstract fun getStringArray(a: TypedArray, index: Int): Array<String>?
    abstract fun getString(a: TypedArray, index: Int): String?
    abstract fun getInt(a: TypedArray, index: Int, defaultValue: Int): Int
    abstract fun getFlags(a: TypedArray, index: Int): Int

    protected fun parseString(a: TypedArray, index: Int): String? {
        if (a.hasValue(index)) return mTextsSet.resolveTextReference(a.getString(index))
        return null
    }

    protected fun parseStringArray(a: TypedArray, index: Int): Array<String>? {
        if (a.hasValue(index)) {
            val text = mTextsSet.resolveTextReference(a.getString(index))
            return MoreKeySpec.splitKeySpecs(text)
        }
        return null
    }
}
