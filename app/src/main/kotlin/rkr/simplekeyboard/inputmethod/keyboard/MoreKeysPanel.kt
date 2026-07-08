package rkr.simplekeyboard.inputmethod.keyboard

import android.view.View
import android.view.ViewGroup

interface MoreKeysPanel {
    interface Controller {
        fun onShowMoreKeysPanel(panel: MoreKeysPanel)
        fun onDismissMoreKeysPanel()
        fun onCancelMoreKeysPanel()
    }

    fun showMoreKeysPanel(parentView: View, controller: Controller, pointX: Int, pointY: Int, listener: KeyboardActionListener)
    fun dismissMoreKeysPanel()
    fun onMoveEvent(x: Int, y: Int, pointerId: Int)
    fun onDownEvent(x: Int, y: Int, pointerId: Int)
    fun onUpEvent(x: Int, y: Int, pointerId: Int)
    fun translateX(x: Int): Int
    fun translateY(y: Int): Int
    fun showInParent(parentView: ViewGroup)
    fun removeFromParent()
    fun isShowingInParent(): Boolean

    companion object {
        val EMPTY_CONTROLLER: Controller = object : Controller {
            override fun onShowMoreKeysPanel(panel: MoreKeysPanel) {}
            override fun onDismissMoreKeysPanel() {}
            override fun onCancelMoreKeysPanel() {}
        }
    }
}
