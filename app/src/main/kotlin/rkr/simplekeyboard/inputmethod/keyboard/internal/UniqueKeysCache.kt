/*
 * Copyright (C) 2014 The Android Open Source Project
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

package rkr.simplekeyboard.inputmethod.keyboard.internal

import rkr.simplekeyboard.inputmethod.keyboard.Key

abstract class UniqueKeysCache {
    abstract fun setEnabled(enabled: Boolean)
    abstract fun clear()
    abstract fun getUniqueKey(key: Key): Key

    companion object {
        @JvmField
        val NO_CACHE: UniqueKeysCache = object : UniqueKeysCache() {
            override fun setEnabled(enabled: Boolean) {}
            override fun clear() {}
            override fun getUniqueKey(key: Key): Key = key
        }

        @JvmStatic
        fun newInstance(): UniqueKeysCache = UniqueKeysCacheImpl()
    }

    private class UniqueKeysCacheImpl : UniqueKeysCache() {
        private val mCache = HashMap<Key, Key>()
        private var mEnabled = false

        override fun setEnabled(enabled: Boolean) {
            mEnabled = enabled
        }

        override fun clear() {
            mCache.clear()
        }

        override fun getUniqueKey(key: Key): Key {
            if (!mEnabled) return key
            val existingKey = mCache[key]
            if (existingKey != null) return existingKey
            mCache[key] = key
            return key
        }
    }
}
