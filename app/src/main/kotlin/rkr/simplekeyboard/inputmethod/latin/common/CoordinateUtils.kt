/*
 * Copyright (C) 2012 The Android Open Source Project
 * Copyright (C) 2019 Raimondas Rimkus
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

package rkr.simplekeyboard.inputmethod.latin.common

object CoordinateUtils {
    private const val INDEX_X = 0
    private const val INDEX_Y = 1
    private const val ELEMENT_SIZE = INDEX_Y + 1

    @JvmStatic
    fun newInstance(): IntArray = IntArray(ELEMENT_SIZE)

    @JvmStatic
    fun x(coords: IntArray): Int = coords[INDEX_X]

    @JvmStatic
    fun y(coords: IntArray): Int = coords[INDEX_Y]

    @JvmStatic
    fun set(coords: IntArray, x: Int, y: Int) {
        coords[INDEX_X] = x
        coords[INDEX_Y] = y
    }

    @JvmStatic
    fun copy(destination: IntArray, source: IntArray) {
        destination[INDEX_X] = source[INDEX_X]
        destination[INDEX_Y] = source[INDEX_Y]
    }
}
