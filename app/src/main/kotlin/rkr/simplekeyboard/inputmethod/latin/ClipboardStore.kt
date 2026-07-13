package rkr.simplekeyboard.inputmethod.latin

import android.content.Context
import rkr.simplekeyboard.inputmethod.compat.PreferenceManagerCompat

class ClipboardStore(context: Context) {

    companion object {
        private const val PREF_CLIPBOARD_ITEMS = "clipboard_items"
        private const val PREF_CLIPBOARD_COUNT = "pref_clipboard_max_items"
        private const val DEFAULT_MAX_ITEMS = 10
    }

    private val mPrefs = PreferenceManagerCompat.getDeviceSharedPreferences(context)
    private val mMaxItems: Int = mPrefs.getInt(PREF_CLIPBOARD_COUNT, DEFAULT_MAX_ITEMS)
    private val mItems: MutableList<String> = loadItems()

    fun getMaxItems(): Int {
        return mMaxItems
    }

    fun getItems(): List<String> {
        return ArrayList(mItems)
    }

    fun getItem(index: Int): String? {
        return if (index in mItems.indices) {
            mItems[index]
        } else {
            null
        }
    }

    fun getItemCount(): Int {
        return mItems.size
    }

    fun addItem(text: String?) {
        if (text == null || text.isEmpty()) {
            return
        }
        mItems.remove(text)
        mItems.add(0, text)
        while (mItems.size > mMaxItems) {
            mItems.removeAt(mItems.size - 1)
        }
        saveItems()
    }

    fun removeItem(index: Int) {
        if (index in mItems.indices) {
            mItems.removeAt(index)
            saveItems()
        }
    }

    fun clearAll() {
        mItems.clear()
        saveItems()
    }

    private fun loadItems(): MutableList<String> {
        val items = mutableListOf<String>()
        val count = mPrefs.getInt(PREF_CLIPBOARD_ITEMS + "_count", 0)
        for (i in 0 until count) {
            val item = mPrefs.getString(PREF_CLIPBOARD_ITEMS + "_" + i, null)
            if (item != null && item.isNotEmpty()) {
                items.add(item)
            }
        }
        return items
    }

    private fun saveItems() {
        val editor = mPrefs.edit()
        editor.putInt(PREF_CLIPBOARD_ITEMS + "_count", mItems.size)
        for (i in mItems.indices) {
            editor.putString(PREF_CLIPBOARD_ITEMS + "_" + i, mItems[i])
        }
        editor.apply()
    }
}
