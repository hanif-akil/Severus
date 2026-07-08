package rkr.simplekeyboard.inputmethod.latin.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.latin.utils.ApplicationUtils

class SettingsFragment : InputMethodSettingsFragment() {
    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setHasOptionsMenu(true)
        addPreferencesFromResource(R.xml.prefs)
        preferenceScreen.title = ApplicationUtils.getActivityTitleResId(activity, SettingsActivity::class.java)
        val res = resources
        findPreference("privacy_policy").onPreferenceClickListener = {
            openUrl(res.getString(R.string.privacy_policy_url))
            true
        }
        findPreference("license").onPreferenceClickListener = {
            openUrl(res.getString(R.string.license_url))
            true
        }
    }

    private fun openUrl(uri: String) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            startActivity(browserIntent)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Browser not found")
        }
    }

    companion object {
        private const val TAG = "SettingsFragment"
    }
}
