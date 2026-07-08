package rkr.simplekeyboard.inputmethod.keyboard.internal

import android.text.TextUtils
import android.util.Log
import rkr.simplekeyboard.inputmethod.event.Event
import rkr.simplekeyboard.inputmethod.latin.common.Constants
import rkr.simplekeyboard.inputmethod.latin.utils.CapsModeUtils
import rkr.simplekeyboard.inputmethod.latin.utils.RecapitalizeStatus

class KeyboardState(private val mSwitchActions: SwitchActions) {
    interface SwitchActions {
        fun setAlphabetKeyboard()
        fun setAlphabetManualShiftedKeyboard()
        fun setAlphabetAutomaticShiftedKeyboard()
        fun setAlphabetShiftLockedKeyboard()
        fun setSymbolsKeyboard()
        fun setSymbolsShiftedKeyboard()
        fun requestUpdatingShiftState(autoCapsFlags: Int, recapitalizeMode: Int)
        fun startDoubleTapShiftKeyTimer()
        fun isInDoubleTapShiftKeyTimeout(): Boolean
        fun cancelDoubleTapShiftKeyTimer()
    }

    private val mShiftKeyState = ShiftKeyState("Shift")
    private val mSymbolKeyState = ModifierKeyState("Symbol")
    private var mSwitchState = SWITCH_STATE_ALPHA
    private var mIsAlphabetMode = false
    private val mAlphabetShiftState = AlphabetShiftState()
    private var mIsSymbolShifted = false
    private var mPrevMainKeyboardWasShiftLocked = false
    private var mPrevSymbolsKeyboardWasShifted = false
    private var mRecapitalizeMode = RecapitalizeStatus.NOT_A_RECAPITALIZE_MODE
    private var mIsInAlphabetUnshiftedFromShifted = false
    private var mIsInDoubleTapShiftKey = false

    fun onLoadKeyboard(autoCapsFlags: Int, recapitalizeMode: Int) {
        if (DEBUG_EVENT) Log.d(TAG, "onLoadKeyboard: ${stateToString(autoCapsFlags, recapitalizeMode)}")
        mAlphabetShiftState.setShiftLocked(false)
        mPrevMainKeyboardWasShiftLocked = false
        mPrevSymbolsKeyboardWasShifted = false
        mShiftKeyState.onRelease()
        mSymbolKeyState.onRelease()
        setAlphabetKeyboard(autoCapsFlags, recapitalizeMode)
    }

    private fun setShifted(shiftMode: Int) {
        if (DEBUG_INTERNAL_ACTION) Log.d(TAG, "setShifted: shiftMode=${shiftModeToString(shiftMode)} $this")
        if (!mIsAlphabetMode) return
        val prevShiftMode = when {
            mAlphabetShiftState.isAutomaticShifted() -> AUTOMATIC_SHIFT
            mAlphabetShiftState.isManualShifted() -> MANUAL_SHIFT
            else -> UNSHIFT
        }
        when (shiftMode) {
            AUTOMATIC_SHIFT -> {
                mAlphabetShiftState.setAutomaticShifted()
                if (shiftMode != prevShiftMode) mSwitchActions.setAlphabetAutomaticShiftedKeyboard()
            }
            MANUAL_SHIFT -> {
                mAlphabetShiftState.setShifted(true)
                if (shiftMode != prevShiftMode) mSwitchActions.setAlphabetManualShiftedKeyboard()
            }
            UNSHIFT -> {
                mAlphabetShiftState.setShifted(false)
                if (shiftMode != prevShiftMode) mSwitchActions.setAlphabetKeyboard()
            }
            SHIFT_LOCK_SHIFTED -> mAlphabetShiftState.setShifted(true)
        }
    }

    private fun setShiftLocked(shiftLocked: Boolean) {
        if (DEBUG_INTERNAL_ACTION) Log.d(TAG, "setShiftLocked: shiftLocked=$shiftLocked $this")
        if (!mIsAlphabetMode) return
        if (shiftLocked && (!mAlphabetShiftState.isShiftLocked() || mAlphabetShiftState.isShiftLockShifted())) {
            mSwitchActions.setAlphabetShiftLockedKeyboard()
        }
        if (!shiftLocked && mAlphabetShiftState.isShiftLocked()) {
            mSwitchActions.setAlphabetKeyboard()
        }
        mAlphabetShiftState.setShiftLocked(shiftLocked)
    }

