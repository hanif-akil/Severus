package rkr.simplekeyboard.inputmethod.keyboard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.latin.ClipboardHistory

class ClipboardHistoryView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), ClipboardHistory.ClipboardChangeListener {

    private var mListener: KeyboardActionListener? = null
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private var mListView: LinearLayout? = null
    private var mScrollView: ScrollView? = null
    private var mEmptyView: TextView? = null
    private var mIsShowing = false

    override fun onFinishInflate() {
        super.onFinishInflate()
        mListView = findViewById(R.id.clipboard_list)
        mScrollView = findViewById(R.id.clipboard_scroll)
        mEmptyView = findViewById(R.id.clipboard_empty)
    }

    fun setListener(listener: KeyboardActionListener?) {
        mListener = listener
    }

    fun show(parent: ViewGroup?) {
        if (mIsShowing || parent == null) return
        try {
            val history = ClipboardHistory.getInstance(context)
            history.setListener(this)
            refreshItems(history)
            val currentParent = parent as? ViewGroup
            currentParent?.removeView(this)
            parent.addView(this)
            mIsShowing = true
        } catch (e: Exception) {
            mIsShowing = false
        }
    }

    fun dismiss() {
        if (!mIsShowing) return
        val parent = parent as? ViewGroup
        parent?.removeView(this)
        try {
            val history = ClipboardHistory.getInstance(context)
            history.setListener(null)
        } catch (e: Exception) { }
        mIsShowing = false
    }

    fun isShowing(): Boolean = mIsShowing

    override fun onClipboardChanged() {
        try {
            val history = ClipboardHistory.getInstance(context)
            refreshItems(history)
        } catch (e: Exception) { }
    }

    private fun refreshItems(history: ClipboardHistory) {
        if (mListView == null) return
        val items = history.getItems()
        mListView!!.removeAllViews()
        if (items.isEmpty()) {
            mEmptyView?.visibility = View.VISIBLE
            mScrollView?.visibility = View.GONE
            return
        }
        mEmptyView?.visibility = View.GONE
        mScrollView?.visibility = View.VISIBLE
        for (i in items.indices) {
            val text = items[i]
            val itemView = mInflater.inflate(R.layout.clipboard_history_item, mListView, false)
            val textView = itemView.findViewById<TextView>(R.id.clipboard_item_text)
            val deleteButton = itemView.findViewById<ImageButton>(R.id.clipboard_item_delete)
            textView?.text = text
            itemView.setOnClickListener {
                mListener?.onTextInput(text)
                dismiss()
            }
            deleteButton?.setOnClickListener { history.removeItem(i) }
            mListView!!.addView(itemView)
        }
    }
}
