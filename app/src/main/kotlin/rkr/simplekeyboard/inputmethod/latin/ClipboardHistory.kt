package rkr.simplekeyboard.inputmethod.latin

import android.content.ClipboardManager
import android.content.Context
import android.util.Log

class ClipboardHistory private constructor(context: Context) : ClipboardManager.OnPrimaryClipChangedListener {
    private val mClipboardManager: ClipboardManager? = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
    private val mItems = ArrayList<String>()
    private var mListener: ClipboardChangeListener? = null

    interface ClipboardChangeListener {
        fun onClipboardChanged()
    }

    init {
        mClipboardManager?.addPrimaryClipChangedListener(this)
        loadCurrentClipboard()
    }

    fun setListener(listener: ClipboardChangeListener?) {
        mListener = listener
    }

    override fun onPrimaryClipChanged() {
        try {
            val clipData = getClipboardData()
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text
                if (text != null && text.isNotEmpty()) {
                    addItem(text.toString())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading clipboard", e)
        }
    }

    private fun loadCurrentClipboard() {
        try {
            val clipData = getClipboardData()
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text
                if (text != null && text.isNotEmpty()) {
                    mItems.add(text.toString())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading clipboard", e)
        }
    }

    private fun getClipboardData(): android.content.ClipData? {
        if (mClipboardManager == null) return null
        return try {
            if (!mClipboardManager.hasPrimaryClip()) null else mClipboardManager.primaryClip
        } catch (e: SecurityException) {
            Log.w(TAG, "Clipboard access denied - app may not have focus", e)
            null
        }
    }

    private fun addItem(text: String) {
        mItems.remove(text)
        mItems.add(0, text)
        while (mItems.size > MAX_ITEMS) {
            mItems.removeAt(mItems.size - 1)
        }
        mListener?.onClipboardChanged()
    }

    fun getItems(): List<String> = ArrayList(mItems)
    fun getItemCount(): Int = mItems.size

    fun removeItem(index: Int) {
        if (index in mItems.indices) {
            mItems.removeAt(index)
            mListener?.onClipboardChanged()
        }
    }

    fun clear() {
        mItems.clear()
        mListener?.onClipboardChanged()
    }

    companion object {
        private const val TAG = "ClipboardHistory"
        private const val MAX_ITEMS = 20
        private var sInstance: ClipboardHistory? = null

        @Synchronized
        fun getInstance(context: Context): ClipboardHistory {
            if (sInstance == null) {
                sInstance = ClipboardHistory(context.applicationContext)
            }
            return sInstance!!
        }
    }
}
