package rkr.simplekeyboard.inputmethod.latin.utils

import rkr.simplekeyboard.inputmethod.latin.settings.AppearanceSettingsFragment
import rkr.simplekeyboard.inputmethod.latin.settings.KeyPressSettingsFragment
import rkr.simplekeyboard.inputmethod.latin.settings.LanguagesSettingsFragment
import rkr.simplekeyboard.inputmethod.latin.settings.PreferencesSettingsFragment
import rkr.simplekeyboard.inputmethod.latin.settings.SettingsFragment
import rkr.simplekeyboard.inputmethod.latin.settings.SingleLanguageSettingsFragment
import rkr.simplekeyboard.inputmethod.latin.settings.ThemeSettingsFragment

object FragmentUtils {
    private val sLatinImeFragments = HashSet<String>().apply {
        add(PreferencesSettingsFragment::class.java.name)
        add(KeyPressSettingsFragment::class.java.name)
        add(AppearanceSettingsFragment::class.java.name)
        add(ThemeSettingsFragment::class.java.name)
        add(SettingsFragment::class.java.name)
        add(LanguagesSettingsFragment::class.java.name)
        add(SingleLanguageSettingsFragment::class.java.name)
    }

    fun isValidFragment(fragmentName: String): Boolean {
        return sLatinImeFragments.contains(fragmentName)
    }
}
