package rkr.simplekeyboard.inputmethod.keyboard.internal

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup

class DrawingPreviewPlacerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    override fun dispatchDraw(canvas: Canvas) {
        // Do not draw anything.
    }

    fun placeViewAt(view: View, x: Int, y: Int, width: Int, height: Int) {
        val lp = view.layoutParams
        if (lp is ViewGroup.MarginLayoutParams) {
            lp.width = width
            lp.height = height
            lp.setMargins(x, y, 0, 0)
            view.layoutParams = lp
        }
    }
}
