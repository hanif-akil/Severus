/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (C) 2025 Raimondas Rimkus
 * Copyright (C) 2024 wittmane
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

package rkr.simplekeyboard.inputmethod.keyboard

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.event.Event
import rkr.simplekeyboard.inputmethod.keyboard.KeyboardLayoutSet.KeyboardLayoutSetException
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeyboardState
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeyboardTextsSet
import rkr.simplekeyboard.inputmethod.latin.InputView
import rkr.simplekeyboard.inputmethod.latin.LatinIME
import rkr.simplekeyboard.inputmethod.latin.RichInputMethodManager
import rkr.simplekeyboard.inputmethod.latin.settings.Settings
import rkr.simplekeyboard.inputmethod.latin.settings.SettingsValues
import rkr.simplekeyboard.inputmethod.latin.utils.CapsModeUtils
import rkr.simplekeyboard.inputmethod.latin.utils.LanguageOnSpacebarUtils
import rkr.simplekeyboard.inputmethod.latin.utils.RecapitalizeStatus
import rkr.simplekeyboard.inputmethod.latin.utils.ResourceUtils

class KeyboardSwitcher private constructor() : KeyboardState.SwitchActions {

    private var mKeyboardView: MainKeyboardView? = null
    private var mLatinIME: LatinIME? = null
    private var mRichImm: RichInputMethodManager? = null
    private lateinit var mState: KeyboardState
    private var mKeyboardLayoutSet: KeyboardLayoutSet? = null
    private val mKeyboardTextsSet = KeyboardTextsSet()
    private var mKeyboardTheme: KeyboardTheme? = null
    private var mThemeContext: Context? = null

    private fun initInternal(latinIme: LatinIME) {
        mLatinIME = latinIme
        mRichImm = RichInputMethodManager.getInstance()
        mState = KeyboardState(this)
    }

    fun updateKeyboardTheme() {
        val themeUpdated = updateKeyboardThemeAndContextThemeWrapper(
            mLatinIME!!, KeyboardTheme.getKeyboardTheme(mLatinIME!!)
        )
        if (themeUpdated && mKeyboardView != null) {
            mLatinIME!!.setInputView(onCreateInputView())
        }
    }

    fun onConfigurationChanged() {
        mKeyboardTheme = KeyboardTheme.getKeyboardTheme(mLatinIME!!)
        mThemeContext = ContextThemeWrapper(mLatinIME, mKeyboardTheme!!.mStyleId)
        KeyboardLayoutSet.onKeyboardThemeChanged()
        if (mKeyboardView != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            mLatinIME!!.setInputView(onCreateInputView())
        }
    }

    private fun updateKeyboardThemeAndContextThemeWrapper(context: Context, keyboardTheme: KeyboardTheme?): Boolean {
        if (mThemeContext == null || keyboardTheme != mKeyboardTheme) {
            mKeyboardTheme = keyboardTheme
            mThemeContext = ContextThemeWrapper(context, keyboardTheme!!.mStyleId)
            KeyboardLayoutSet.onKeyboardThemeChanged()
            return true
        }
        return false
    }

    fun loadKeyboard(
        editorInfo: EditorInfo,
        settingsValues: SettingsValues,
        currentAutoCapsState: Int,
        currentRecapitalizeState: Int
    ) {
        val builder = KeyboardLayoutSet.Builder(mThemeContext!!, editorInfo)
        val res = mThemeContext!!.resources
        val keyboardWidth = mLatinIME!!.maxWidth
        val keyboardHeight = ResourceUtils.getKeyboardHeight(res, settingsValues)
        val keyboardBottomOffset = ResourceUtils.getKeyboardBottomOffset(res, settingsValues)
        builder.setKeyboardTheme(mKeyboardTheme!!.mThemeId)
        builder.setKeyboardGeometry(keyboardWidth, keyboardHeight, keyboardBottomOffset)
        builder.setSubtype(mRichImm!!.currentSubtype)
        builder.setLanguageSwitchKeyEnabled(mLatinIME!!.shouldShowLanguageSwitchKey())
        builder.setShowSpecialChars(settingsValues.mShowSpecialChars)
        builder.setShowNumberRow(settingsValues.mShowNumberRow)
        mKeyboardLayoutSet = builder.build()
        try {
            mState.onLoadKeyboard(currentAutoCapsState, currentRecapitalizeState)
            mKeyboardTextsSet.setLocale(
                mRichImm!!.currentSubtype.getLocaleObject(),
                mThemeContext!!
            )
        } catch (e: KeyboardLayoutSetException) {
            Log.w(TAG, "loading keyboard failed: ${e.mKeyboardId}", e.cause)
        }

        if (mKeyboardView != null) {
            val parent = mKeyboardView!!.parent as? View
            if (parent is InputView) {
                parent.setToolbarVisible(settingsValues.mShowToolbar)
            }
        }
    }

