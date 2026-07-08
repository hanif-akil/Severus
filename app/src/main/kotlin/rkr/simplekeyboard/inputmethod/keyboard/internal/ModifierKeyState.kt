package rkr.simplekeyboard.inputmethod.keyboard.internal

import android.util.Log

internal open class ModifierKeyState(val mName: String) {
    protected var mState = RELEASING

    open fun onPress() { mState = PRESSING }
    open fun onRelease() { mState = RELEASING }

    open fun onOtherKeyPressed() {
        val oldState = mState
        if (oldState == PRESSING) mState = CHORDING
        if (DEBUG) Log.d(TAG, "$mName.onOtherKeyPressed: ${toString(oldState)} > $this")
    }

    fun isPressing(): Boolean = mState == PRESSING
    fun isReleasing(): Boolean = mState == RELEASING
    fun isChording(): Boolean = mState == CHORDING

    override fun toString(): String = toString(mState)

    protected open fun toString(state: Int): String = when (state) {
        RELEASING -> "RELEASING"
        PRESSING -> "PRESSING"
        CHORDING -> "CHORDING"
        else -> "UNKNOWN"
    }

    companion object {
        protected const val TAG = "ModifierKeyState"
        protected const val DEBUG = false
        protected const val RELEASING = 0
        protected const val PRESSING = 1
        protected const val CHORDING = 2
    }
}
