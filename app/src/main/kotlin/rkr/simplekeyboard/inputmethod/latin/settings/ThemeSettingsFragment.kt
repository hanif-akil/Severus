package rkr.simplekeyboard.inputmethod.latin.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceScreen
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.keyboard.KeyboardTheme
import rkr.simplekeyboard.inputmethod.latin.settings.RadioButtonPreference.OnRadioButtonClickedListener

class ThemeSettingsFragment : SubScreenFragment(), OnRadioButtonClickedListener {
    private var mSelectedThemeId = 0

    internal class KeyboardThemePreference(context: Context, name: String, val mThemeId: Int) : Preference(context) {
        private var mSelected = false
        init {
            title = name
        }
        fun setSelected(selected: Boolean) {
            mSelected = selected
            summary = if (selected) "Selected" else null
        }
    }

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        addPreferencesFromResource(R.xml.prefs_screen_theme)
        val screen = preferenceScreen
        val context = activity
        val res = resources
        val keyboardThemeNames = res.getStringArray(R.array.keyboard_theme_names)
        val keyboardThemeIds = res.getIntArray(R.array.keyboard_theme_ids)
        for (index in keyboardThemeIds.indices) {
            val pref = KeyboardThemePreference(context, keyboardThemeNames[index], keyboardThemeIds[index])
            screen.addPreference(pref)
            pref.setOnPreferenceClickListener {
                mSelectedThemeId = pref.mThemeId
                updateSelected()
                true
            }
        }
        val keyboardTheme = KeyboardTheme.getKeyboardTheme(context)
        mSelectedThemeId = keyboardTheme.mThemeId
    }

    override fun onRadioButtonClicked(preference: RadioButtonPreference) {
        if (preference is KeyboardThemePreference) {
            mSelectedThemeId = preference.mThemeId
            updateSelected()
        }
    }

    override fun onResume() {
        super.onResume()
        updateSelected()
    }

    override fun onPause() {
        super.onPause()
        KeyboardTheme.saveKeyboardThemeId(mSelectedThemeId, sharedPreferences)
        Settings.removeKeyboardColor(sharedPreferences)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {}

    private fun updateSelected() {
        val screen = preferenceScreen
        val count = screen.preferenceCount
        for (index in 0 until count) {
            val preference = screen.getPreference(index)
            if (preference is KeyboardThemePreference) {
                preference.setSelected(mSelectedThemeId == preference.mThemeId)
            }
        }
    }

    companion object {
        fun updateKeyboardThemeSummary(pref: Preference) {
            val context = pref.context
            val res = context.resources
            val keyboardTheme = KeyboardTheme.getKeyboardTheme(context)
            val keyboardThemeNames = res.getStringArray(R.array.keyboard_theme_names)
            val keyboardThemeIds = res.getIntArray(R.array.keyboard_theme_ids)
            for (index in keyboardThemeIds.indices) {
                if (keyboardTheme.mThemeId == keyboardThemeIds[index]) {
                    pref.summary = keyboardThemeNames[index]
                    return
                }
            }
        }
    }
}
