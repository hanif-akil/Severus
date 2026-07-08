package rkr.simplekeyboard.inputmethod.keyboard

open class KeyDetector(
    keyHysteresisDistance: Float = 0.0f,
    keyHysteresisDistanceForSlidingModifier: Float = 0.0f
) {
    private val mKeyHysteresisDistanceSquared: Int = (keyHysteresisDistance * keyHysteresisDistance).toInt()
    private val mKeyHysteresisDistanceForSlidingModifierSquared: Int = (keyHysteresisDistanceForSlidingModifier * keyHysteresisDistanceForSlidingModifier).toInt()
    private var mKeyboard: Keyboard? = null
    private var mCorrectionX = 0
    private var mCorrectionY = 0

    fun setKeyboard(keyboard: Keyboard, correctionX: Float, correctionY: Float) {
        mCorrectionX = correctionX.toInt()
        mCorrectionY = correctionY.toInt()
        mKeyboard = keyboard
    }

    fun getKeyHysteresisDistanceSquared(isSlidingFromModifier: Boolean): Int {
        return if (isSlidingFromModifier) mKeyHysteresisDistanceForSlidingModifierSquared else mKeyHysteresisDistanceSquared
    }

    fun getTouchX(x: Int): Int = x + mCorrectionX
    fun getTouchY(y: Int): Int = y + mCorrectionY
    fun getKeyboard(): Keyboard? = mKeyboard

    open fun alwaysAllowsKeySelectionByDraggingFinger(): Boolean = false

    open fun detectHitKey(x: Int, y: Int): Key? {
        val keyboard = mKeyboard ?: return null
        val touchX = getTouchX(x)
        val touchY = getTouchY(y)
        for (key in keyboard.getNearestKeys(touchX, touchY)) {
            if (key.isOnKey(touchX, touchY)) return key
        }
        return null
    }
}
