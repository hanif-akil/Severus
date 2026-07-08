package rkr.simplekeyboard.inputmethod.compat

import android.view.inputmethod.EditorInfo
import java.util.Locale

object EditorInfoCompatUtils {
    fun imeActionName(imeOptions: Int): String {
        val actionId = imeOptions and EditorInfo.IME_MASK_ACTION
        return when (actionId) {
            EditorInfo.IME_ACTION_UNSPECIFIED -> "actionUnspecified"
            EditorInfo.IME_ACTION_NONE -> "actionNone"
            EditorInfo.IME_ACTION_GO -> "actionGo"
            EditorInfo.IME_ACTION_SEARCH -> "actionSearch"
            EditorInfo.IME_ACTION_SEND -> "actionSend"
            EditorInfo.IME_ACTION_NEXT -> "actionNext"
            EditorInfo.IME_ACTION_DONE -> "actionDone"
            EditorInfo.IME_ACTION_PREVIOUS -> "actionPrevious"
            else -> "actionUnknown($actionId)"
        }
    }

    fun getPrimaryHintLocale(editorInfo: EditorInfo?): Locale? {
        if (editorInfo == null) return null
        val localeList = editorInfo.hintLocales
        return if (localeList != null && !localeList.isEmpty) localeList[0] else null
    }
}
