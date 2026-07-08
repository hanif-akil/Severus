package rkr.simplekeyboard.inputmethod.latin.inputlogic

import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import rkr.simplekeyboard.inputmethod.event.Event
import rkr.simplekeyboard.inputmethod.event.InputTransaction
import rkr.simplekeyboard.inputmethod.keyboard.KeyboardSwitcher
import rkr.simplekeyboard.inputmethod.latin.LatinIME
import rkr.simplekeyboard.inputmethod.latin.RichInputConnection
import rkr.simplekeyboard.inputmethod.latin.RichInputMethodManager
import rkr.simplekeyboard.inputmethod.latin.common.Constants
import rkr.simplekeyboard.inputmethod.latin.settings.SettingsValues
import rkr.simplekeyboard.inputmethod.latin.utils.RecapitalizeStatus

class InputLogic(private val mLatinIME: LatinIME, private val mHandler: Any?) {
    private var mRichInputConnection: RichInputConnection = RichInputConnection(mLatinIME)
    private var mEditorInfo: EditorInfo? = null
    private var mInputTransaction: InputTransaction? = null

    fun startInput(editorInfo: EditorInfo, restarting: Boolean) {
        mEditorInfo = editorInfo
        mRichInputConnection.clearCaches()
        mRichInputConnection.reloadTextCache(editorInfo, restarting)
    }

    fun onStartInputView(restarting: Boolean) {
        val editorInfo = mEditorInfo ?: return
        mRichInputConnection.reloadTextCache(editorInfo, restarting)
    }

    fun onFinishInput() {
        mRichInputConnection.clearCaches()
    }

    fun onUpdateSelection(newSelStart: Int, newSelEnd: Int) {
        mRichInputConnection.updateSelection(newSelStart, newSelEnd)
    }

    fun onUpdateCursorAnchorInfo(cursorAnchorInfo: android.view.inputmethod.CursorAnchorInfo?) {
        // Handle cursor anchor info if needed
    }

    fun onCodeInput(code: Int, isKeyRepeat: Boolean, autoCapsFlags: Int, recapitalizeMode: Int) {
        val settingsValues = mLatinIME.settingsValues ?: return
        val event = if (code != Constants.CODE_UNSPECIFIED && code != Constants.NOT_A_CODE) {
            Event.createSoftwareKeypressEvent(code, code, isKeyRepeat)
        } else {
            return
        }
        processEvent(event, autoCapsFlags, recapitalizeMode)
    }

    fun onTextInput(text: String) {
        val settingsValues = mLatinIME.settingsValues ?: return
        mRichInputConnection.beginBatchEdit()
        mRichInputConnection.commitText(text, 1)
        mRichInputConnection.endBatchEdit()
    }

    fun processEvent(event: Event, autoCapsFlags: Int, recapitalizeMode: Int) {
        mInputTransaction = InputTransaction(mLatinIME.settingsValues!!)
        val keyboardSwitcher = KeyboardSwitcher.getInstance()
        keyboardSwitcher.onEvent(event, autoCapsFlags, recapitalizeMode)
        val textToCommit = event.getTextToCommit()
        if (textToCommit.isNotEmpty()) {
            mRichInputConnection.beginBatchEdit()
            mRichInputConnection.commitText(textToCommit, 1)
            mRichInputConnection.endBatchEdit()
        }
    }

    fun sendDownUpKeyEvents(keyCode: Int) {
        val connection = mRichInputConnection
        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val upEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        connection.sendKeyEvent(downEvent)
        connection.sendKeyEvent(upEvent)
    }
}
