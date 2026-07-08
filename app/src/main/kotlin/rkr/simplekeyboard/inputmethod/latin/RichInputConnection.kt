package rkr.simplekeyboard.inputmethod.latin

import android.annotation.TargetApi
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.SurroundingText
import rkr.simplekeyboard.inputmethod.latin.common.Constants
import rkr.simplekeyboard.inputmethod.latin.common.StringUtils
import rkr.simplekeyboard.inputmethod.latin.settings.SpacingAndPunctuations
import rkr.simplekeyboard.inputmethod.latin.utils.CapsModeUtils
import java.util.concurrent.Executors

class RichInputConnection(private val mLatinIME: LatinIME) {
    private var mIC: InputConnection? = null
    private var mNestLevel = 0
    private val mBackgroundThread = Executors.newSingleThreadExecutor()

    private var mExpectedSelStart = INVALID_CURSOR_POSITION
    private var mExpectedSelEnd = INVALID_CURSOR_POSITION
    private var mTextBeforeCursor = ""
    private var mTextAfterCursor = ""
    private var mTextSelection = ""

    fun isConnected(): Boolean = mIC != null

    fun beginBatchEdit() {
        if (++mNestLevel == 1) {
            mIC = mLatinIME.currentInputConnection
            if (isConnected()) mIC!!.beginBatchEdit()
        } else {
            Log.e(TAG, "Nest level too deep : $mNestLevel")
        }
    }

    fun endBatchEdit() {
        if (mNestLevel <= 0) Log.e(TAG, "Batch edit not in progress!")
        if (--mNestLevel == 0 && isConnected()) mIC!!.endBatchEdit()
    }

    fun updateSelection(newSelStart: Int, newSelEnd: Int) {
        mExpectedSelStart = newSelStart
        mExpectedSelEnd = newSelEnd
    }

    @TargetApi(Build.VERSION_CODES.S)
    private fun setTextAroundCursor(textAroundCursor: SurroundingText?) {
        if (textAroundCursor == null) {
            Log.e(TAG, "Unable get text around cursor.")
            mTextBeforeCursor = ""; mTextAfterCursor = ""; mTextSelection = ""
            return
        }
        val text = textAroundCursor.text
        mTextBeforeCursor = text.subSequence(0, textAroundCursor.selectionStart).toString()
        mTextSelection = text.subSequence(textAroundCursor.selectionStart, textAroundCursor.selectionEnd).toString()
        mTextAfterCursor = text.subSequence(textAroundCursor.selectionEnd, text.length).toString()
    }

