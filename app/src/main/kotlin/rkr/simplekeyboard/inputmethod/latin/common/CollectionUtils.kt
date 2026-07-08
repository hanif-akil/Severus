package rkr.simplekeyboard.inputmethod.latin.common

object CollectionUtils {
    fun <E> arrayAsList(array: Array<E>, start: Int, end: Int): ArrayList<E> {
        if (start < 0 || start > end || end > array.size) {
            throw IllegalArgumentException(
                "Invalid start: $start end: $end with array.length: ${array.size}"
            )
        }
        val list = ArrayList<E>(end - start)
        for (i in start until end) {
            list.add(array[i])
        }
        return list
    }
}
