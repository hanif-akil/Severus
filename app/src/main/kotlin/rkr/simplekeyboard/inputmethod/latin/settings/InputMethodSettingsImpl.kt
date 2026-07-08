package rkr.simplekeyboard.inputmethod.latin.settings

import android.content.Context
import android.preference.Preference
import android.preference.PreferenceScreen
import android.text.TextUtils
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.compat.PreferenceManagerCompat
import rkr.simplekeyboard.inputmethod.latin.RichInputMethodManager

internal class InputMethodSettingsImpl {
    private var mSubtypeEnablerPreference: Preference? = null
    private var mRichImm: RichInputMethodManager? = null

    fun init(context: Context, prefScreen: PreferenceScreen): Boolean {
        RichInputMethodManager.init(context)
        mRichImm = RichInputMethodManager.getInstance()
        val prefs = PreferenceManagerCompat.getDeviceSharedPreferences(context)
        val restrictionsMgr = context.getSystemService(Context.RESTRICTIONS_SERVICE) as android.content.RestrictionsManager
        val restrictionKeys = Settings.loadRestrictions(restrictionsMgr, prefs)
        mSubtypeEnablerPreference = Preference(context).apply {
            setTitle(R.string.select_language)
            fragment = LanguagesSettingsFragment::class.java.name
            isEnabled = !restrictionKeys.contains(Settings.PREF_ENABLED_SUBTYPES)
        }
        prefScreen.addPreference(mSubtypeEnablerPreference)
        updateEnabledSubtypeList()
        return true
    }

    private fun getEnabledSubtypesLabel(richImm: RichInputMethodManager?): String? {
        if (richImm == null) return null
        val subtypes = richImm.getEnabledSubtypes(true)
        val sb = StringBuilder()
        for (subtype in subtypes) {
            if (sb.isNotEmpty()) sb.append(", ")
            sb.append(subtype.name)
        }
        return sb.toString()
    }

    fun updateEnabledSubtypeList() {
        if (mSubtypeEnablerPreference != null) {
            val summary = getEnabledSubtypesLabel(mRichImm)
            if (!TextUtils.isEmpty(summary)) {
                mSubtypeEnablerPreference!!.summary = summary
            }
        }
    }
}
