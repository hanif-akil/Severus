/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright (C) 2018 Raimondas Rimkus
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

package rkr.simplekeyboard.inputmethod.latin

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import rkr.simplekeyboard.inputmethod.R

class InputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    interface OnToolbarActionListener {
        fun onClipboardClicked()
        fun onSettingsClicked()
        fun onNumPadClicked()
    }

    interface OnClipboardItemClickListener {
        fun onClipboardItemClick(text: String)
    }

    private var mToolbarContainer: LinearLayout? = null
    private var mClipboardButton: ImageView? = null
    private var mSettingsButton: ImageView? = null
    private var mNumPadButton: ImageView? = null
    private var mClipboardPopupContainer: FrameLayout? = null
    private var mToolbarVisible = true
    private var mListener: OnToolbarActionListener? = null

    override fun onFinishInflate() {
        super.onFinishInflate()
        mToolbarContainer = findViewById(R.id.toolbar_container)
        mClipboardButton = findViewById(R.id.toolbar_clipboard)
        mSettingsButton = findViewById(R.id.toolbar_settings)
        mNumPadButton = findViewById(R.id.toolbar_numpad)
        mClipboardPopupContainer = findViewById(R.id.clipboard_popup_container)

        mClipboardButton?.setOnClickListener {
            mListener?.onClipboardClicked()
        }

        mSettingsButton?.setOnClickListener {
            mListener?.onSettingsClicked()
        }

        mNumPadButton?.setOnClickListener {
            mListener?.onNumPadClicked()
        }
    }

    fun setOnToolbarActionListener(listener: OnToolbarActionListener?) {
        mListener = listener
    }

    fun setToolbarVisible(visible: Boolean) {
        mToolbarVisible = visible
        mToolbarContainer?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun isToolbarVisible(): Boolean {
        return mToolbarVisible
    }

    fun showClipboardPopup(items: List<String>?, listener: OnClipboardItemClickListener?) {
        val popupContainer = mClipboardPopupContainer ?: return
        popupContainer.removeAllViews()

        if (items.isNullOrEmpty()) {
            val emptyView = TextView(context).apply {
                text = "Clipboard is empty"
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(16, 12, 16, 12)
                textSize = 14f
            }
            popupContainer.addView(emptyView)
        } else {
            val listLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(0xFF222222.toInt())
                setPadding(0, 4, 0, 4)
            }

            val maxDisplay = minOf(items.size, 5)
            for (i in 0 until maxDisplay) {
                val item = items[i]
                val itemView = TextView(context).apply {
                    val displayText = if (item.length > 50) item.substring(0, 50) + "..." else item
                    text = displayText.replace("\n", " ")
                    setTextColor(0xFFFFFFFF.toInt())
                    textSize = 14f
                    setPadding(16, 10, 16, 10)
                    setBackgroundColor(0xFF333333.toInt())
                    setOnClickListener {
                        listener?.onClipboardItemClick(item)
                        hideClipboardPopup()
                    }
                }
                listLayout.addView(itemView)
            }

            popupContainer.addView(listLayout)
        }

        popupContainer.visibility = View.VISIBLE
    }

    fun hideClipboardPopup() {
        mClipboardPopupContainer?.let { container ->
            container.visibility = View.GONE
            container.removeAllViews()
        }
    }
}
