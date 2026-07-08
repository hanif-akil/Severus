package rkr.simplekeyboard.inputmethod.keyboard.internal

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Rect
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import rkr.simplekeyboard.inputmethod.keyboard.Key

class KeyPreviewView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : TextView(context, attrs, defStyleAttr) {

    private val mBackgroundPadding = Rect()

    init {
        gravity = Gravity.CENTER
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
        typeface = key.selectPreviewTypeface(drawParams)
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
            text?.let { sNoScaleXTextSet.add(it) }
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

        private fun getTextWidth(text: String?, paint: android.text.TextPaint): Float {
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
