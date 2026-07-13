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

object CollectionUtils {

    @JvmStatic
    fun <E> arrayAsList(array: Array<E>, start: Int, end: Int): ArrayList<E> {
        require(start in 0..end && end <= array.size) {
            "Invalid start: $start end: $end with array.length: ${array.size}"
        }
        val list = ArrayList<E>(end - start)
        for (i in start until end) {
            list.add(array[i])
        }
        return list
    }
}
