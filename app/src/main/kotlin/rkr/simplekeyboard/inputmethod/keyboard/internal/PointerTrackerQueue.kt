package rkr.simplekeyboard.inputmethod.keyboard.internal

import android.util.Log

class PointerTrackerQueue {
    interface Element {
        fun isModifier(): Boolean
        fun isInDraggingFinger(): Boolean
        fun isInCursorMove(): Boolean
        fun onPhantomUpEvent(eventTime: Long)
        fun cancelTrackingForAction()
    }

    private val mExpandableArrayOfActivePointers = ArrayList<Element>(INITIAL_CAPACITY)
    private var mArraySize = 0

    val size: Int
        get() = synchronized(mExpandableArrayOfActivePointers) { mArraySize }

    fun add(pointer: Element) {
        synchronized(mExpandableArrayOfActivePointers) {
            if (DEBUG) Log.d(TAG, "add: $pointer $this")
            if (mArraySize < mExpandableArrayOfActivePointers.size) {
                mExpandableArrayOfActivePointers[mArraySize] = pointer
            } else {
                mExpandableArrayOfActivePointers.add(pointer)
            }
            mArraySize++
        }
    }

    fun remove(pointer: Element) {
        synchronized(mExpandableArrayOfActivePointers) {
            if (DEBUG) Log.d(TAG, "remove: $pointer $this")
            var newIndex = 0
            for (index in 0 until mArraySize) {
                val element = mExpandableArrayOfActivePointers[index]
                if (element == pointer) {
                    if (newIndex != index) Log.w(TAG, "Found duplicated element in remove: $pointer")
                    continue
                }
                if (newIndex != index) mExpandableArrayOfActivePointers[newIndex] = element
                newIndex++
            }
            mArraySize = newIndex
        }
    }

    fun releaseAllPointersOlderThan(pointer: Element, eventTime: Long) {
        synchronized(mExpandableArrayOfActivePointers) {
            if (DEBUG) Log.d(TAG, "releaseAllPointerOlderThan: $pointer $this")
            var newIndex = 0
            var index = 0
            while (index < mArraySize) {
                val element = mExpandableArrayOfActivePointers[index]
                if (element == pointer) break
                if (!element.isModifier()) {
                    element.onPhantomUpEvent(eventTime)
                    index++
                    continue
                }
                if (newIndex != index) mExpandableArrayOfActivePointers[newIndex] = element
                newIndex++
                index++
            }
            var count = 0
            while (index < mArraySize) {
                val element = mExpandableArrayOfActivePointers[index]
                if (element == pointer) {
                    count++
                    if (count > 1) Log.w(TAG, "Found duplicated element in releaseAllPointersOlderThan: $pointer")
                }
                if (newIndex != index) mExpandableArrayOfActivePointers[newIndex] = mExpandableArrayOfActivePointers[index]
                newIndex++
                index++
            }
            mArraySize = newIndex
        }
    }

    fun releaseAllPointers(eventTime: Long) {
        releaseAllPointersExcept(null, eventTime)
    }

    fun releaseAllPointersExcept(pointer: Element?, eventTime: Long) {
        synchronized(mExpandableArrayOfActivePointers) {
            if (DEBUG) {
                if (pointer == null) Log.d(TAG, "releaseAllPointers: $this")
                else Log.d(TAG, "releaseAllPointerExcept: $pointer $this")
            }
            var newIndex = 0
            var count = 0
            for (index in 0 until mArraySize) {
                val element = mExpandableArrayOfActivePointers[index]
                if (element == pointer) {
                    count++
                    if (count > 1) Log.w(TAG, "Found duplicated element in releaseAllPointersExcept: $pointer")
                } else {
                    element.onPhantomUpEvent(eventTime)
                    continue
                }
                if (newIndex != index) mExpandableArrayOfActivePointers[newIndex] = element
                newIndex++
            }
            mArraySize = newIndex
        }
    }

    fun hasModifierKeyOlderThan(pointer: Element): Boolean {
        synchronized(mExpandableArrayOfActivePointers) {
            for (index in 0 until mArraySize) {
                val element = mExpandableArrayOfActivePointers[index]
                if (element == pointer) return false
                if (element.isModifier()) return true
            }
            return false
        }
    }

    fun isAnyInDraggingFinger(): Boolean {
        synchronized(mExpandableArrayOfActivePointers) {
            for (index in 0 until mArraySize) {
                if (mExpandableArrayOfActivePointers[index].isInDraggingFinger()) return true
            }
            return false
        }
    }

    fun isAnyInCursorMove(): Boolean {
        synchronized(mExpandableArrayOfActivePointers) {
            for (index in 0 until mArraySize) {
                if (mExpandableArrayOfActivePointers[index].isInCursorMove()) return true
            }
            return false
        }
    }

    fun cancelAllPointerTrackers() {
        synchronized(mExpandableArrayOfActivePointers) {
            if (DEBUG) Log.d(TAG, "cancelAllPointerTracker: $this")
            for (index in 0 until mArraySize) {
                mExpandableArrayOfActivePointers[index].cancelTrackingForAction()
            }
        }
    }

    override fun toString(): String {
        synchronized(mExpandableArrayOfActivePointers) {
            val sb = StringBuilder()
            for (index in 0 until mArraySize) {
                if (sb.length > 0) sb.append(" ")
                sb.append(mExpandableArrayOfActivePointers[index].toString())
            }
            return "[$sb]"
        }
    }

    companion object {
        private const val TAG = "PointerTrackerQueue"
        private const val DEBUG = false
        private const val INITIAL_CAPACITY = 10
    }
}
