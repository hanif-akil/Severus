package rkr.simplekeyboard.inputmethod.latin.settings

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.keyboard.KeyboardLayoutSet

class PreferencesSettingsFragment : SubScreenFragment() {
    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        addPreferencesFromResource(R.xml.prefs_screen_preferences)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) {
            removePreference(Settings.PREF_USE_ON_SCREEN)
        }
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        if (key == Settings.PREF_SHOW_SPECIAL_CHARS || key == Settings.PREF_SHOW_NUMBER_ROW) {
            KeyboardLayoutSet.onKeyboardThemeChanged()
        }
    }
}
