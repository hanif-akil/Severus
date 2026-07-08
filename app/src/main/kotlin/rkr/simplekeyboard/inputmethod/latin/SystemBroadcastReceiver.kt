package rkr.simplekeyboard.inputmethod.latin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import rkr.simplekeyboard.inputmethod.latin.utils.LocaleResourceUtils

class SystemBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_LOCALE_CHANGED == intent.action) {
            Log.i(TAG, "System locale changed")
            RichInputMethodManager.getInstance().reloadSubtypes(context)
            LocaleResourceUtils.onLocalChange(context)
        }
    }

    companion object {
        private const val TAG = "SystemBroadcastReceiver"
    }
}
