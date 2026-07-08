package rkr.simplekeyboard.inputmethod.latin.settings

import android.os.Bundle
import android.preference.PreferenceFragment
import rkr.simplekeyboard.inputmethod.compat.PreferenceManagerCompat

abstract class InputMethodSettingsFragment : PreferenceFragment() {
    private val mSettings = InputMethodSettingsImpl()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = activity
        preferenceScreen = preferenceManager.createPreferenceScreen(
            PreferenceManagerCompat.getDeviceContext(context)
        )
        mSettings.init(context, preferenceScreen)
    }

    override fun onResume() {
        super.onResume()
        mSettings.updateEnabledSubtypeList()
    }
}
