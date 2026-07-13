/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (C) 2025 Raimondas Rimkus
 * Copyright (C) 2021 wittmane
 * Copyright (C) 2021 Maarten Trompper
 * Copyright (C) 2019 Micha LaQua
 * Copyright (C) 2019 Emmanuel
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

package rkr.simplekeyboard.inputmethod.latin

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.Debug
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.util.PrintWriterPrinter
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.view.WindowInsetsController
import android.view.inputmethod.EditorInfo
import rkr.simplekeyboard.inputmethod.compat.EditorInfoCompatUtils
import rkr.simplekeyboard.inputmethod.compat.PreferenceManagerCompat
import rkr.simplekeyboard.inputmethod.event.Event
import rkr.simplekeyboard.inputmethod.event.InputTransaction
import rkr.simplekeyboard.inputmethod.keyboard.Keyboard
import rkr.simplekeyboard.inputmethod.keyboard.KeyboardActionListener
import rkr.simplekeyboard.inputmethod.keyboard.KeyboardId
import rkr.simplekeyboard.inputmethod.keyboard.KeyboardSwitcher
import rkr.simplekeyboard.inputmethod.keyboard.MainKeyboardView
import rkr.simplekeyboard.inputmethod.latin.common.Constants
import rkr.simplekeyboard.inputmethod.latin.define.DebugFlags
import rkr.simplekeyboard.inputmethod.latin.inputlogic.InputLogic
import rkr.simplekeyboard.inputmethod.latin.settings.Settings
import rkr.simplekeyboard.inputmethod.latin.settings.SettingsActivity
import rkr.simplekeyboard.inputmethod.latin.settings.SettingsValues
import rkr.simplekeyboard.inputmethod.latin.utils.ApplicationUtils
import rkr.simplekeyboard.inputmethod.latin.utils.LeakGuardHandlerWrapper
import rkr.simplekeyboard.inputmethod.latin.utils.ResourceUtils
import rkr.simplekeyboard.inputmethod.latin.utils.ViewLayoutUtils
import java.io.FileDescriptor
import java.io.PrintWriter
import java.util.Locale
import java.util.concurrent.TimeUnit