    private fun toggleAlphabetAndSymbols(autoCapsFlags: Int, recapitalizeMode: Int) {
        if (DEBUG_INTERNAL_ACTION) Log.d(TAG, "toggleAlphabetAndSymbols: ${stateToString(autoCapsFlags, recapitalizeMode)}")
        if (mIsAlphabetMode) {
            mPrevMainKeyboardWasShiftLocked = mAlphabetShiftState.isShiftLocked()
            if (mPrevSymbolsKeyboardWasShifted) setSymbolsShiftedKeyboard() else setSymbolsKeyboard()
            mPrevSymbolsKeyboardWasShifted = false
        } else {
            mPrevSymbolsKeyboardWasShifted = mIsSymbolShifted
            setAlphabetKeyboard(autoCapsFlags, recapitalizeMode)
            if (mPrevMainKeyboardWasShiftLocked) setShiftLocked(true)
            mPrevMainKeyboardWasShiftLocked = false
        }
    }

    private fun resetKeyboardStateToAlphabet(autoCapsFlags: Int, recapitalizeMode: Int) {
        if (DEBUG_INTERNAL_ACTION) Log.d(TAG, "resetKeyboardStateToAlphabet: ${stateToString(autoCapsFlags, recapitalizeMode)}")
        if (mIsAlphabetMode) return
        mPrevSymbolsKeyboardWasShifted = mIsSymbolShifted
        setAlphabetKeyboard(autoCapsFlags, recapitalizeMode)
        if (mPrevMainKeyboardWasShiftLocked) setShiftLocked(true)
        mPrevMainKeyboardWasShiftLocked = false
    }

    private fun toggleShiftInSymbols() {
        if (mIsSymbolShifted) setSymbolsKeyboard() else setSymbolsShiftedKeyboard()
    }

    private fun setAlphabetKeyboard(autoCapsFlags: Int, recapitalizeMode: Int) {
        if (DEBUG_INTERNAL_ACTION) Log.d(TAG, "setAlphabetKeyboard: ${stateToString(autoCapsFlags, recapitalizeMode)}")
        mSwitchActions.setAlphabetKeyboard()
        mIsAlphabetMode = true
        mIsSymbolShifted = false
        mRecapitalizeMode = RecapitalizeStatus.NOT_A_RECAPITALIZE_MODE
        mSwitchState = SWITCH_STATE_ALPHA
        mSwitchActions.requestUpdatingShiftState(autoCapsFlags, recapitalizeMode)
    }

    private fun setSymbolsKeyboard() {
        if (DEBUG_INTERNAL_ACTION) Log.d(TAG, "setSymbolsKeyboard")
        mSwitchActions.setSymbolsKeyboard()
        mIsAlphabetMode = false
        mIsSymbolShifted = false
        mRecapitalizeMode = RecapitalizeStatus.NOT_A_RECAPITALIZE_MODE
        mAlphabetShiftState.setShiftLocked(false)
        mSwitchState = SWITCH_STATE_SYMBOL_BEGIN
    }

    private fun setSymbolsShiftedKeyboard() {
        if (DEBUG_INTERNAL_ACTION) Log.d(TAG, "setSymbolsShiftedKeyboard")
        mSwitchActions.setSymbolsShiftedKeyboard()
        mIsAlphabetMode = false
        mIsSymbolShifted = true
        mRecapitalizeMode = RecapitalizeStatus.NOT_A_RECAPITALIZE_MODE
        mAlphabetShiftState.setShiftLocked(false)
        mSwitchState = SWITCH_STATE_SYMBOL_BEGIN
    }