    fun reloadTextCache(editorInfo: EditorInfo, restarting: Boolean) {
        mIC = mLatinIME.currentInputConnection
        if (mExpectedSelStart != INVALID_CURSOR_POSITION && mExpectedSelEnd != INVALID_CURSOR_POSITION && !restarting) return
        updateSelection(editorInfo.initialSelStart, editorInfo.initialSelEnd)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val textAroundCursor = editorInfo.getInitialSurroundingText(Constants.EDITOR_CONTENTS_CACHE_SIZE, Constants.EDITOR_CONTENTS_CACHE_SIZE, 0)
            setTextAroundCursor(textAroundCursor)
            mLatinIME.mHandler.postUpdateShiftState()
        } else {
            reloadTextCache()
        }
    }

    fun reloadTextCache() {
        mIC = mLatinIME.currentInputConnection
        if (!isConnected()) return
        val expectedSelStart = mExpectedSelStart
        val expectedSelEnd = mExpectedSelEnd
        mBackgroundThread.execute {
            if (!isConnected()) return@execute
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val textAroundCursor = mIC!!.getSurroundingText(Constants.EDITOR_CONTENTS_CACHE_SIZE, Constants.EDITOR_CONTENTS_CACHE_SIZE, 0)
                if (expectedSelStart != mExpectedSelStart || expectedSelEnd != mExpectedSelEnd) { Log.w(TAG, "Selection range modified before thread completion."); return@execute }
                setTextAroundCursor(textAroundCursor)
                mLatinIME.mHandler.postUpdateShiftState()
            } else {
                val textBeforeCursor = mIC!!.getTextBeforeCursor(Constants.EDITOR_CONTENTS_CACHE_SIZE, 0)
                if (expectedSelStart != mExpectedSelStart) { Log.w(TAG, "Selection start modified before thread completion."); return@execute }
                mTextBeforeCursor = textBeforeCursor?.toString() ?: ""
                mLatinIME.mHandler.postUpdateShiftState()
                val textAfterCursor = mIC!!.getTextAfterCursor(Constants.EDITOR_CONTENTS_CACHE_SIZE, 0)
                if (expectedSelEnd != mExpectedSelEnd) { Log.w(TAG, "Selection end modified before thread completion."); return@execute }
                mTextAfterCursor = textAfterCursor?.toString() ?: ""
                if (hasSelection()) {
                    val textSelection = mIC!!.getSelectedText(0)
                    if (expectedSelStart != mExpectedSelStart || expectedSelEnd != mExpectedSelEnd) { Log.w(TAG, "Selection range modified before thread completion."); return@execute }
                    mTextSelection = textSelection?.toString() ?: ""
                } else {
                    mTextSelection = ""
                }
            }
        }
    }

    fun clearCaches() {
        mExpectedSelStart = INVALID_CURSOR_POSITION; mExpectedSelEnd = INVALID_CURSOR_POSITION
        mTextBeforeCursor = ""; mTextSelection = ""; mTextAfterCursor = ""
    }

    fun commitText(text: CharSequence, newCursorPosition: Int) {
        RichInputMethodManager.getInstance().resetSubtypeCycleOrder()
        mTextBeforeCursor += text
        if (hasCursorPosition()) { mExpectedSelStart += text.length; mExpectedSelEnd = mExpectedSelStart }
        if (isConnected()) mIC!!.commitText(text, newCursorPosition)
    }

    fun getSelectedText(): CharSequence = mTextSelection
    fun canDeleteCharacters(): Boolean = mExpectedSelStart > 0

    fun getCursorCapsMode(inputType: Int, spacingAndPunctuations: SpacingAndPunctuations): Int {
        mIC = mLatinIME.currentInputConnection
        if (!isConnected()) return Constants.TextUtils.CAP_MODE_OFF
        return CapsModeUtils.getCapsMode(mTextBeforeCursor, inputType, spacingAndPunctuations)
    }

    fun getCodePointBeforeCursor(): Int {
        val length = mTextBeforeCursor.length
        if (length < 1) return Constants.NOT_A_CODE
        return Character.codePointBefore(mTextBeforeCursor, length)
    }

    fun replaceText(startPosition: Int, endPosition: Int, text: CharSequence) {
        if (mExpectedSelStart != mExpectedSelEnd) { Log.e(TAG, "replaceText called with text range selected"); return }
        if (mExpectedSelStart != startPosition) { Log.e(TAG, "replaceText called with range not starting with current cursor position"); return }
        val numCharsSelected = endPosition - startPosition
        val textAfterCursor = mTextAfterCursor
        if (textAfterCursor.length < numCharsSelected) { Log.e(TAG, "replaceText called with range longer than current text"); return }
        mTextAfterCursor = text.toString() + textAfterCursor.substring(numCharsSelected)
        RichInputMethodManager.getInstance().resetSubtypeCycleOrder()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            mIC!!.replaceText(startPosition, endPosition, text, 0, null)
        } else {
            mIC!!.deleteSurroundingText(0, numCharsSelected)
            mIC!!.commitText(text, 0)
        }
    }

    fun deleteTextBeforeCursor(numChars: Int) {
        val textBeforeCursor = mTextBeforeCursor
        if (textBeforeCursor.isNotEmpty() && textBeforeCursor.length >= numChars) {
            mTextBeforeCursor = textBeforeCursor.substring(0, textBeforeCursor.length - numChars)
        }
        if (mExpectedSelStart >= numChars) mExpectedSelStart -= numChars
        mIC!!.deleteSurroundingText(numChars, 0)
    }

    fun deleteSelectedText() {
        if (mExpectedSelStart == mExpectedSelEnd) { Log.e(TAG, "deleteSelectedText called with text range not selected"); return }
        beginBatchEdit()
        val selectionLength = mExpectedSelEnd - mExpectedSelStart
        mTextSelection = ""
        setSelection(mExpectedSelStart, mExpectedSelStart)
        mIC!!.deleteSurroundingText(0, selectionLength)
        endBatchEdit()
    }

    fun performEditorAction(actionId: Int) {
        mIC = mLatinIME.currentInputConnection
        if (isConnected()) mIC!!.performEditorAction(actionId)
    }

    fun pasteClipboard() {
        val clipboard = mLatinIME.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount == 1) {
                val mimeType = clipData.description.getMimeType(0)
                if (android.content.ClipDescription.MIMETYPE_TEXT_PLAIN == mimeType || android.content.ClipDescription.MIMETYPE_TEXT_HTML == mimeType) {
                    val pasteData = clipData.getItemAt(0).text
                    if (pasteData != null && pasteData.isNotEmpty()) { mLatinIME.onTextInput(pasteData.toString()); return }
                }
            }
        }
        mIC!!.performContextMenuAction(android.R.id.paste)
    }

    fun sendKeyEvent(keyEvent: KeyEvent) {
        RichInputMethodManager.getInstance().resetSubtypeCycleOrder()
        if (keyEvent.action == KeyEvent.ACTION_DOWN) {
            when (keyEvent.keyCode) {
                KeyEvent.KEYCODE_ENTER -> {
                    mTextBeforeCursor += "\n"
                    if (hasCursorPosition()) { mExpectedSelStart += 1; mExpectedSelEnd = mExpectedSelStart }
                }
                KeyEvent.KEYCODE_UNKNOWN -> {
                    if (keyEvent.characters != null) {
                        mTextBeforeCursor += keyEvent.characters
                        if (hasCursorPosition()) { mExpectedSelStart += keyEvent.characters!!.length; mExpectedSelEnd = mExpectedSelStart }
                    }
                }
                KeyEvent.KEYCODE_DEL -> {}
                else -> {
                    val text = StringUtils.newSingleCodePointString(keyEvent.unicodeChar)
                    mTextBeforeCursor += text
                    if (hasCursorPosition()) { mExpectedSelStart += text.length; mExpectedSelEnd = mExpectedSelStart }
                }
            }
        }
        if (isConnected()) mIC!!.sendKeyEvent(keyEvent)
    }

    fun setSelection(start: Int, end: Int) {
        if (start < 0 || end < 0 || start > end) return
        if (mExpectedSelStart == start && mExpectedSelEnd == end) return
        val textStart = mExpectedSelStart - mTextBeforeCursor.length
        val textRange = mTextBeforeCursor + mTextSelection + mTextAfterCursor
        if (textRange.length >= end - textStart && start - textStart >= 0 && textStart >= 0) {
            mTextBeforeCursor = textRange.substring(0, start - textStart)
            mTextSelection = textRange.substring(start - textStart, end - textStart)
            mTextAfterCursor = textRange.substring(end - textStart)
        }
        RichInputMethodManager.getInstance().resetSubtypeCycleOrder()
        mExpectedSelStart = start; mExpectedSelEnd = end
        if (isConnected()) mIC!!.setSelection(start, end)
    }

    fun getExpectedSelectionStart(): Int = mExpectedSelStart
    fun getExpectedSelectionEnd(): Int = mExpectedSelEnd
    fun hasSelection(): Boolean = mExpectedSelEnd != mExpectedSelStart
    fun hasCursorPosition(): Boolean = mExpectedSelStart != INVALID_CURSOR_POSITION && mExpectedSelEnd != INVALID_CURSOR_POSITION

    fun getUnicodeSteps(chars: Int, rightSidePointer: Boolean): Int {
        var steps = 0
        var charsRemaining = chars
        if (charsRemaining < 0) {
            val charsBeforeCursor = if (rightSidePointer && hasSelection()) getSelectedText() else mTextBeforeCursor
            if (charsBeforeCursor.isNullOrEmpty()) return charsRemaining
            var i = charsBeforeCursor.length - 1
            while (i >= 0 && charsRemaining < 0) {
                if (i > 1 && charsBeforeCursor[i - 1] == '\u200d') { i--; continue }
                if (charsBeforeCursor[i] == '\u200d') { i--; continue }
                if (Character.isSurrogate(charsBeforeCursor[i]) && !Character.isHighSurrogate(charsBeforeCursor[i])) { i--; continue }
                charsRemaining++; steps--; i--
            }
        } else if (charsRemaining > 0) {
            val charsAfterCursor = if (!rightSidePointer && hasSelection()) getSelectedText() else mTextAfterCursor
            if (charsAfterCursor.isNullOrEmpty()) return charsRemaining
            var i = 0
            while (i < charsAfterCursor.length && charsRemaining > 0) {
                if (i < charsAfterCursor.length - 1 && charsAfterCursor[i + 1] == '\u200d') { i++; continue }
                if (charsAfterCursor[i] == '\u200d') { i++; continue }
                if (Character.isHighSurrogate(charsAfterCursor[i])) { i++; continue }
                charsRemaining--; steps++; i++
            }
        }
        return steps
    }

    companion object {
        private const val TAG = "RichInputConnection"
        private const val INVALID_CURSOR_POSITION = -1
    }
}
