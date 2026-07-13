/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2025 Raimondas Rimkus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rkr.simplekeyboard.inputmethod.keyboard.internal

import android.util.Log
import java.util.ArrayList

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

    fun size(): Int {
        synchronized(mExpandableArrayOfActivePointers) {
            return mArraySize
        }
    }

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
                val element = mExpandableArrayOfActivePointers[index]
                if (sb.isNotEmpty()) sb.append(" ")
                sb.append(element.toString())
            }
            return "[$sb]"
        }
    }

    companion object {
        private val TAG = PointerTrackerQueue::class.java.simpleName
        private const val DEBUG = false
        private const val INITIAL_CAPACITY = 10
    }
}