    fun onPressKey(code: Int, isSinglePointer: Boolean, autoCapsFlags: Int, recapitalizeMode: Int) {
        if (DEBUG_EVENT) Log.d(TAG, "onPressKey: code=${Constants.printableCode(code)} single=$isSinglePointer ${stateToString(autoCapsFlags, recapitalizeMode)}")
        if (code != Constants.CODE_SHIFT) mSwitchActions.cancelDoubleTapShiftKeyTimer()
        when (code) {
            Constants.CODE_SHIFT -> onPressShift()
            Constants.CODE_CAPSLOCK -> { /* See onReleaseKey */ }
            Constants.CODE_SWITCH_ALPHA_SYMBOL -> onPressSymbol(autoCapsFlags, recapitalizeMode)
            else -> {
                mShiftKeyState.onOtherKeyPressed()
                mSymbolKeyState.onOtherKeyPressed()
                if (!isSinglePointer && mIsAlphabetMode && autoCapsFlags != TextUtils.CAP_MODE_CHARACTERS) {
                    val needsToResetAutoCaps = (mAlphabetShiftState.isAutomaticShifted() && !mShiftKeyState.isChording()) ||
                            (mAlphabetShiftState.isManualShifted() && mShiftKeyState.isReleasing())
                    if (needsToResetAutoCaps) mSwitchActions.setAlphabetKeyboard()
                }
            }
        }
    }

    fun onReleaseKey(code: Int, withSliding: Boolean, autoCapsFlags: Int, recapitalizeMode: Int) {
        if (DEBUG_EVENT) Log.d(TAG, "onReleaseKey: code=${Constants.printableCode(code)} sliding=$withSliding ${stateToString(autoCapsFlags, recapitalizeMode)}")
        when (code) {
            Constants.CODE_SHIFT -> onReleaseShift(withSliding, autoCapsFlags, recapitalizeMode)
            Constants.CODE_CAPSLOCK -> setShiftLocked(!mAlphabetShiftState.isShiftLocked())
            Constants.CODE_SWITCH_ALPHA_SYMBOL -> onReleaseSymbol(withSliding, autoCapsFlags, recapitalizeMode)
        }
    }

    private fun onPressSymbol(autoCapsFlags: Int, recapitalizeMode: Int) {
        toggleAlphabetAndSymbols(autoCapsFlags, recapitalizeMode)
        mSymbolKeyState.onPress()
        mSwitchState = SWITCH_STATE_MOMENTARY_ALPHA_AND_SYMBOL
    }

    private fun onReleaseSymbol(withSliding: Boolean, autoCapsFlags: Int, recapitalizeMode: Int) {
        if (mSymbolKeyState.isChording()) {
            toggleAlphabetAndSymbols(autoCapsFlags, recapitalizeMode)
        } else if (!withSliding) {
            mPrevSymbolsKeyboardWasShifted = false
        }
        mSymbolKeyState.onRelease()
    }

    fun onUpdateShiftState(autoCapsFlags: Int, recapitalizeMode: Int) {
        if (DEBUG_EVENT) Log.d(TAG, "onUpdateShiftState: ${stateToString(autoCapsFlags, recapitalizeMode)}")
        mRecapitalizeMode = recapitalizeMode
        updateAlphabetShiftState(autoCapsFlags, recapitalizeMode)
    }

    fun onResetKeyboardStateToAlphabet(autoCapsFlags: Int, recapitalizeMode: Int) {
        if (DEBUG_EVENT) Log.d(TAG, "onResetKeyboardStateToAlphabet: ${stateToString(autoCapsFlags, recapitalizeMode)}")
        resetKeyboardStateToAlphabet(autoCapsFlags, recapitalizeMode)
    }

    private fun updateShiftStateForRecapitalize(recapitalizeMode: Int) {
        when (recapitalizeMode) {
            RecapitalizeStatus.CAPS_MODE_ALL_UPPER -> setShifted(SHIFT_LOCK_SHIFTED)
            RecapitalizeStatus.CAPS_MODE_FIRST_WORD_UPPER -> setShifted(AUTOMATIC_SHIFT)
            else -> setShifted(UNSHIFT)
        }
    }

    private fun updateAlphabetShiftState(autoCapsFlags: Int, recapitalizeMode: Int) {
        if (!mIsAlphabetMode) return
        if (RecapitalizeStatus.NOT_A_RECAPITALIZE_MODE != recapitalizeMode) {
            updateShiftStateForRecapitalize(recapitalizeMode)
            return
        }
        if (!mShiftKeyState.isReleasing()) return
        if (!mAlphabetShiftState.isShiftLocked() && !mShiftKeyState.isIgnoring()) {
            if (mShiftKeyState.isReleasing() && autoCapsFlags != Constants.TextUtils.CAP_MODE_OFF) {
                setShifted(AUTOMATIC_SHIFT)
            } else {
                setShifted(if (mShiftKeyState.isChording()) MANUAL_SHIFT else UNSHIFT)
            }
        }
    }