    fun onHideWindow() {
        mKeyboardView?.onHideWindow()
    }

    private fun setKeyboard(keyboardId: Int, toggleState: KeyboardSwitchState) {
        val currentSettingsValues = Settings.getInstance().current
        setMainKeyboardFrame(currentSettingsValues, toggleState)
        val keyboardView = mKeyboardView!!
        val oldKeyboard = keyboardView.getKeyboard()
        val newKeyboard = mKeyboardLayoutSet!!.getKeyboard(keyboardId)
        keyboardView.setKeyboard(newKeyboard)
        keyboardView.setKeyPreviewPopupEnabled(
            currentSettingsValues.mKeyPreviewPopupOn,
            currentSettingsValues.mKeyPreviewPopupDismissDelay
        )
        val subtypeChanged = oldKeyboard == null ||
                newKeyboard.mId.mSubtype != oldKeyboard.mId.mSubtype
        val languageOnSpacebarFormatType = LanguageOnSpacebarUtils
            .getLanguageOnSpacebarFormatType(newKeyboard.mId.mSubtype)
        keyboardView.startDisplayLanguageOnSpacebar(subtypeChanged, languageOnSpacebarFormatType)
    }

    fun getKeyboard(): Keyboard? = mKeyboardView?.getKeyboard()

    fun resetKeyboardStateToAlphabet(currentAutoCapsState: Int, currentRecapitalizeState: Int) {
        mState.onResetKeyboardStateToAlphabet(currentAutoCapsState, currentRecapitalizeState)
    }

    fun onPressKey(code: Int, isSinglePointer: Boolean, currentAutoCapsState: Int, currentRecapitalizeState: Int) {
        mState.onPressKey(code, isSinglePointer, currentAutoCapsState, currentRecapitalizeState)
    }

    fun onReleaseKey(code: Int, withSliding: Boolean, currentAutoCapsState: Int, currentRecapitalizeState: Int) {
        mState.onReleaseKey(code, withSliding, currentAutoCapsState, currentRecapitalizeState)
    }

    fun onFinishSlidingInput(currentAutoCapsState: Int, currentRecapitalizeState: Int) {
        mState.onFinishSlidingInput(currentAutoCapsState, currentRecapitalizeState)
    }

    override fun setAlphabetKeyboard() {
        if (DEBUG_ACTION) Log.d(TAG, "setAlphabetKeyboard")
        setKeyboard(KeyboardId.ELEMENT_ALPHABET, KeyboardSwitchState.OTHER)
    }

    override fun setAlphabetManualShiftedKeyboard() {
        if (DEBUG_ACTION) Log.d(TAG, "setAlphabetManualShiftedKeyboard")
        setKeyboard(KeyboardId.ELEMENT_ALPHABET_MANUAL_SHIFTED, KeyboardSwitchState.OTHER)
    }

    override fun setAlphabetAutomaticShiftedKeyboard() {
        if (DEBUG_ACTION) Log.d(TAG, "setAlphabetAutomaticShiftedKeyboard")
        setKeyboard(KeyboardId.ELEMENT_ALPHABET_AUTOMATIC_SHIFTED, KeyboardSwitchState.OTHER)
    }

    override fun setAlphabetShiftLockedKeyboard() {
        if (DEBUG_ACTION) Log.d(TAG, "setAlphabetShiftLockedKeyboard")
        setKeyboard(KeyboardId.ELEMENT_ALPHABET_SHIFT_LOCKED, KeyboardSwitchState.OTHER)
    }

    override fun setSymbolsKeyboard() {
        if (DEBUG_ACTION) Log.d(TAG, "setSymbolsKeyboard")
        setKeyboard(KeyboardId.ELEMENT_SYMBOLS, KeyboardSwitchState.OTHER)
    }

    override fun setSymbolsShiftedKeyboard() {
        if (DEBUG_ACTION) Log.d(TAG, "setSymbolsShiftedKeyboard")
        setKeyboard(KeyboardId.ELEMENT_SYMBOLS_SHIFTED, KeyboardSwitchState.SYMBOLS_SHIFTED)
    }

    fun isImeSuppressedByHardwareKeyboard(settingsValues: SettingsValues, toggleState: KeyboardSwitchState): Boolean {
        return settingsValues.mHasHardwareKeyboard && toggleState == KeyboardSwitchState.HIDDEN
    }

    private fun setMainKeyboardFrame(settingsValues: SettingsValues, toggleState: KeyboardSwitchState) {
        val visibility = if (isImeSuppressedByHardwareKeyboard(settingsValues, toggleState))
            View.GONE else View.VISIBLE
        mKeyboardView?.visibility = visibility
    }

