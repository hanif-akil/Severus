package rkr.simplekeyboard.inputmethod.compat

import android.app.ActionBar
import android.graphics.PorterDuff
import android.view.MenuItem
import android.view.View
import android.widget.TextView

object MenuItemIconColorCompat {
    fun matchMenuIconColor(view: View?, menuItem: MenuItem?, actionBar: ActionBar?) {
        if (view == null || menuItem == null || actionBar == null) return
        val views = ArrayList<View>()
        view.rootView.findViewsWithText(views, actionBar.title, View.FIND_VIEWS_WITH_TEXT)
        if (views.size == 1 && views[0] is TextView) {
            val color = (views[0] as TextView).currentTextColor
            setIconColor(menuItem, color)
        }
    }

    private fun setIconColor(menuItem: MenuItem, color: Int) {
        val drawable = menuItem.icon ?: return
        drawable.mutate()
        drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
    }
}
