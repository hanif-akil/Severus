package rkr.simplekeyboard.inputmethod.latin.common

object CoordinateUtils {
    private const val INDEX_X = 0
    private const val INDEX_Y = 1
    private const val ELEMENT_SIZE = INDEX_Y + 1

    fun newInstance(): IntArray = IntArray(ELEMENT_SIZE)

    fun x(coords: IntArray): Int = coords[INDEX_X]

    fun y(coords: IntArray): Int = coords[INDEX_Y]

    fun set(coords: IntArray, x: Int, y: Int) {
        coords[INDEX_X] = x
        coords[INDEX_Y] = y
    }

    fun copy(destination: IntArray, source: IntArray) {
        destination[INDEX_X] = source[INDEX_X]
        destination[INDEX_Y] = source[INDEX_Y]
    }
}
