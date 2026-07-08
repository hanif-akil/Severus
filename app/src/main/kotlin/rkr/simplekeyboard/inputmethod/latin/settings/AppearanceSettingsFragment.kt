package rkr.simplekeyboard.inputmethod.latin.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.Preference
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.keyboard.KeyboardTheme

class AppearanceSettingsFragment : SubScreenFragment() {
    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        addPreferencesFromResource(R.xml.prefs_screen_appearance)
        setupKeyboardHeightSettings()
        setupBottomOffsetPortraitSettings()
        setupKeyboardColorSettings()
    }

    override fun onResume() {
        super.onResume()
        ThemeSettingsFragment.updateKeyboardThemeSummary(findPreference(Settings.SCREEN_THEME))
        val colorPreference = findPreference(Settings.PREF_KEYBOARD_COLOR)
        if (colorPreference.isEnabled) {
            val prefs = sharedPreferences
            val theme = KeyboardTheme.getKeyboardTheme(prefs)
            colorPreference.isEnabled = theme.mCustomColorSupport
        }
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        if (KeyboardTheme.KEYBOARD_THEME_KEY == key) {
            ThemeSettingsFragment.updateKeyboardThemeSummary(findPreference(Settings.SCREEN_THEME))
            val theme = KeyboardTheme.getKeyboardTheme(prefs)
            setPreferenceEnabled(Settings.PREF_KEYBOARD_COLOR, theme.mCustomColorSupport)
        }
    }

    private fun setupKeyboardHeightSettings() {
        val pref = findPreference(Settings.PREF_KEYBOARD_HEIGHT) as? SeekBarDialogPreference ?: return
        val prefs = sharedPreferences
        val res = resources
        pref.setInterface(object : SeekBarDialogPreference.ValueProxy {
            private val PERCENTAGE_FLOAT = 100.0f
            private fun getValueFromPercentage(percentage: Int): Float = percentage / PERCENTAGE_FLOAT
            private fun getPercentageFromValue(floatValue: Float): Int = Math.round(floatValue * PERCENTAGE_FLOAT)
            override fun writeValue(value: Int, key: String) { prefs.edit().putFloat(key, getValueFromPercentage(value)).apply() }
            override fun writeDefaultValue(key: String) { prefs.edit().remove(key).apply() }
            override fun readValue(key: String): Int = getPercentageFromValue(Settings.readKeyboardHeight(prefs, 1f))
            override fun readDefaultValue(key: String): Int = getPercentageFromValue(1f)
            override fun getValueText(value: Int): String = if (value < 0) res.getString(R.string.settings_system_default) else res.getString(R.string.abbreviation_unit_percent, value)
            override fun feedbackValue(value: Int) {}
        })
    }

    private fun setupBottomOffsetPortraitSettings() {
        val pref = findPreference(Settings.PREF_BOTTOM_OFFSET_PORTRAIT) as? SeekBarDialogPreference ?: return
        val prefs = sharedPreferences
        val res = resources
        pref.setInterface(object : SeekBarDialogPreference.ValueProxy {
            override fun writeValue(value: Int, key: String) { prefs.edit().putInt(key, value).apply() }
            override fun writeDefaultValue(key: String) { prefs.edit().remove(key).apply() }
            override fun readValue(key: String): Int = Settings.readBottomOffsetPortrait(prefs)
            override fun readDefaultValue(key: String): Int = Settings.DEFAULT_BOTTOM_OFFSET
            override fun getValueText(value: Int): String = if (value < 0) res.getString(R.string.settings_system_default) else res.getString(R.string.abbreviation_unit_dp, value)
            override fun feedbackValue(value: Int) {}
        })
    }

    private fun setupKeyboardColorSettings() {
        val pref = findPreference(Settings.PREF_KEYBOARD_COLOR) as? ColorDialogPreference ?: return
        val prefs = sharedPreferences
        val context = activity.applicationContext
        pref.setInterface(object : ColorDialogPreference.ValueProxy {
            override fun writeValue(value: Int, key: String) { prefs.edit().putInt(key, value).apply() }
            override fun readValue(key: String): Int = Settings.readKeyboardColor(prefs, context)
            override fun writeDefaultValue(key: String) { Settings.removeKeyboardColor(prefs) }
        })
    }
}
