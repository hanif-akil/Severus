/*
 * Copyright (C) 2013 The Android Open Source Project
 * Copyright (C) 2025 Raimondas Rimkus
 * Copyright (C) 2025 Camille019
 * Copyright (C) 2023 Md. Rifat Hasan Jihan
 * Copyright (C) 2021 wittmane
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

package rkr.simplekeyboard.inputmethod.latin.inputlogic

import android.os.SystemClock
import android.text.TextUtils
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import rkr.simplekeyboard.inputmethod.event.Event
import rkr.simplekeyboard.inputmethod.event.InputTransaction
import rkr.simplekeyboard.inputmethod.latin.AvroPhoneticConverter
import rkr.simplekeyboard.inputmethod.latin.LatinIME
import rkr.simplekeyboard.inputmethod.latin.RichInputConnection
import rkr.simplekeyboard.inputmethod.latin.RichInputMethodManager
import rkr.simplekeyboard.inputmethod.latin.common.Constants
import rkr.simplekeyboard.inputmethod.latin.common.StringUtils
import rkr.simplekeyboard.inputmethod.latin.settings.SettingsValues
import rkr.simplekeyboard.inputmethod.latin.utils.InputTypeUtils
import rkr.simplekeyboard.inputmethod.latin.utils.RecapitalizeStatus
import rkr.simplekeyboard.inputmethod.latin.utils.SubtypeLocaleUtils

class InputLogic(val mLatinIME: LatinIME) {

    val mConnection: RichInputConnection = RichInputConnection(mLatinIME)
    private val mRecapitalizeStatus = RecapitalizeStatus()

    fun startInput() {
        mRecapitalizeStatus.disable()
    }

    fun clearCaches() {
        mConnection.clearCaches()
    }

    fun onSubtypeChanged() {
        startInput()
    }

    fun onTextInput(settingsValues: SettingsValues, event: Event): InputTransaction {
        val rawText = event.getTextToCommit().toString()
        val inputTransaction = InputTransaction(settingsValues)
        val text = performSpecificTldProcessingOnTextInput(rawText)
        mConnection.commitText(text, 1)
        inputTransaction.requireShiftUpdate(InputTransaction.SHIFT_UPDATE_NOW)
        return inputTransaction
    }

    fun onUpdateSelection(newSelStart: Int, newSelEnd: Int) {
        mConnection.updateSelection(newSelStart, newSelEnd)
    }

    fun reloadTextCache() {
        mConnection.reloadTextCache()

        mRecapitalizeStatus.enable()
        mRecapitalizeStatus.stop()
    }

    fun onCodeInput(settingsValues: SettingsValues, event: Event): InputTransaction {
        val inputTransaction = InputTransaction(settingsValues)

        var currentEvent: Event? = event
        while (currentEvent != null) {
            if (currentEvent.isConsumed()) {
                handleConsumedEvent(currentEvent)
            } else if (currentEvent.isFunctionalKeyEvent()) {
                handleFunctionalEvent(currentEvent, inputTransaction)
            } else {
                handleNonFunctionalEvent(currentEvent, inputTransaction)
            }
            currentEvent = currentEvent.mNextEvent
        }
        return inputTransaction
    }

    private fun handleConsumedEvent(event: Event) {
        val textToCommit = event.getTextToCommit()
        if (!TextUtils.isEmpty(textToCommit)) {
            mConnection.commitText(textToCommit, 1)
        }
    }

    private fun handleFunctionalEvent(event: Event, inputTransaction: InputTransaction) {
        when (event.mKeyCode) {
            Constants.CODE_DELETE -> handleBackspaceEvent(event, inputTransaction)
            Constants.CODE_SHIFT -> {
                performRecapitalization()
                inputTransaction.requireShiftUpdate(InputTransaction.SHIFT_UPDATE_NOW)
            }
            Constants.CODE_CAPSLOCK -> { /* handled in KeyboardSwitcher */ }
            Constants.CODE_SYMBOL_SHIFT -> { /* handled in onPressKey/onReleaseKey */ }
            Constants.CODE_SWITCH_ALPHA_SYMBOL -> { /* handled in onPressKey/onReleaseKey */ }
            Constants.CODE_SETTINGS -> onSettingsKeyPressed()
            Constants.CODE_PASTE -> mConnection.pasteClipboard()
            Constants.CODE_COPY -> mConnection.copySelectedText()
            Constants.CODE_CUT -> mConnection.cutSelectedText()
            Constants.CODE_ACTION_NEXT -> performEditorAction(EditorInfo.IME_ACTION_NEXT)
            Constants.CODE_ACTION_PREVIOUS -> performEditorAction(EditorInfo.IME_ACTION_PREVIOUS)
            Constants.CODE_LANGUAGE_SWITCH -> handleLanguageSwitchKey()
            Constants.CODE_SHIFT_ENTER -> sendDownUpKeyEvent(KeyEvent.KEYCODE_ENTER, KeyEvent.META_SHIFT_ON)
            Constants.CODE_CLIPBOARD -> onClipboardKeyPressed()
            Constants.CODE_NUMPAD -> onNumPadKeyPressed()
            else -> throw RuntimeException("Unknown key code : ${event.mKeyCode}")
        }
    }

    private fun handleNonFunctionalEvent(event: Event, inputTransaction: InputTransaction) {
        when (event.mCodePoint) {
            Constants.CODE_ENTER -> {
                val editorInfo = getCurrentInputEditorInfo()
                val imeOptionsActionId = InputTypeUtils.getImeOptionsActionIdFromEditorInfo(editorInfo)
                if (InputTypeUtils.IME_ACTION_CUSTOM_LABEL == imeOptionsActionId) {
                    performEditorAction(editorInfo.actionId)
                } else if (EditorInfo.IME_ACTION_NONE != imeOptionsActionId) {
                    performEditorAction(imeOptionsActionId)
                } else {
                    handleNonSpecialCharacterEvent(event, inputTransaction)
                }
            }
            else -> handleNonSpecialCharacterEvent(event, inputTransaction)
        }
    }

    private fun handleNonSpecialCharacterEvent(event: Event, inputTransaction: InputTransaction) {
        val codePoint = event.mCodePoint
        if (inputTransaction.mSettingsValues.isWordSeparator(codePoint)
            || Character.getType(codePoint) == Character.OTHER_SYMBOL
        ) {
            handleSeparatorEvent(event, inputTransaction)
        } else {
            handleNonSeparatorEvent(event)
        }
    }

    private fun handleNonSeparatorEvent(event: Event) {
        if (isAvroPhoneticLayout()) {
            handleAvroPhoneticInput(event)
        } else {
            sendKeyCodePoint(event.mCodePoint)
        }
    }

    private fun handleSeparatorEvent(event: Event, inputTransaction: InputTransaction) {
        sendKeyCodePoint(event.mCodePoint)
        inputTransaction.requireShiftUpdate(InputTransaction.SHIFT_UPDATE_NOW)
    }

    private fun handleBackspaceEvent(event: Event, inputTransaction: InputTransaction) {
        val shiftUpdateKind =
            if (event.isKeyRepeat() && mConnection.getExpectedSelectionStart() > 0) {
                InputTransaction.SHIFT_UPDATE_LATER
            } else {
                InputTransaction.SHIFT_UPDATE_NOW
            }
        inputTransaction.requireShiftUpdate(shiftUpdateKind)

        if (mConnection.hasSelection()) {
            mConnection.deleteSelectedText()
        } else {
            val codePointBeforeCursor = mConnection.getCodePointBeforeCursor()
            if (codePointBeforeCursor == Constants.NOT_A_CODE) {
                sendDownUpKeyEvent(KeyEvent.KEYCODE_DEL)
            } else {
                val numChars = if (Character.isSupplementaryCodePoint(codePointBeforeCursor)) 2 else 1
                mConnection.deleteTextBeforeCursor(numChars)
            }
        }
    }

    private fun handleLanguageSwitchKey() {
        mLatinIME.switchToNextSubtype()
    }

    private fun performRecapitalization() {
        if (!mConnection.hasSelection() || !mRecapitalizeStatus.mIsEnabled()) {
            return
        }
        val selectionStart = mConnection.getExpectedSelectionStart()
        val selectionEnd = mConnection.getExpectedSelectionEnd()
        val numCharsSelected = selectionEnd - selectionStart
        if (numCharsSelected > Constants.MAX_CHARACTERS_FOR_RECAPITALIZATION) {
            return
        }
        if (!mRecapitalizeStatus.isStarted()
            || !mRecapitalizeStatus.isSetAt(selectionStart, selectionEnd)
        ) {
            val selectedText = mConnection.getSelectedText()
            if (TextUtils.isEmpty(selectedText)) return
            mRecapitalizeStatus.start(
                selectionStart, selectionEnd, selectedText.toString(),
                mLatinIME.getCurrentLayoutLocale()
            )
            mRecapitalizeStatus.trim()
        }
        mConnection.beginBatchEdit()
        mConnection.setSelection(selectionStart, selectionStart)
        mRecapitalizeStatus.rotate()
        mConnection.replaceText(selectionStart, selectionEnd, mRecapitalizeStatus.getRecapitalizedString())
        mConnection.setSelection(mRecapitalizeStatus.getNewCursorStart(), mRecapitalizeStatus.getNewCursorEnd())
        mConnection.endBatchEdit()
    }

    fun getCurrentAutoCapsState(settingsValues: SettingsValues, layoutSetName: String): Int {
        if (!settingsValues.mAutoCap || !layoutUsesAutoCaps(layoutSetName)) {
            return Constants.TextUtils.CAP_MODE_OFF
        }

        val ei = getCurrentInputEditorInfo() ?: return Constants.TextUtils.CAP_MODE_OFF
        val inputType = ei.inputType
        return mConnection.getCursorCapsMode(inputType, settingsValues.mSpacingAndPunctuations)
    }

    private fun layoutUsesAutoCaps(layoutSetName: String): Boolean {
        return when (layoutSetName) {
            SubtypeLocaleUtils.LAYOUT_ARABIC,
            SubtypeLocaleUtils.LAYOUT_BENGALI,
            SubtypeLocaleUtils.LAYOUT_BENGALI_AKKHOR,
            SubtypeLocaleUtils.LAYOUT_BENGALI_UNIJOY,
            SubtypeLocaleUtils.LAYOUT_FARSI,
            SubtypeLocaleUtils.LAYOUT_GEORGIAN,
            SubtypeLocaleUtils.LAYOUT_HEBREW,
            SubtypeLocaleUtils.LAYOUT_HINDI,
            SubtypeLocaleUtils.LAYOUT_HINDI_COMPACT,
            SubtypeLocaleUtils.LAYOUT_KANNADA,
            SubtypeLocaleUtils.LAYOUT_KHMER,
            SubtypeLocaleUtils.LAYOUT_LAO,
            SubtypeLocaleUtils.LAYOUT_MALAYALAM,
            SubtypeLocaleUtils.LAYOUT_MARATHI,
            SubtypeLocaleUtils.LAYOUT_NEPALI_ROMANIZED,
            SubtypeLocaleUtils.LAYOUT_NEPALI_TRADITIONAL,
            SubtypeLocaleUtils.LAYOUT_TAMIL,
            SubtypeLocaleUtils.LAYOUT_TELUGU,
            SubtypeLocaleUtils.LAYOUT_THAI,
            SubtypeLocaleUtils.LAYOUT_URDU -> false
            else -> true
        }
    }

    fun getCurrentRecapitalizeState(): Int {
        if (!mRecapitalizeStatus.isStarted()
            || !mRecapitalizeStatus.isSetAt(
                mConnection.getExpectedSelectionStart(),
                mConnection.getExpectedSelectionEnd()
            )
        ) {
            return RecapitalizeStatus.NOT_A_RECAPITALIZE_MODE
        }
        return mRecapitalizeStatus.getCurrentMode()
    }

    private fun getCurrentInputEditorInfo(): EditorInfo? {
        return mLatinIME.currentInputEditorInfo
    }

    private fun performEditorAction(actionId: Int) {
        mConnection.performEditorAction(actionId)
    }

    private fun performSpecificTldProcessingOnTextInput(text: String): String {
        if (text.length <= 1 || text[0] != Constants.CODE_PERIOD
            || !Character.isLetter(text[1])
        ) {
            return text
        }
        val codePointBeforeCursor = mConnection.getCodePointBeforeCursor()
        if (Constants.CODE_PERIOD == codePointBeforeCursor) {
            return text.substring(1)
        }
        return text
    }

    private fun onSettingsKeyPressed() {
        mLatinIME.launchSettings()
    }

    private fun onClipboardKeyPressed() {
        mLatinIME.showClipboardManager()
    }

    private fun onNumPadKeyPressed() {
        mLatinIME.toggleNumPad()
    }

    @JvmOverloads
    fun sendDownUpKeyEvent(keyCode: Int, metaState: Int = 0) {
        val eventTime = SystemClock.uptimeMillis()
        mConnection.sendKeyEvent(
            KeyEvent(
                eventTime, eventTime,
                KeyEvent.ACTION_DOWN, keyCode, 0, metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
            )
        )
        mConnection.sendKeyEvent(
            KeyEvent(
                SystemClock.uptimeMillis(), eventTime,
                KeyEvent.ACTION_UP, keyCode, 0, metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
            )
        )
    }

    private fun sendKeyCodePoint(codePoint: Int) {
        if (codePoint in '0'.code..'9'.code) {
            sendDownUpKeyEvent(codePoint - '0'.code + KeyEvent.KEYCODE_0)
            return
        }

        mConnection.commitText(StringUtils.newSingleCodePointString(codePoint), 1)
    }

    private fun isAvroPhoneticLayout(): Boolean {
        val layoutSet = RichInputMethodManager.getInstance().currentSubtype.keyboardLayoutSet
        return SubtypeLocaleUtils.LAYOUT_BENGALI_AVRO == layoutSet
    }

    private fun handleAvroPhoneticInput(event: Event) {
        val codePoint = event.mCodePoint
        val c = codePoint.toChar()

        if (codePoint in 'a'.code..'z'.code) {
            val roman = c.toString()
            val bengali = AvroPhoneticConverter.convert(roman)
            if (bengali != null && bengali.isNotEmpty()) {
                mConnection.commitText(bengali, 1)
            } else {
                sendKeyCodePoint(codePoint)
            }
        } else if (codePoint in 'A'.code..'Z'.code) {
            val roman = Character.toLowerCase(c).toString()
            val bengali = AvroPhoneticConverter.convert(roman)
            if (bengali != null && bengali.isNotEmpty()) {
                mConnection.commitText(bengali, 1)
            } else {
                sendKeyCodePoint(codePoint)
            }
        } else {
            sendKeyCodePoint(codePoint)
        }
    }
}