class LatinIME : InputMethodService(), KeyboardActionListener,
    RichInputMethodManager.SubtypeChangedListener {

    class UIHandler(ownerInstance: LatinIME) : LeakGuardHandlerWrapper<LatinIME>(ownerInstance) {

        companion object {
            private const val MSG_UPDATE_SHIFT_STATE = 0
            private const val MSG_PENDING_IMS_CALLBACK = 1
            private const val MSG_DEALLOCATE_MEMORY = 9
        }

        private var mIsOrientationChanging = false
        private var mPendingSuccessiveImsCallback = false
        private var mHasPendingStartInput = false
        private var mHasPendingFinishInputView = false
        private var mHasPendingFinishInput = false
        private var mAppliedEditorInfo: EditorInfo? = null

        override fun handleMessage(msg: Message) {
            val latinIme = ownerInstance ?: return
            val switcher = latinIme.mKeyboardSwitcher
            when (msg.what) {
                MSG_UPDATE_SHIFT_STATE -> switcher.requestUpdatingShiftState(
                    latinIme.getCurrentAutoCapsState(),
                    latinIme.getCurrentRecapitalizeState()
                )
                MSG_DEALLOCATE_MEMORY -> latinIme.deallocateMemory()
            }
        }

        fun postUpdateShiftState() {
            removeMessages(MSG_UPDATE_SHIFT_STATE)
            sendMessage(obtainMessage(MSG_UPDATE_SHIFT_STATE))
        }

        fun postDeallocateMemory() {
            sendMessageDelayed(
                obtainMessage(MSG_DEALLOCATE_MEMORY),
                DELAY_DEALLOCATE_MEMORY_MILLIS
            )
        }

        fun cancelDeallocateMemory() {
            removeMessages(MSG_DEALLOCATE_MEMORY)
        }

        fun hasPendingDeallocateMemory(): Boolean {
            return hasMessages(MSG_DEALLOCATE_MEMORY)
        }

        private fun resetPendingImsCallback() {
            mHasPendingFinishInputView = false
            mHasPendingFinishInput = false
            mHasPendingStartInput = false
        }

        private fun executePendingImsCallback(
            latinIme: LatinIME,
            editorInfo: EditorInfo?,
            restarting: Boolean
        ) {
            if (mHasPendingFinishInputView) {
                latinIme.onFinishInputViewInternal(mHasPendingFinishInput)
            }
            if (mHasPendingFinishInput) {
                latinIme.onFinishInputInternal()
            }
            if (mHasPendingStartInput) {
                latinIme.onStartInputInternal(editorInfo!!, restarting)
            }
            resetPendingImsCallback()
        }

        fun onStartInput(editorInfo: EditorInfo, restarting: Boolean) {
            if (hasMessages(MSG_PENDING_IMS_CALLBACK)) {
                mHasPendingStartInput = true
            } else {
                if (mIsOrientationChanging && restarting) {
                    mIsOrientationChanging = false
                    mPendingSuccessiveImsCallback = true
                }
                val latinIme = ownerInstance
                if (latinIme != null) {
                    executePendingImsCallback(latinIme, editorInfo, restarting)
                    latinIme.onStartInputInternal(editorInfo, restarting)
                }
            }
        }

        fun onStartInputView(editorInfo: EditorInfo, restarting: Boolean) {
            if (hasMessages(MSG_PENDING_IMS_CALLBACK)
                && KeyboardId.equivalentEditorInfoForKeyboard(editorInfo, mAppliedEditorInfo)
            ) {
                resetPendingImsCallback()
            } else {
                if (mPendingSuccessiveImsCallback) {
                    mPendingSuccessiveImsCallback = false
                    resetPendingImsCallback()
                    sendMessageDelayed(
                        obtainMessage(MSG_PENDING_IMS_CALLBACK),
                        PENDING_IMS_CALLBACK_DURATION_MILLIS
                    )
                }
                val latinIme = ownerInstance
                if (latinIme != null) {
                    executePendingImsCallback(latinIme, editorInfo, restarting)
                    latinIme.onStartInputViewInternal(editorInfo, restarting)
                    mAppliedEditorInfo = editorInfo
                }
                cancelDeallocateMemory()
            }
        }

        fun onFinishInputView(finishingInput: Boolean) {
            if (hasMessages(MSG_PENDING_IMS_CALLBACK)) {
                mHasPendingFinishInputView = true
            } else {
                val latinIme = ownerInstance
                if (latinIme != null) {
                    latinIme.onFinishInputViewInternal(finishingInput)
                    mAppliedEditorInfo = null
                }
                if (!hasPendingDeallocateMemory()) {
                    postDeallocateMemory()
                }
            }
        }

        fun onFinishInput() {
            if (hasMessages(MSG_PENDING_IMS_CALLBACK)) {
                mHasPendingFinishInput = true
            } else {
                val latinIme = ownerInstance
                if (latinIme != null) {
                    executePendingImsCallback(latinIme, null, false)
                    latinIme.onFinishInputInternal()
                }
            }
        }
    }

    val mSettings: Settings = Settings.getInstance()
    private var mLocale: Locale? = null
    val mInputLogic: InputLogic = InputLogic(this)
    private var mClipboardStore: ClipboardStore? = null

    private var mInputView: View? = null
    private var mRichImm: RichInputMethodManager? = null
    val mKeyboardSwitcher: KeyboardSwitcher = KeyboardSwitcher.getInstance()

    private var mOptionsDialog: AlertDialog? = null

    val mHandler = UIHandler(this)

    override fun onCreate() {
        Settings.init(this)
        DebugFlags.init(PreferenceManagerCompat.getDeviceSharedPreferences(this))
        RichInputMethodManager.init(this)
        mRichImm = RichInputMethodManager.getInstance()
        mRichImm?.setSubtypeChangeHandler(this)
        KeyboardSwitcher.init(this)
        AudioAndHapticFeedbackManager.init(this)
        mClipboardStore = ClipboardStore(this)
        super.onCreate()

        val systemClipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        systemClipboard?.addPrimaryClipChangedListener {
            val clip = systemClipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text
                if (text != null) {
                    mClipboardStore?.addItem(text.toString())
                }
            }
        }

        loadSettings()

        val filter = IntentFilter()
        filter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
        registerReceiver(mRingerModeChangeReceiver, filter)
    }

    private fun loadSettings() {
        mLocale = mRichImm?.currentSubtype?.getLocaleObject()
        val editorInfo = currentInputEditorInfo
        val inputAttributes = InputAttributes(editorInfo, isFullscreenMode)
        mSettings.loadSettings(inputAttributes)
        val currentSettingsValues = mSettings.current
        AudioAndHapticFeedbackManager.instance.onSettingsChanged(currentSettingsValues)
    }

    override fun onDestroy() {
        mSettings.onDestroy()
        unregisterReceiver(mRingerModeChangeReceiver)
        super.onDestroy()
    }

    private fun isImeSuppressedByHardwareKeyboard(): Boolean {
        val switcher = KeyboardSwitcher.getInstance()
        return !onEvaluateInputViewShown() && switcher.isImeSuppressedByHardwareKeyboard(
            mSettings.current, switcher.keyboardSwitchState
        )
    }

    override fun onEvaluateInputViewShown(): Boolean {
        val useOnScreen = super.onEvaluateInputViewShown()
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) {
            useOnScreen
        } else {
            useOnScreen || mSettings.current.mUseOnScreen
        }
    }

    override fun onConfigurationChanged(conf: Configuration) {
        val settingsValues = mSettings.current
        if (settingsValues.mHasHardwareKeyboard != Settings.readHasHardwareKeyboard(conf)) {
            loadSettings()
        }

        mKeyboardSwitcher.onConfigurationChanged()

        super.onConfigurationChanged(conf)
    }

    override fun onCreateInputView(): View {
        return mKeyboardSwitcher.onCreateInputView()
    }

    override fun setInputView(view: View) {
        super.setInputView(view)
        mInputView = view
        updateSoftInputWindowLayoutParameters()
        view.requestApplyInsets()
    }

    override fun setCandidatesView(view: View) {
        // To ensure that CandidatesView will never be set.
    }

    override fun onStartInput(editorInfo: EditorInfo, restarting: Boolean) {
        mHandler.onStartInput(editorInfo, restarting)
    }

    override fun onStartInputView(editorInfo: EditorInfo, restarting: Boolean) {
        mHandler.onStartInputView(editorInfo, restarting)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        mInputLogic.clearCaches()
        mRichImm?.resetSubtypeCycleOrder()
        mHandler.onFinishInputView(finishingInput)
    }

    override fun onFinishInput() {
        mHandler.onFinishInput()
    }

    override fun onCurrentSubtypeChanged() {
        mInputLogic.onSubtypeChanged()
        loadKeyboard()
    }

    internal fun onStartInputInternal(editorInfo: EditorInfo, restarting: Boolean) {
        super.onStartInput(editorInfo, restarting)

        val primaryHintLocale = EditorInfoCompatUtils.getPrimaryHintLocale(editorInfo)
        if (primaryHintLocale == null) {
            return
        }
        mRichImm?.setCurrentSubtype(primaryHintLocale)
    }

    internal fun onStartInputViewInternal(editorInfo: EditorInfo, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)

        val switcher = mKeyboardSwitcher
        switcher.updateKeyboardTheme()
        val mainKeyboardView = switcher.mainKeyboardView
        var currentSettingsValues = mSettings.current

        if (editorInfo == null) {
            Log.e(TAG, "Null EditorInfo in onStartInputView()")
            if (DebugFlags.DEBUG_ENABLED) {
                throw NullPointerException("Null EditorInfo in onStartInputView()")
            }
            return
        }
        if (DebugFlags.DEBUG_ENABLED) {
            Log.d(
                TAG, "onStartInputView: editorInfo:" +
                        String.format(
                            "inputType=0x%08x imeOptions=0x%08x",
                            editorInfo.inputType, editorInfo.imeOptions
                        )
            )
            Log.d(
                TAG, "All caps = "
                        + (editorInfo.inputType and InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS != 0)
                        + ", sentence caps = "
                        + (editorInfo.inputType and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES != 0)
                        + ", word caps = "
                        + (editorInfo.inputType and InputType.TYPE_TEXT_FLAG_CAP_WORDS != 0)
            )
        }
        Log.i(
            TAG, "Starting input. Cursor position = "
                    + editorInfo.initialSelStart + "," + editorInfo.initialSelEnd +
                    " Restarting = $restarting"
        )

        if (mainKeyboardView == null) {
            return
        }

        val inputTypeChanged = !currentSettingsValues.isSameInputType(editorInfo)
        val isDifferentTextField = !restarting || inputTypeChanged

        updateFullscreenMode()

        if (!isImeSuppressedByHardwareKeyboard()) {
            mInputLogic.startInput()
            mInputLogic.mConnection.reloadTextCache(editorInfo, restarting)
        }

        if (isDifferentTextField ||
            !currentSettingsValues.hasSameOrientation(resources.configuration)
        ) {
            loadSettings()
        }
        if (isDifferentTextField) {
            mainKeyboardView.closing()
            currentSettingsValues = mSettings.current

            switcher.loadKeyboard(
                editorInfo, currentSettingsValues, getCurrentAutoCapsState(),
                getCurrentRecapitalizeState()
            )
        } else {
            switcher.resetKeyboardStateToAlphabet(
                getCurrentAutoCapsState(),
                getCurrentRecapitalizeState()
            )
        }

        if (TRACE) Debug.startMethodTracing("/data/trace/latinime")
    }

    override fun onWindowShown() {
        super.onWindowShown()
        if (isInputViewShown) setNavigationBarColor()
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        val mainKeyboardView = mKeyboardSwitcher.mainKeyboardView
        if (mainKeyboardView != null) {
            mainKeyboardView.closing()
        }
    }

    internal fun onFinishInputInternal() {
        super.onFinishInput()

        val mainKeyboardView = mKeyboardSwitcher.mainKeyboardView
        if (mainKeyboardView != null) {
            mainKeyboardView.closing()
        }
    }

    internal fun onFinishInputViewInternal(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
    }

    protected fun deallocateMemory() {
        mKeyboardSwitcher.deallocateMemory()
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        composingSpanStart: Int, composingSpanEnd: Int
    ) {
        super.onUpdateSelection(
            oldSelStart, oldSelEnd, newSelStart, newSelEnd,
            composingSpanStart, composingSpanEnd
        )
        val keyboardView = mKeyboardSwitcher.mainKeyboardView
        if (keyboardView != null && keyboardView.isInCursorMove) {
            return
        }

        Log.i(TAG, "Update Selection. Cursor position = $newSelStart,$newSelEnd")

        mInputLogic.onUpdateSelection(newSelStart, newSelEnd)
        if (isInputViewShown) {
            mInputLogic.reloadTextCache()

            mKeyboardSwitcher.requestUpdatingShiftState(
                getCurrentAutoCapsState(),
                getCurrentRecapitalizeState()
            )
        }
    }

    override fun hideWindow() {
        mKeyboardSwitcher.onHideWindow()

        if (TRACE) Debug.stopMethodTracing()
        if (isShowingOptionDialog()) {
            mOptionsDialog?.dismiss()
            mOptionsDialog = null
        }
        super.hideWindow()
    }

    override fun onComputeInsets(outInsets: InputMethodService.Insets) {
        super.onComputeInsets(outInsets)
        if (mInputView == null) {
            return
        }
        val visibleKeyboardView = mKeyboardSwitcher.visibleKeyboardView
        if (visibleKeyboardView == null) {
            return
        }
        val inputHeight = mInputView!!.height
        if (isImeSuppressedByHardwareKeyboard() && !visibleKeyboardView.isShown) {
            outInsets.contentTopInsets = inputHeight
            outInsets.visibleTopInsets = inputHeight
            return
        }
        val visibleTopY = inputHeight - visibleKeyboardView.height
        if (visibleKeyboardView.isShown) {
            val touchLeft = 0
            val touchTop = if (mKeyboardSwitcher.isShowingMoreKeysPanel) 0 else visibleTopY
            val touchRight = visibleKeyboardView.width
            val touchBottom = inputHeight + EXTENDED_TOUCHABLE_REGION_HEIGHT
            outInsets.touchableInsets = InputMethodService.Insets.TOUCHABLE_INSETS_REGION
            outInsets.touchableRegion.set(touchLeft, touchTop, touchRight, touchBottom)
        }
        outInsets.contentTopInsets = visibleTopY
        outInsets.visibleTopInsets = visibleTopY
    }

    override fun onShowInputRequested(flags: Int, configChange: Boolean): Boolean {
        if (isImeSuppressedByHardwareKeyboard()) {
            return true
        }
        return super.onShowInputRequested(flags, configChange)
    }

    override fun onEvaluateFullscreenMode(): Boolean {
        if (isImeSuppressedByHardwareKeyboard()) {
            return false
        }
        val isFullscreenModeAllowed = Settings.readUseFullscreenMode(resources)
        if (super.onEvaluateFullscreenMode() && isFullscreenModeAllowed) {
            val ei = currentInputEditorInfo
            return !(ei != null && (ei.imeOptions and EditorInfo.IME_FLAG_NO_EXTRACT_UI) != 0)
        }
        return false
    }

    override fun updateFullscreenMode() {
        super.updateFullscreenMode()
        updateSoftInputWindowLayoutParameters()
    }

    private fun updateSoftInputWindowLayoutParameters() {
        val window = window.window
        ViewLayoutUtils.updateLayoutHeightOf(window, LayoutParams.MATCH_PARENT)
        if (mInputView != null) {
            val layoutHeight = if (isFullscreenMode()) {
                LayoutParams.WRAP_CONTENT
            } else {
                LayoutParams.MATCH_PARENT
            }
            val inputArea = window?.findViewById<View>(android.R.id.inputArea)
            if (inputArea != null) {
                ViewLayoutUtils.updateLayoutHeightOf(inputArea, layoutHeight)
                ViewLayoutUtils.updateLayoutGravityOf(inputArea, Gravity.BOTTOM)
            }
            ViewLayoutUtils.updateLayoutHeightOf(mInputView, layoutHeight)
        }
    }

    internal fun getCurrentAutoCapsState(): Int {
        return mInputLogic.getCurrentAutoCapsState(
            mSettings.current,
            mRichImm?.currentSubtype?.keyboardLayoutSet ?: ""
        )
    }

    internal fun getCurrentRecapitalizeState(): Int {
        return mInputLogic.getCurrentRecapitalizeState()
    }

    override fun onCustomRequest(requestCode: Int): Boolean {
        return when (requestCode) {
            Constants.CUSTOM_CODE_SHOW_INPUT_METHOD_PICKER -> showInputMethodPicker()
            else -> false
        }
    }

    private fun showInputMethodPicker(): Boolean {
        if (isShowingOptionDialog()) {
            return false
        }
        mOptionsDialog = mRichImm?.showSubtypePicker(
            this,
            mKeyboardSwitcher.mainKeyboardView?.windowToken, this
        )
        return mOptionsDialog != null
    }

    fun getCurrentLayoutLocale(): Locale? {
        return mLocale
    }

    override fun onMoveCursorPointer(steps: Int) {
        var remainingSteps = steps
        if (mInputLogic.mConnection.hasCursorPosition()) {
            if (TextUtils.getLayoutDirectionFromLocale(getCurrentLayoutLocale()) == View.LAYOUT_DIRECTION_RTL) {
                remainingSteps = -remainingSteps
            }

            remainingSteps = mInputLogic.mConnection.getUnicodeSteps(remainingSteps, true)
            if (remainingSteps == 0) {
                return
            }
            val end = mInputLogic.mConnection.getExpectedSelectionEnd() + remainingSteps
            val start = if (mInputLogic.mConnection.hasSelection()) {
                mInputLogic.mConnection.getExpectedSelectionStart()
            } else {
                end
            }
            mInputLogic.mConnection.setSelection(start, end)
            hapticTickFeedback()
        } else {
            while (remainingSteps < 0) {
                mInputLogic.sendDownUpKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT)
                remainingSteps++
            }
            while (remainingSteps > 0) {
                mInputLogic.sendDownUpKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT)
                remainingSteps--
            }
            hapticTickFeedback()
        }
    }

    override fun onMoveDeletePointer(steps: Int) {
        var remainingSteps = steps
        if (mInputLogic.mConnection.hasCursorPosition()) {
            remainingSteps = mInputLogic.mConnection.getUnicodeSteps(remainingSteps, false)
            if (remainingSteps == 0) {
                return
            }
            val end = mInputLogic.mConnection.getExpectedSelectionEnd()
            val start = mInputLogic.mConnection.getExpectedSelectionStart() + remainingSteps
            mInputLogic.mConnection.setSelection(start, end)
            hapticTickFeedback()
        } else {
            while (remainingSteps < 0) {
                mInputLogic.sendDownUpKeyEvent(KeyEvent.KEYCODE_DEL)
                remainingSteps++
            }
            hapticTickFeedback()
        }
    }

    override fun onUpWithDeletePointerActive() {
        if (mInputLogic.mConnection.hasSelection()) {
            mInputLogic.mConnection.deleteSelectedText()
        }
    }

    override fun onUpWithSpacePointerActive() {
        mInputLogic.reloadTextCache()
    }

    private fun isShowingOptionDialog(): Boolean {
        return mOptionsDialog != null && mOptionsDialog?.isShowing == true
    }

    fun switchToNextSubtype() {
        val token = window.window?.attributes?.token
        mRichImm?.switchToNextInputMethod(token, !shouldSwitchToOtherInputMethods(token))
    }

    private fun getCodePointForKeyboard(codePoint: Int): Int {
        if (Constants.CODE_SHIFT == codePoint) {
            val currentKeyboard = mKeyboardSwitcher.keyboard
            if (currentKeyboard != null && currentKeyboard.mId.isAlphabetKeyboard()) {
                return codePoint
            }
            return Constants.CODE_SYMBOL_SHIFT
        }
        return codePoint
    }

    override fun onCodeInput(codePoint: Int, x: Int, y: Int, isKeyRepeat: Boolean) {
        val event = createSoftwareKeypressEvent(getCodePointForKeyboard(codePoint), isKeyRepeat)
        onEvent(event)
    }

    fun onEvent(event: Event) {
        val completeInputTransaction = mInputLogic.onCodeInput(mSettings.current, event)
        updateStateAfterInputTransaction(completeInputTransaction)
        mKeyboardSwitcher.onEvent(event, getCurrentAutoCapsState(), getCurrentRecapitalizeState())
    }

    companion object {
        @JvmField
        val TAG: String = LatinIME::class.java.simpleName
        private const val TRACE = false

        private const val EXTENDED_TOUCHABLE_REGION_HEIGHT = 100
        private const val PERIOD_FOR_AUDIO_AND_HAPTIC_FEEDBACK_IN_KEY_REPEAT = 2
        private const val PENDING_IMS_CALLBACK_DURATION_MILLIS = 800
        @JvmField
        val DELAY_DEALLOCATE_MEMORY_MILLIS: Long = TimeUnit.SECONDS.toMillis(10)

        @JvmStatic
        fun createSoftwareKeypressEvent(keyCodeOrCodePoint: Int, isKeyRepeat: Boolean): Event {
            val keyCode: Int
            val codePoint: Int
            if (keyCodeOrCodePoint <= 0) {
                keyCode = keyCodeOrCodePoint
                codePoint = Event.NOT_A_CODE_POINT
            } else {
                keyCode = Event.NOT_A_KEY_CODE
                codePoint = keyCodeOrCodePoint
            }
            return Event.createSoftwareKeypressEvent(codePoint, keyCode, isKeyRepeat)
        }
    }

    override fun onTextInput(rawText: String) {
        val event = Event.createSoftwareTextEvent(rawText, Constants.CODE_OUTPUT_TEXT)
        val completeInputTransaction = mInputLogic.onTextInput(mSettings.current, event)
        updateStateAfterInputTransaction(completeInputTransaction)
        mKeyboardSwitcher.onEvent(event, getCurrentAutoCapsState(), getCurrentRecapitalizeState())
    }

    override fun onFinishSlidingInput() {
        mKeyboardSwitcher.onFinishSlidingInput(
            getCurrentAutoCapsState(),
            getCurrentRecapitalizeState()
        )
    }

    fun getClipboardStore(): ClipboardStore? {
        return mClipboardStore
    }

    fun showClipboardManager() {
        val inputView = mInputView as? InputView ?: return

        val items = mClipboardStore?.getItems() ?: emptyList()
        inputView.showClipboardPopup(items) { text ->
            onTextInput(text)
        }
    }

    fun toggleNumPad() {
        val switcher = mKeyboardSwitcher
        val currentKeyboard = switcher.keyboard
        if (currentKeyboard != null && currentKeyboard.mId.isAlphabetKeyboard()) {
            switcher.setSymbolsKeyboard()
        } else {
            switcher.setAlphabetKeyboard()
        }
    }

    private fun loadKeyboard() {
        loadSettings()
        if (mKeyboardSwitcher.mainKeyboardView != null) {
            mKeyboardSwitcher.loadKeyboard(
                currentInputEditorInfo, mSettings.current,
                getCurrentAutoCapsState(), getCurrentRecapitalizeState()
            )
        }
    }

    private fun updateStateAfterInputTransaction(inputTransaction: InputTransaction) {
        when (inputTransaction.getRequiredShiftUpdate()) {
            InputTransaction.SHIFT_UPDATE_LATER -> mHandler.postUpdateShiftState()
            InputTransaction.SHIFT_UPDATE_NOW -> mKeyboardSwitcher.requestUpdatingShiftState(
                getCurrentAutoCapsState(),
                getCurrentRecapitalizeState()
            )
            else -> { /* SHIFT_NO_UPDATE */ }
        }
    }

    private fun hapticAndAudioFeedback(code: Int, repeatCount: Int) {
        val keyboardView = mKeyboardSwitcher.mainKeyboardView
        if (keyboardView != null && keyboardView.isInDraggingFinger) {
            return
        }
        if (repeatCount > 0) {
            if (code == Constants.CODE_DELETE && !mInputLogic.mConnection.canDeleteCharacters()) {
                return
            }
            if (repeatCount % PERIOD_FOR_AUDIO_AND_HAPTIC_FEEDBACK_IN_KEY_REPEAT == 0) {
                return
            }
        }
        val feedbackManager = AudioAndHapticFeedbackManager.instance
        if (repeatCount == 0) {
            feedbackManager.performHapticFeedback(keyboardView)
        }
        feedbackManager.performAudioFeedback(code)
    }

    private fun hapticTickFeedback() {
        AudioAndHapticFeedbackManager.instance.performTickFeedback()
    }

    override fun onPressKey(primaryCode: Int, repeatCount: Int, isSinglePointer: Boolean) {
        mKeyboardSwitcher.onPressKey(
            primaryCode, isSinglePointer, getCurrentAutoCapsState(),
            getCurrentRecapitalizeState()
        )
        hapticAndAudioFeedback(primaryCode, repeatCount)
    }

    override fun onReleaseKey(primaryCode: Int, withSliding: Boolean) {
        mKeyboardSwitcher.onReleaseKey(
            primaryCode, withSliding, getCurrentAutoCapsState(),
            getCurrentRecapitalizeState()
        )
    }

    private val mRingerModeChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == AudioManager.RINGER_MODE_CHANGED_ACTION) {
                AudioAndHapticFeedbackManager.instance.onRingerModeChanged()
            }
        }
    }

    fun launchSettings() {
        requestHideSelf(0)
        val mainKeyboardView = mKeyboardSwitcher.mainKeyboardView
        if (mainKeyboardView != null) {
            mainKeyboardView.closing()
        }
        val intent = Intent().apply {
            setClass(this@LatinIME, SettingsActivity::class.java)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    override fun dump(fd: FileDescriptor, fout: PrintWriter, args: Array<String>) {
        super.dump(fd, fout, args)

        val p = PrintWriterPrinter(fout)
        p.println("LatinIME state :")
        p.println("  VersionCode = " + ApplicationUtils.getVersionCode(this))
        p.println("  VersionName = " + ApplicationUtils.getVersionName(this))
        val keyboard = mKeyboardSwitcher.keyboard
        val keyboardMode = keyboard?.mId?.mMode ?: -1
        p.println("  Keyboard mode = $keyboardMode")
    }

    fun shouldSwitchToOtherInputMethods(token: IBinder?): Boolean {
        if (!mSettings.current.mImeSwitchEnabled) {
            return false
        }
        return mRichImm?.shouldOfferSwitchingToOtherInputMethods(token) ?: false
    }

    fun shouldShowLanguageSwitchKey(): Boolean {
        if (mSettings.current.isLanguageSwitchKeyDisabled()) {
            return false
        }
        if (mRichImm?.hasMultipleEnabledSubtypes() == true) {
            return true
        }

        val token = window.window?.attributes?.token
        if (token == null) {
            return false
        }
        return shouldSwitchToOtherInputMethods(token)
    }

    private fun setNavigationBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val window = window.window ?: return
            val prefs = PreferenceManagerCompat.getDeviceSharedPreferences(this)
            val keyboardColor = Settings.readKeyboardColor(prefs, this)
            window.navigationBarColor = keyboardColor
            window.isNavigationBarContrastEnforced = false
            val flag = WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            if (ResourceUtils.isBrightColor(keyboardColor)) {
                window.insetsController?.setSystemBarsAppearance(flag, flag)
            } else {
                window.insetsController?.setSystemBarsAppearance(0, flag)
            }
        }
    }
}
