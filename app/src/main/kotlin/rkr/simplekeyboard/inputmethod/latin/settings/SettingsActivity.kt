/*
 * Copyright (C) 2012 The Android Open Source Project
 * Copyright (C) 2025 Raimondas Rimkus
 * Copyright (C) 2021 wittmane
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
            AlertDialog.Builder(this)
                .setMessage(R.string.setup_message)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    val intent = Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    finish()
                }
                .setCancelable(false)
                .create()
                .show()
        }
    }

    private fun isInputMethodOfThisImeEnabled(): Boolean {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val imePackageName = packageName
        for (imi in imm.enabledInputMethodList) {
            if (imi.packageName == imePackageName) {
                return true
            }
        }
        return false
    }

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            val list: View? = listView
            val parent: View? = list?.parent as? View
            val container: View? = parent?.parent as? View
            container?.setOnApplyWindowInsetsListener { view, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsets.Type.systemBars())
                val lp = view.layoutParams
                if (lp is ViewGroup.MarginLayoutParams) {
                    lp.topMargin = insets.top
                    lp.leftMargin = insets.left
                    lp.bottomMargin = insets.bottom
                    lp.rightMargin = insets.right
                    view.layoutParams = lp
                }
                WindowInsets.CONSUMED
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }

        actionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
            elevation = 0f
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            super.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun getIntent(): Intent {
        val intent = super.getIntent()
        val fragment = intent.getStringExtra(EXTRA_SHOW_FRAGMENT)
        if (fragment == null) {
            intent.putExtra(EXTRA_SHOW_FRAGMENT, DEFAULT_FRAGMENT)
        }
        intent.putExtra(EXTRA_NO_HEADERS, true)
        return intent
    }

    override fun isValidFragment(fragmentName: String): Boolean {
        return FragmentUtils.isValidFragment(fragmentName)
    }

    companion object {
        private val DEFAULT_FRAGMENT = SettingsFragment::class.java.name
        private const val TAG = "SettingsActivity"
    }
}
