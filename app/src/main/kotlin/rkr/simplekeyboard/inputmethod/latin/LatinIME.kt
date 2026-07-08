package rkr.simplekeyboard.inputmethod.latin

import android.content.Intent
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.keyboard.KeyboardActionListener
import rkr.simplekeyboard.inputmethod.keyboard.KeyboardLayoutSet
import rkr.simplekeyboard.inputmethod.keyboard.KeyboardSwitcher
import rkr.simplekeyboard.inputmethod.keyboard.MainKeyboardView
import rkr.simplekeyboard.inputmethod.keyboard.PointerTracker
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeyboardState
import rkr.simplekeyboard.inputmethod.latin.common.Constants
import rkr.simplekeyboard.inputmethod.latin.inputlogic.InputLogic
import rkr.simplekeyboard.inputmethod.latin.settings.Settings
import rkr.simplekeyboard.inputmethod.latin.settings.SettingsValues
import rkr.simplekeyboard.inputmethod.latin.utils.CapsModeUtils
import rkr.simplekeyboard.inputmethod.latin.utils.LeakGuardHandlerWrapper
import rkr.simplekeyboard.inputmethod.latin.utils.RecapitalizeStatus
import rkr.simplekeyboard.inputmethod.latin.utils.SubtypePreferenceUtils

class LatinIME : InputMethodService(), KeyboardActionListener {
    val mHandler = UIHandler(this)
    private var mRichImm: RichInputMethodManager? = null
    private var mInputLogic: InputLogic? = null
    private var mSettingsValues: SettingsValues? = null
    val settingsValues: SettingsValues? get() = mSettingsValues
    private var mIsFullscreenMode = false
    private var mIsPressureInBarrelSensor = false
    private val mRecapitalizeStatus = RecapitalizeStatus()
    private var mIsInImeSubtypeSwitching = false
    private var mIsDeviceLocked = false

    override fun onCreate() {
        super.onCreate()
        Settings.init(this)
        RichInputMethodManager.init(this)
        mRichImm = RichInputMethodManager.getInstance()
        mInputLogic = InputLogic(this, mHandler)
        KeyboardSwitcher.init(this)
        mSettingsValues = Settings.getInstance().current
        AudioAndHapticFeedbackManager.init(this)
        AudioAndHapticFeedbackManager.getInstance().onSettingsChanged(mSettingsValues!!)
    }

    override fun onStartInput(editorInfo: EditorInfo?, restarting: Boolean) {
        super.onStartInput(editorInfo, restarting)
        if (editorInfo != null) {
            mInputLogic!!.startInput(editorInfo, restarting)
        }
    }

