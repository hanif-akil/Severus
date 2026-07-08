package rkr.simplekeyboard.inputmethod.compat

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

object PreferenceManagerCompat {
    fun getDeviceContext(context: Context): Context = context.createDeviceProtectedStorageContext()
    fun getDeviceSharedPreferences(context: Context): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(getDeviceContext(context))
}
