package rkr.simplekeyboard.inputmethod.keyboard

class MoreKeysDetector(slideAllowance: Float) : KeyDetector() {
    private val mSlideAllowanceSquare: Int = (slideAllowance * slideAllowance).toInt()
    private val mSlideAllowanceSquareTop: Int = mSlideAllowanceSquare * 2

    override fun alwaysAllowsKeySelectionByDraggingFinger(): Boolean = true

    override fun detectHitKey(x: Int, y: Int): Key? {
        val keyboard = getKeyboard() ?: return null
        val touchX = getTouchX(x)
        val touchY = getTouchY(y)
        var nearestKey: Key? = null
        var nearestDist = if (y < 0) mSlideAllowanceSquareTop else mSlideAllowanceSquare
        for (key in keyboard.getSortedKeys()) {
            val dist = key.squaredDistanceToHitboxEdge(touchX, touchY)
            if (dist < nearestDist) {
                nearestKey = key
                nearestDist = dist
            }
        }
        return nearestKey
    }
}
