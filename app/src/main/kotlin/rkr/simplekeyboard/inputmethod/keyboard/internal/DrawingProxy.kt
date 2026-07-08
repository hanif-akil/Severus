package rkr.simplekeyboard.inputmethod.keyboard.internal

interface DrawingProxy {
    fun invalidateKey(key: rkr.simplekeyboard.inputmethod.keyboard.Key)
    fun getKeyPreviewDrawParams(): KeyPreviewDrawParams
    fun getKeyDrawParams(): KeyDrawParams
    fun getKeyPreviewChoreographer(): KeyPreviewChoreographer
}
