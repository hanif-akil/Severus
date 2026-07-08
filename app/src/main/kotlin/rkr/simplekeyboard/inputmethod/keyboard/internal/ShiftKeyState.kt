package rkr.simplekeyboard.inputmethod.keyboard.internal

import android.util.Log

internal class ShiftKeyState(name: String) : ModifierKeyState(name) {
    override fun onOtherKeyPressed() {
        val oldState = mState
        if (oldState == PRESSING) {
            mState = CHORDING
        } else if (oldState == PRESSING_ON_SHIFTED) {
            mState = IGNORING
        }
        if (DEBUG) Log.d(TAG, "$mName.onOtherKeyPressed: ${toString(oldState)} > $this")
    }

    fun onPressOnShifted() { mState = PRESSING_ON_SHIFTED }
    fun isPressingOnShifted(): Boolean = mState == PRESSING_ON_SHIFTED
    fun isIgnoring(): Boolean = mState == IGNORING

    override fun toString(): String = toString(mState)

    override fun toString(state: Int): String = when (state) {
        PRESSING_ON_SHIFTED -> "PRESSING_ON_SHIFTED"
        IGNORING -> "IGNORING"
        else -> super.toString(state)
    }

    companion object {
        private const val PRESSING_ON_SHIFTED = 3
        private const val IGNORING = 4
    }
}
