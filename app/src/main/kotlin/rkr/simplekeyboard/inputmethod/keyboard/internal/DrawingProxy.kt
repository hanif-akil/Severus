package rkr.simplekeyboard.inputmethod.keyboard.internal

interface DrawingProxy {
    fun invalidateKey(key: rkr.simplekeyboard.inputmethod.keyboard.Key)
    fun getKeyPreviewDrawParams(): KeyPreviewDrawParams
    fun getKeyDrawParams(): KeyDrawParams
    fun getKeyPreviewChoreographer(): KeyPreviewChoreographer

    companion object {
        const val FADE_IN = 0
        const val FADE_OUT = 1
    }
}
