package rkr.simplekeyboard.inputmethod.keyboard

interface KeyboardActionListener {
    fun onPressKey(primaryCode: Int, repeatCount: Int, isSinglePointer: Boolean)
    fun onReleaseKey(primaryCode: Int, withSliding: Boolean)
    fun onCodeInput(primaryCode: Int, x: Int, y: Int, isKeyRepeat: Boolean)
    fun onTextInput(rawText: String)
    fun onFinishSlidingInput()
    fun onCustomRequest(requestCode: Int): Boolean
    fun onMoveCursorPointer(steps: Int)
    fun onMoveDeletePointer(steps: Int)
    fun onUpWithDeletePointerActive()
    fun onUpWithSpacePointerActive()
    fun onToggleClipboardHistory()

    open class Adapter : KeyboardActionListener {
        override fun onPressKey(primaryCode: Int, repeatCount: Int, isSinglePointer: Boolean) {}
        override fun onReleaseKey(primaryCode: Int, withSliding: Boolean) {}
        override fun onCodeInput(primaryCode: Int, x: Int, y: Int, isKeyRepeat: Boolean) {}
        override fun onTextInput(text: String) {}
        override fun onFinishSlidingInput() {}
        override fun onCustomRequest(requestCode: Int): Boolean = false
        override fun onMoveCursorPointer(steps: Int) {}
        override fun onMoveDeletePointer(steps: Int) {}
        override fun onUpWithDeletePointerActive() {}
        override fun onUpWithSpacePointerActive() {}
        override fun onToggleClipboardHistory() {}
    }

    companion object {
        val EMPTY_LISTENER: KeyboardActionListener = Adapter()
    }
}
