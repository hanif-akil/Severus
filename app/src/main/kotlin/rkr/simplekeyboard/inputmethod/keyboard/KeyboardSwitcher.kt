package rkr.simplekeyboard.inputmethod.keyboard

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.event.Event
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
        mThemeContext = ContextThemeWrapper(mLatinIME!!, mKeyboardTheme!!.mStyleId)
        KeyboardLayoutSet.onKeyboardThemeChanged()
        if (mKeyboardView != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            mLatinIME!!.setInputView(onCreateInputView())
        }
    }

    private fun updateKeyboardThemeAndContextThemeWrapper(context: Context, keyboardTheme: KeyboardTheme): Boolean {
        if (mThemeContext == null || keyboardTheme != mKeyboardTheme) {
            mKeyboardTheme = keyboardTheme
            mThemeContext = ContextThemeWrapper(context, keyboardTheme.mStyleId)
            KeyboardLayoutSet.onKeyboardThemeChanged()
            return true
        }
        return false
    }

    fun loadKeyboard(editorInfo: EditorInfo, settingsValues: SettingsValues, currentAutoCapsState: Int, currentRecapitalizeState: Int) {
        val builder = KeyboardLayoutSet.Builder(mThemeContext!!, editorInfo)
        val res = mThemeContext!!.resources
        val keyboardWidth = mLatinIME!!.getMaxWidth()
        val keyboardHeight = ResourceUtils.getKeyboardHeight(res, settingsValues)
        val keyboardBottomOffset = ResourceUtils.getKeyboardBottomOffset(res, settingsValues)
        builder.setKeyboardTheme(mKeyboardTheme!!.mThemeId)
        builder.setKeyboardGeometry(keyboardWidth, keyboardHeight, keyboardBottomOffset)
        builder.setSubtype(mRichImm!!.getCurrentSubtype())
        builder.setLanguageSwitchKeyEnabled(mLatinIME!!.shouldShowLanguageSwitchKey())
        builder.setShowSpecialChars(settingsValues.mShowSpecialChars)
        builder.setShowNumberRow(settingsValues.mShowNumberRow)
        mKeyboardLayoutSet = builder.build()
        try {
            mState.onLoadKeyboard(currentAutoCapsState, currentRecapitalizeState)
            mKeyboardTextsSet.setLocale(mRichImm!!.getCurrentSubtype().localeObject!!, mThemeContext!!)
        } catch (e: KeyboardLayoutSet.KeyboardLayoutSetException) {
            Log.w(TAG, "loading keyboard failed: ${e.mKeyboardId}", e.cause)
        }
    }

    fun onHideWindow() {
        mKeyboardView?.onHideWindow()
    }

    private fun setMainKeyboardFrame(settingsValues: SettingsValues?, toggleState: KeyboardSwitchState) {
        // Stub - sets up the main keyboard frame
    }

    private fun setKeyboard(keyboardId: Int, toggleState: KeyboardSwitchState) {
        val currentSettingsValues = Settings.getInstance().current
        setMainKeyboardFrame(currentSettingsValues, toggleState)
        val keyboardView = mKeyboardView ?: return
        val oldKeyboard = keyboardView.keyboard
        val newKeyboard = mKeyboardLayoutSet!!.getKeyboard(keyboardId)
        keyboardView.keyboard = newKeyboard
        keyboardView.setKeyPreviewPopupEnabled(currentSettingsValues!!.mKeyPreviewPopupOn, currentSettingsValues.mKeyPreviewPopupDismissDelay)
        val subtypeChanged = oldKeyboard == null || newKeyboard.mId!!.mSubtype != oldKeyboard.mId!!.mSubtype
        val languageOnSpacebarFormatType = LanguageOnSpacebarUtils.getLanguageOnSpacebarFormatType(newKeyboard.mId!!.mSubtype)
        keyboardView.startDisplayLanguageOnSpacebar(subtypeChanged, languageOnSpacebarFormatType)
    }

    fun getKeyboard(): Keyboard? = mKeyboardView?.keyboard

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

    override fun setAlphabetKeyboard() { setKeyboard(KeyboardId.ELEMENT_ALPHABET, KeyboardSwitchState.OTHER) }
    override fun setAlphabetManualShiftedKeyboard() { setKeyboard(KeyboardId.ELEMENT_ALPHABET_MANUAL_SHIFTED, KeyboardSwitchState.OTHER) }
    override fun setAlphabetAutomaticShiftedKeyboard() { setKeyboard(KeyboardId.ELEMENT_ALPHABET_AUTOMATIC_SHIFTED, KeyboardSwitchState.OTHER) }
    override fun setAlphabetShiftLockedKeyboard() { setKeyboard(KeyboardId.ELEMENT_ALPHABET_SHIFT_LOCKED, KeyboardSwitchState.OTHER) }
    override fun setSymbolsKeyboard() { setKeyboard(KeyboardId.ELEMENT_SYMBOLS, KeyboardSwitchState.OTHER) }
    override fun setSymbolsShiftedKeyboard() { setKeyboard(KeyboardId.ELEMENT_SYMBOLS_SHIFTED, KeyboardSwitchState.SYMBOLS_SHIFTED) }

    override fun requestUpdatingShiftState(autoCapsFlags: Int, recapitalizeMode: Int) {
        mState.onUpdateShiftState(autoCapsFlags, recapitalizeMode)
    }

    override fun startDoubleTapShiftKeyTimer() { mKeyboardView?.startDoubleTapShiftKeyTimer() }
    override fun cancelDoubleTapShiftKeyTimer() { mKeyboardView?.cancelDoubleTapShiftKeyTimer() }
    override fun isInDoubleTapShiftKeyTimeout(): Boolean = mKeyboardView?.isInDoubleTapShiftKeyTimeout() ?: false

    fun onEvent(event: Event, currentAutoCapsState: Int, currentRecapitalizeState: Int) {
        mState.onEvent(event, currentAutoCapsState, currentRecapitalizeState)
    }

    fun isShowingKeyboardId(vararg keyboardIds: Int): Boolean {
        if (mKeyboardView == null || !mKeyboardView!!.isShown) return false
        val activeKeyboardId = mKeyboardView!!.keyboard?.mId?.mElementId ?: return false
        return keyboardIds.any { it == activeKeyboardId }
    }

    fun isShowingMoreKeysPanel(): Boolean = mKeyboardView?.isShowingMoreKeysPanel() ?: false
    fun getVisibleKeyboardView(): View? = mKeyboardView
    fun getMainKeyboardView(): MainKeyboardView? = mKeyboardView

    fun deallocateMemory() {
        mKeyboardView?.cancelAllOngoingEvents()
        mKeyboardView?.deallocateMemory()
    }

    fun onCreateInputView(): View {
        mKeyboardView?.closing()
        updateKeyboardThemeAndContextThemeWrapper(mLatinIME!!, KeyboardTheme.getKeyboardTheme(mLatinIME!!))
        val currentInputView = LayoutInflater.from(mThemeContext!!).inflate(R.layout.input_view, null) as InputView
        mKeyboardView = currentInputView.findViewById(R.id.keyboard_view)
        mKeyboardView!!.setKeyboardActionListener(mLatinIME!!)
        return currentInputView
    }

    enum class KeyboardSwitchState(val mKeyboardId: Int) {
        HIDDEN(-1),
        SYMBOLS_SHIFTED(KeyboardId.ELEMENT_SYMBOLS_SHIFTED),
        OTHER(-1)
    }

    fun getKeyboardSwitchState(): KeyboardSwitchState {
        val hidden = mKeyboardLayoutSet == null || mKeyboardView == null || !mKeyboardView!!.isShown
        return when {
            hidden -> KeyboardSwitchState.HIDDEN
            isShowingKeyboardId(KeyboardId.ELEMENT_SYMBOLS_SHIFTED) -> KeyboardSwitchState.SYMBOLS_SHIFTED
            else -> KeyboardSwitchState.OTHER
        }
    }

    companion object {
        private const val TAG = "KeyboardSwitcher"
        private val sInstance = KeyboardSwitcher()
        fun getInstance(): KeyboardSwitcher = sInstance
        fun init(latinIme: LatinIME) { sInstance.initInternal(latinIme) }
    }

    private fun initInternal(latinIme: LatinIME) {
        mLatinIME = latinIme
        mRichImm = RichInputMethodManager.getInstance()
        mState = KeyboardState(this)
    }
}