    private fun onPressShift() {
        if (RecapitalizeStatus.NOT_A_RECAPITALIZE_MODE != mRecapitalizeMode) return
        if (mIsAlphabetMode) {
            mIsInDoubleTapShiftKey = mSwitchActions.isInDoubleTapShiftKeyTimeout()
            if (!mIsInDoubleTapShiftKey) mSwitchActions.startDoubleTapShiftKeyTimer()
            if (mIsInDoubleTapShiftKey) {
                if (mAlphabetShiftState.isManualShifted() || mIsInAlphabetUnshiftedFromShifted) {
                    setShiftLocked(true)
                }
            } else {
                if (mAlphabetShiftState.isShiftLocked()) {
                    setShifted(SHIFT_LOCK_SHIFTED)
                    mShiftKeyState.onPress()
                } else if (mAlphabetShiftState.isAutomaticShifted()) {
                    mShiftKeyState.onPress()
                } else if (mAlphabetShiftState.isShiftedOrShiftLocked()) {
                    mShiftKeyState.onPressOnShifted()
                } else {
                    setShifted(MANUAL_SHIFT)
                    mShiftKeyState.onPress()
                }
            }
        } else {
            toggleShiftInSymbols()
            mSwitchState = SWITCH_STATE_MOMENTARY_SYMBOL_AND_MORE
            mShiftKeyState.onPress()
        }
    }

    private fun onReleaseShift(withSliding: Boolean, autoCapsFlags: Int, recapitalizeMode: Int) {
        if (RecapitalizeStatus.NOT_A_RECAPITALIZE_MODE != mRecapitalizeMode) {
            updateShiftStateForRecapitalize(mRecapitalizeMode)
        } else if (mIsAlphabetMode) {
            val isShiftLocked = mAlphabetShiftState.isShiftLocked()
            mIsInAlphabetUnshiftedFromShifted = false
            if (mIsInDoubleTapShiftKey) {
                mIsInDoubleTapShiftKey = false
            } else if (mShiftKeyState.isChording()) {
                if (mAlphabetShiftState.isShiftLockShifted()) setShiftLocked(true) else setShifted(UNSHIFT)
                mShiftKeyState.onRelease()
                mSwitchActions.requestUpdatingShiftState(autoCapsFlags, recapitalizeMode)
                return
            } else if (isShiftLocked && !mAlphabetShiftState.isShiftLockShifted() &&
                (mShiftKeyState.isPressing() || mShiftKeyState.isPressingOnShifted()) && !withSliding) {
                // Ignore
            } else if (isShiftLocked && !mShiftKeyState.isIgnoring() && !withSliding) {
                setShiftLocked(false)
            } else if (mAlphabetShiftState.isShiftedOrShiftLocked() && mShiftKeyState.isPressingOnShifted() && !withSliding) {
                setShifted(UNSHIFT)
                mIsInAlphabetUnshiftedFromShifted = true
            } else if (mAlphabetShiftState.isAutomaticShifted() && mShiftKeyState.isPressing() && !withSliding) {
                setShifted(UNSHIFT)
                mIsInAlphabetUnshiftedFromShifted = true
            }
        } else {
            if (mShiftKeyState.isChording()) toggleShiftInSymbols()
        }
        mShiftKeyState.onRelease()
    }

    fun onFinishSlidingInput(autoCapsFlags: Int, recapitalizeMode: Int) {
        if (DEBUG_EVENT) Log.d(TAG, "onFinishSlidingInput: ${stateToString(autoCapsFlags, recapitalizeMode)}")
        when (mSwitchState) {
            SWITCH_STATE_MOMENTARY_ALPHA_AND_SYMBOL -> toggleAlphabetAndSymbols(autoCapsFlags, recapitalizeMode)
            SWITCH_STATE_MOMENTARY_SYMBOL_AND_MORE -> toggleShiftInSymbols()
        }
    }

