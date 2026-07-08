package rkr.simplekeyboard.inputmethod.latin.settings

import android.app.backup.BackupManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceFragment
import android.preference.PreferenceGroup
import android.preference.PreferenceScreen
import android.util.Log
import rkr.simplekeyboard.inputmethod.compat.PreferenceManagerCompat

abstract class SubScreenFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {
    private var mSharedPreferenceChangeListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    override fun addPreferencesFromResource(preferencesResId: Int) {
        super.addPreferencesFromResource(preferencesResId)
        val restrictionKeys = sharedPreferences.getStringSet(Settings.ACTIVE_RESTRICTIONS, null)
        if (restrictionKeys != null && restrictionKeys.isNotEmpty()) {
            val group = preferenceScreen
            val count = group.preferenceCount
            for (index in 0 until count) {
                val preference = group.getPreference(index)
                if (restrictionKeys.contains(preference.key)) {
                    preference.isEnabled = false
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceManager.setStorageDeviceProtected()
        mSharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            val context = activity ?: return@OnSharedPreferenceChangeListener
            if (preferenceScreen == null) {
                Log.w(javaClass.simpleName, "onSharedPreferenceChanged called before activity starts.")
                return@OnSharedPreferenceChangeListener
            }
            BackupManager(context).dataChanged()
            onSharedPreferenceChanged(prefs, key)
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener)
    }

    override fun onDestroy() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener)
        super.onDestroy()
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {}

    companion object {
        fun setPreferenceEnabled(prefKey: String, enabled: Boolean, screen: PreferenceScreen) {
            val preference = screen.findPreference(prefKey) ?: return
            preference.isEnabled = enabled
        }

        fun removePreference(prefKey: String, screen: PreferenceScreen) {
            val preference = screen.findPreference(prefKey) ?: return
            screen.removePreference(preference)
        }
    }

    fun setPreferenceEnabled(prefKey: String, enabled: Boolean) {
        setPreferenceEnabled(prefKey, enabled, preferenceScreen)
    }

    fun removePreference(prefKey: String) {
        removePreference(prefKey, preferenceScreen)
    }

    val sharedPreferences: SharedPreferences
        get() = PreferenceManagerCompat.getDeviceSharedPreferences(activity)
}