    enum class KeyboardSwitchState(val mKeyboardId: Int) {
        HIDDEN(-1),
        SYMBOLS_SHIFTED(KeyboardId.ELEMENT_SYMBOLS_SHIFTED),
        OTHER(-1);
    }

    fun getKeyboardSwitchState(): KeyboardSwitchState {
        val hidden = mKeyboardLayoutSet == null ||
                mKeyboardView == null ||
                !mKeyboardView!!.isShown
        return when {
            hidden -> KeyboardSwitchState.HIDDEN
            isShowingKeyboardId(KeyboardId.ELEMENT_SYMBOLS_SHIFTED) -> KeyboardSwitchState.SYMBOLS_SHIFTED
            else -> KeyboardSwitchState.OTHER
        }
    }

    override fun requestUpdatingShiftState(autoCapsFlags: Int, recapitalizeMode: Int) {
        if (DEBUG_ACTION) Log.d(TAG, "requestUpdatingShiftState: autoCapsFlags=${CapsModeUtils.flagsToString(autoCapsFlags)} recapitalizeMode=${RecapitalizeStatus.modeToString(recapitalizeMode)}")
        mState.onUpdateShiftState(autoCapsFlags, recapitalizeMode)
    }

    override fun startDoubleTapShiftKeyTimer() {
        if (DEBUG_TIMER_ACTION) Log.d(TAG, "startDoubleTapShiftKeyTimer")
        getMainKeyboardView()?.startDoubleTapShiftKeyTimer()
    }

    override fun cancelDoubleTapShiftKeyTimer() {
        if (DEBUG_TIMER_ACTION) Log.d(TAG, "setAlphabetKeyboard")
        getMainKeyboardView()?.cancelDoubleTapShiftKeyTimer()
    }

    override fun isInDoubleTapShiftKeyTimeout(): Boolean {
        if (DEBUG_TIMER_ACTION) Log.d(TAG, "isInDoubleTapShiftKeyTimeout")
        return getMainKeyboardView()?.isInDoubleTapShiftKeyTimeout() == true
    }

    fun onEvent(event: Event, currentAutoCapsState: Int, currentRecapitalizeState: Int) {
        mState.onEvent(event, currentAutoCapsState, currentRecapitalizeState)
    }

    fun isShowingKeyboardId(vararg keyboardIds: Int): Boolean {
        if (mKeyboardView == null || !mKeyboardView!!.isShown) return false
        val activeKeyboardId = mKeyboardView!!.getKeyboard().mId.mElementId
        return keyboardIds.any { it == activeKeyboardId }
    }

    fun isShowingMoreKeysPanel(): Boolean = mKeyboardView?.isShowingMoreKeysPanel() == true

    fun getVisibleKeyboardView(): View? = mKeyboardView

    fun getMainKeyboardView(): MainKeyboardView? = mKeyboardView

    fun deallocateMemory() {
        mKeyboardView?.cancelAllOngoingEvents()
        mKeyboardView?.deallocateMemory()
    }

    fun onCreateInputView(): View {
        mKeyboardView?.closing()

        updateKeyboardThemeAndContextThemeWrapper(
            mLatinIME!!, KeyboardTheme.getKeyboardTheme(mLatinIME!!)
        )
        val currentInputView = LayoutInflater.from(mThemeContext).inflate(
            R.layout.input_view, null
        ) as InputView

        mKeyboardView = currentInputView.findViewById(R.id.keyboard_view)
        mKeyboardView!!.setKeyboardActionListener(mLatinIME!!)

        val clipboardBtn = currentInputView.findViewById<ImageView>(R.id.toolbar_clipboard)
        val settingsBtn = currentInputView.findViewById<ImageView>(R.id.toolbar_settings)
        val numpadBtn = currentInputView.findViewById<ImageView>(R.id.toolbar_numpad)

        clipboardBtn?.setOnClickListener { mLatinIME!!.showClipboardManager() }
        settingsBtn?.setOnClickListener { mLatinIME!!.launchSettings() }
        numpadBtn?.setOnClickListener { mLatinIME!!.toggleNumPad() }

        val settingsValues = Settings.getInstance().current
        if (settingsValues != null) {
            currentInputView.setToolbarVisible(settingsValues.mShowToolbar)
        }

        return currentInputView
    }

    companion object {
        private const val TAG = "KeyboardSwitcher"
        private const val DEBUG_ACTION = false
        private const val DEBUG_TIMER_ACTION = false

        private val sInstance = KeyboardSwitcher()

        @JvmStatic
        fun getInstance(): KeyboardSwitcher = sInstance

        @JvmStatic
        fun init(latinIme: LatinIME) {
            sInstance.initInternal(latinIme)
        }
    }
}
