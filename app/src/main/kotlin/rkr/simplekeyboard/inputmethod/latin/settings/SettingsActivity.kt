package rkr.simplekeyboard.inputmethod.latin.settings

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceActivity
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.latin.utils.FragmentUtils

class SettingsActivity : PreferenceActivity() {
    override fun onStart() {
        super.onStart()
        var enabled = false
        try {
            enabled = isInputMethodOfThisImeEnabled()
        } catch (e: Exception) {
            Log.e(TAG, "Exception in check if input method is enabled", e)
        }
        if (!enabled) {
            val context: Context = this
            val builder = AlertDialog.Builder(this)
            builder.setMessage(R.string.setup_message)
            builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
                val intent = Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                dialog.dismiss()
            }
            builder.setNegativeButton(android.R.string.cancel) { dialog, _ -> finish() }
            builder.setCancelable(false)
            builder.create().show()
        }
    }

    private fun isInputMethodOfThisImeEnabled(): Boolean {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val imePackageName = packageName
        for (imi in imm.enabledInputMethodList) {
            if (imi.packageName == imePackageName) return true
        }
        return false
    }

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val container = listView.parent.parent as View
            container.setOnApplyWindowInsetsListener { view, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsets.Type.systemBars())
                val mlp = view.layoutParams as ViewGroup.MarginLayoutParams
                mlp.topMargin = insets.top
                mlp.leftMargin = insets.left
                mlp.bottomMargin = insets.bottom
                mlp.rightMargin = insets.right
                view.layoutParams = mlp
                WindowInsets.CONSUMED
            }
        }
        actionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun getIntent(): Intent {
        val intent = super.getIntent()
        val fragment = intent.getStringExtra(EXTRA_SHOW_FRAGMENT)
        if (fragment == null) intent.putExtra(EXTRA_SHOW_FRAGMENT, DEFAULT_FRAGMENT)
        intent.putExtra(EXTRA_NO_HEADERS, true)
        return intent
    }

    override fun isValidFragment(fragmentName: String): Boolean = FragmentUtils.isValidFragment(fragmentName)

    companion object {
        private val DEFAULT_FRAGMENT = SettingsFragment::class.java.name
        private const val TAG = "SettingsActivity"
    }
}