    override fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)
        val keyboardSwitcher = KeyboardSwitcher.getInstance()
        keyboardSwitcher.loadKeyboard(editorInfo!!, mSettingsValues!!, currentAutoCapsState, currentRecapitalizeState)
        mInputLogic!!.onStartInputView(restarting)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        mInputLogic!!.onFinishInput()
        val mainKeyboardView = KeyboardSwitcher.getInstance().getMainKeyboardView()
        mainKeyboardView?.closing()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        mInputLogic?.onFinishInput()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        KeyboardSwitcher.getInstance().onConfigurationChanged()
    }

    override fun onUpdateCursorAnchorInfo(cursorAnchorInfo: android.view.inputmethod.CursorAnchorInfo?) {
        mInputLogic?.onUpdateCursorAnchorInfo(cursorAnchorInfo)
    }

    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        mInputLogic?.onUpdateSelection(newSelStart, newSelEnd)
    }

    override fun onCreateInputView(): View {
        return KeyboardSwitcher.getInstance().onCreateInputView()
    }

    override fun onCreateCandidatesView(): View? = null

    override fun onComputeInsets(outInsets: android.inputmethodservice.InputMethodService.Insets?) {
        // No insets computation needed for now
    }

    override fun isInputViewShown(): Boolean = KeyboardSwitcher.getInstance().isShowingKeyboardId(0)

    fun shouldShowLanguageSwitchKey(): Boolean = mSettingsValues?.mShowsLanguageSwitchKey == true

    fun getInputView(): View? = KeyboardSwitcher.getInstance().getVisibleKeyboardView()

    override fun onPressKey(primaryCode: Int, repeatCount: Int, isSinglePointer: Boolean) {
        KeyboardSwitcher.getInstance().onPressKey(primaryCode, isSinglePointer, currentAutoCapsState, currentRecapitalizeState)
    }

    override fun onReleaseKey(primaryCode: Int, withSliding: Boolean) {
        KeyboardSwitcher.getInstance().onReleaseKey(primaryCode, withSliding, currentAutoCapsState, currentRecapitalizeState)
    }

    override fun onCodeInput(primaryCode: Int, x: Int, y: Int, isKeyRepeat: Boolean) {
        mInputLogic?.onCodeInput(primaryCode, isKeyRepeat, currentAutoCapsState, currentRecapitalizeState)
    }

    override fun onTextInput(rawText: String) {
        mInputLogic?.onTextInput(rawText)
    }

    override fun onFinishSlidingInput() {
        KeyboardSwitcher.getInstance().onFinishSlidingInput(currentAutoCapsState, currentRecapitalizeState)
    }

    override fun onCustomRequest(requestCode: Int): Boolean {
        return when (requestCode) {
            Constants.CUSTOM_CODE_SHOW_INPUT_METHOD_PICKER -> {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showInputMethodPicker(); true
            }
            else -> false
        }
    }

    override fun onMoveCursorPointer(steps: Int) {
        mInputLogic?.sendDownUpKeyEvents(android.view.KeyEvent.KEYCODE_DPAD_LEFT * steps)
    }

    override fun onMoveDeletePointer(steps: Int) {
        for (i in 0 until steps) mInputLogic?.sendDownUpKeyEvents(android.view.KeyEvent.KEYCODE_DEL)
    }

    override fun onUpWithDeletePointerActive() {}
    override fun onUpWithSpacePointerActive() {}
    override fun onToggleClipboardHistory() {
        KeyboardSwitcher.getInstance().getMainKeyboardView()?.toggleClipboardHistoryPanel()
    }

    private val currentAutoCapsState: Int
        get() {
            val ic = currentInputConnection ?: return 0
            val ei = currentInputEditorInfo
            val inputType = ei?.inputType ?: 0
            return if (mSettingsValues?.mAutoCap == true && inputType and InputType.TYPE_CLASS_TEXT != 0) {
                (ic as android.view.inputmethod.InputConnection).getCapsMode(inputType)
            } else {
                Constants.TextUtils.CAP_MODE_OFF
            }
        }

    private val currentRecapitalizeState: Int
        get() = if (mRecapitalizeStatus.isStarted()) mRecapitalizeStatus.getCurrentMode() else RecapitalizeStatus.NOT_A_RECAPITALIZE_MODE

    override fun getMaxWidth(): Int {
        val mainKeyboardView = KeyboardSwitcher.getInstance().getMainKeyboardView()
        return mainKeyboardView?.width ?: resources.displayMetrics.widthPixels
    }

    private fun onDeviceLocked() {
        mIsDeviceLocked = true
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        KeyboardSwitcher.getInstance().onHideWindow()
    }

    override fun onDestroy() {
        super.onDestroy()
        Settings.getInstance().onDestroy()
    }

    inner class UIHandler(ownerInstance: LatinIME) : LeakGuardHandlerWrapper<LatinIME>(ownerInstance) {
        private var mUpdateShiftStateMessageId = 0

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_UPDATE_SHIFT_STATE -> postUpdateShiftState()
            }
        }

        fun postUpdateShiftState() {
            removeMessages(MSG_UPDATE_SHIFT_STATE)
            sendEmptyMessageDelayed(MSG_UPDATE_SHIFT_STATE, 50)
        }
    }

    companion object {
        private const val TAG = "LatinIME"
        private const val MSG_UPDATE_SHIFT_STATE = 0
    }
}
