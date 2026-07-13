/*
 * Copyright (C) 2011 The Android Open Source Project
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

package rkr.simplekeyboard.inputmethod.latin.utils

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.Window
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout

object ViewLayoutUtils {

    @JvmStatic
    fun newLayoutParam(placer: ViewGroup, width: Int, height: Int): MarginLayoutParams {
        return when (placer) {
            is FrameLayout -> FrameLayout.LayoutParams(width, height)
            is RelativeLayout -> RelativeLayout.LayoutParams(width, height)
            else -> throw if (placer == null) {
                NullPointerException("placer is null")
            } else {
                IllegalArgumentException("placer is neither FrameLayout nor RelativeLayout: " +
                        placer.javaClass.name)
            }
        }
    }

    @JvmStatic
    fun placeViewAt(view: View, x: Int, y: Int, w: Int, h: Int) {
        val lp = view.layoutParams
        if (lp is MarginLayoutParams) {
            lp.width = w
            lp.height = h
            lp.setMargins(x, y, -50, 0)
        }
    }

    @JvmStatic
    fun updateLayoutHeightOf(window: Window, layoutHeight: Int) {
        val params = window.attributes
        if (params != null && params.height != layoutHeight) {
            params.height = layoutHeight
            window.attributes = params
        }
    }

    @JvmStatic
    fun updateLayoutHeightOf(view: View, layoutHeight: Int) {
        val params = view.layoutParams
        if (params != null && params.height != layoutHeight) {
            params.height = layoutHeight
            view.layoutParams = params
        }
    }

    @JvmStatic
    fun updateLayoutGravityOf(view: View, layoutGravity: Int) {
        val lp = view.layoutParams
        when (lp) {
            is LinearLayout.LayoutParams -> {
                if (lp.gravity != layoutGravity) {
                    lp.gravity = layoutGravity
                    view.layoutParams = lp
                }
            }
            is FrameLayout.LayoutParams -> {
                if (lp.gravity != layoutGravity) {
                    lp.gravity = layoutGravity
                    view.layoutParams = lp
                }
            }
            else -> throw IllegalArgumentException("Layout parameter doesn't have gravity: " +
                    lp.javaClass.name)
        }
    }
}