    fun onEvent(event: Event, autoCapsFlags: Int, recapitalizeMode: Int) {
        val code = if (event.isFunctionalKeyEvent()) event.mKeyCode else event.mCodePoint
        if (DEBUG_EVENT) Log.d(TAG, "onEvent: code=${Constants.printableCode(code)} ${stateToString(autoCapsFlags, recapitalizeMode)}")
        when (mSwitchState) {
            SWITCH_STATE_MOMENTARY_ALPHA_AND_SYMBOL -> {
                if (code == Constants.CODE_SWITCH_ALPHA_SYMBOL) {
                    mSwitchState = if (mIsAlphabetMode) SWITCH_STATE_ALPHA else SWITCH_STATE_SYMBOL_BEGIN
                }
            }
            SWITCH_STATE_MOMENTARY_SYMBOL_AND_MORE -> {
                if (code == Constants.CODE_SHIFT) mSwitchState = SWITCH_STATE_SYMBOL_BEGIN
            }
            SWITCH_STATE_SYMBOL_BEGIN -> {
                if (!isSpaceOrEnter(code) && (Constants.isLetterCode(code) || code == Constants.CODE_OUTPUT_TEXT)) {
                    mSwitchState = SWITCH_STATE_SYMBOL
                }
            }
            SWITCH_STATE_SYMBOL -> {
                if (isSpaceOrEnter(code)) {
                    toggleAlphabetAndSymbols(autoCapsFlags, recapitalizeMode)
                    mPrevSymbolsKeyboardWasShifted = false
                }
            }
        }
        if (Constants.isLetterCode(code)) updateAlphabetShiftState(autoCapsFlags, recapitalizeMode)
    }

    override fun toString(): String {
        return "[keyboard=${if (mIsAlphabetMode) mAlphabetShiftState.toString() else if (mIsSymbolShifted) "SYMBOLS_SHIFTED" else "SYMBOLS"} shift=$mShiftKeyState symbol=$mSymbolKeyState switch=${switchStateToString(mSwitchState)}]"
    }

    private fun stateToString(autoCapsFlags: Int, recapitalizeMode: Int): String {
        return "$this autoCapsFlags=${CapsModeUtils.flagsToString(autoCapsFlags)} recapitalizeMode=${RecapitalizeStatus.modeToString(recapitalizeMode)}"
    }

    companion object {
        private const val TAG = "KeyboardState"
        private const val DEBUG_EVENT = false
        private const val DEBUG_INTERNAL_ACTION = false
        private const val UNSHIFT = 0
        private const val MANUAL_SHIFT = 1
        private const val AUTOMATIC_SHIFT = 2
        private const val SHIFT_LOCK_SHIFTED = 3
        private const val SWITCH_STATE_ALPHA = 0
        private const val SWITCH_STATE_SYMBOL_BEGIN = 1
        private const val SWITCH_STATE_SYMBOL = 2
        private const val SWITCH_STATE_MOMENTARY_ALPHA_AND_SYMBOL = 3
        private const val SWITCH_STATE_MOMENTARY_SYMBOL_AND_MORE = 4

        private fun isSpaceOrEnter(c: Int): Boolean = c == Constants.CODE_SPACE || c == Constants.CODE_ENTER

        fun shiftModeToString(shiftMode: Int): String? = when (shiftMode) {
            UNSHIFT -> "UNSHIFT"
            MANUAL_SHIFT -> "MANUAL"
            AUTOMATIC_SHIFT -> "AUTOMATIC"
            else -> null
        }

        private fun switchStateToString(switchState: Int): String? = when (switchState) {
            SWITCH_STATE_ALPHA -> "ALPHA"
            SWITCH_STATE_SYMBOL_BEGIN -> "SYMBOL-BEGIN"
            SWITCH_STATE_SYMBOL -> "SYMBOL"
            SWITCH_STATE_MOMENTARY_ALPHA_AND_SYMBOL -> "MOMENTARY-ALPHA-SYMBOL"
            SWITCH_STATE_MOMENTARY_SYMBOL_AND_MORE -> "MOMENTARY-SYMBOL-MORE"
            else -> null
        }
    }
}
