/*
 * Copyright (C) 2014 The Android Open Source Project
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
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import rkr.simplekeyboard.inputmethod.keyboard.Key

class KeyPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TextView(context, attrs, defStyleAttr) {
    private val mBackgroundPadding = Rect()

    init {
        setGravity(Gravity.CENTER)
    }

    fun setPreviewVisual(key: Key, iconsSet: KeyboardIconsSet, drawParams: KeyDrawParams, backgroundColor: Int) {
        val iconId = key.iconId
        if (iconId != KeyboardIconsSet.ICON_UNDEFINED) {
            setCompoundDrawables(null, null, null, key.getPreviewIcon(iconsSet))
            setText(null)
            return
        }

        setCompoundDrawables(null, null, null, null)
        setTextColor(drawParams.mPreviewTextColor)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, key.selectPreviewTextSize(drawParams).toFloat())
        setTypeface(key.selectPreviewTypeface(drawParams))
        setTextAndScaleX(key.previewLabel)
        setColor(backgroundColor)
    }

    private fun setTextAndScaleX(text: String?) {
        setTextScaleX(1.0f)
        setText(text)
        if (text != null && sNoScaleXTextSet.contains(text)) return

        val background = background ?: return
        background.getPadding(mBackgroundPadding)
        val maxWidth = background.intrinsicWidth - mBackgroundPadding.left - mBackgroundPadding.right
        val width = getTextWidth(text, paint)
        if (width <= maxWidth) {
            if (text != null) sNoScaleXTextSet.add(text)
            return
        }
        setTextScaleX(maxWidth / width)
    }

    private fun setColor(backgroundColor: Int) {
        val background = background ?: return
        if (Color.alpha(backgroundColor) > 0) {
            background.setColorFilter(backgroundColor, PorterDuff.Mode.OVERLAY)
        }
    }

    companion object {
        private val sNoScaleXTextSet = HashSet<String>()

        fun clearTextCache() {
            sNoScaleXTextSet.clear()
        }

        private fun getTextWidth(text: String?, paint: TextPaint): Float {
            if (TextUtils.isEmpty(text)) return 0.0f
            val len = text!!.length
            val widths = FloatArray(len)
            val count = paint.getTextWidths(text, 0, len, widths)
            var width = 0f
            for (i in 0 until count) {
                width += widths[i]
            }
            return width
        }
    }
}
