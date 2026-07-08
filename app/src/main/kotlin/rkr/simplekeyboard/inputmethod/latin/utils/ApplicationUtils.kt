package rkr.simplekeyboard.inputmethod.latin.utils

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import android.util.Log

object ApplicationUtils {
    private const val TAG = "ApplicationUtils"

    fun getActivityTitleResId(context: Context, cls: Class<out Activity>): Int {
        val cn = ComponentName(context, cls)
        try {
            val ai = context.packageManager.getActivityInfo(cn, 0)
            if (ai != null) return ai.labelRes
        } catch (e: NameNotFoundException) {
            Log.e(TAG, "Failed to get settings activity title res id.", e)
        }
        return 0
    }

    fun getVersionName(context: Context?): String {
        try {
            if (context == null) return ""
            val packageName = context.packageName
            val info = context.packageManager.getPackageInfo(packageName, 0)
            @Suppress("DEPRECATION")
            return info.versionName
        } catch (e: NameNotFoundException) {
            Log.e(TAG, "Could not find version info.", e)
        }
        return ""
    }

    fun getVersionCode(context: Context?): Int {
        try {
            if (context == null) return 0
            val packageName = context.packageName
            val info = context.packageManager.getPackageInfo(packageName, 0)
            @Suppress("DEPRECATION")
            return info.versionCode
        } catch (e: NameNotFoundException) {
            Log.e(TAG, "Could not find version info.", e)
        }
        return 0
    }
}
