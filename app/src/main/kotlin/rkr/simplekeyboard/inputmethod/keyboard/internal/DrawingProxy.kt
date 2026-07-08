package rkr.simplekeyboard.inputmethod.keyboard.internal

interface DrawingProxy {
    fun invalidateKey(key: rkr.simplekeyboard.inputmethod.keyboard.Key)
    fun getKeyPreviewDrawParams(): KeyPreviewDrawParams
    fun getKeyDrawParams(): KeyDrawParams
    fun getKeyPreviewChoreographer(): KeyPreviewChoreographer
    fun onKeyPressed(key: rkr.simplekeyboard.inputmethod.keyboard.Key, withPreview: Boolean)
    fun onKeyReleased(key: rkr.simplekeyboard.inputmethod.keyboard.Key, withAnimation: Boolean)
    fun showMoreKeysKeyboard(key: rkr.simplekeyboard.inputmethod.keyboard.Key, tracker: rkr.simplekeyboard.inputmethod.keyboard.PointerTracker): rkr.simplekeyboard.inputmethod.keyboard.MoreKeysPanel?

    companion object {
        const val FADE_IN = 0
        const val FADE_OUT = 1
    }
}
