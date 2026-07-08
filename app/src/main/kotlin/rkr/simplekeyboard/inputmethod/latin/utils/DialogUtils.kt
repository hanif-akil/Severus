package rkr.simplekeyboard.inputmethod.latin.utils

import android.content.Context
import android.view.ContextThemeWrapper
import rkr.simplekeyboard.inputmethod.R

object DialogUtils {
    fun getPlatformDialogThemeContext(context: Context): Context {
        return ContextThemeWrapper(context, R.style.platformDialogTheme)
    }
}
