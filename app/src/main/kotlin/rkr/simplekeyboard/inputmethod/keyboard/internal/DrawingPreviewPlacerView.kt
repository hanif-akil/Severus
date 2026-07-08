package rkr.simplekeyboard.inputmethod.keyboard.internal

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup

class DrawingPreviewPlacerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : ViewGroup(context, attrs, defStyle) {

    private var mKeyboardViewGeometry = IntArray(2)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(0, 0)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // Do not lay out anything.
    }

    override fun dispatchDraw(canvas: Canvas) {
        // Do not draw anything.
    }

    fun setKeyboardViewGeometry(originCoords: IntArray) {
        mKeyboardViewGeometry = originCoords.copyOf()
    }

    fun getKeyboardViewGeometry(): IntArray = mKeyboardViewGeometry

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
