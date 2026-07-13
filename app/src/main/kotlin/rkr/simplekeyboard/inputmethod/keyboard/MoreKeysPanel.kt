/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright (C) 2017 Raimondas Rimkus
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

package rkr.simplekeyboard.inputmethod.keyboard

import android.view.View
import android.view.ViewGroup

interface MoreKeysPanel {
    interface Controller {
        /**
         * Add the [MoreKeysPanel] to the target view.
         * @param panel the panel to be shown.
         */
        fun onShowMoreKeysPanel(panel: MoreKeysPanel)

        /**
         * Remove the current [MoreKeysPanel] from the target view.
         */
        fun onDismissMoreKeysPanel()

        /**
         * Instructs the parent to cancel the panel (e.g., when entering a different input mode).
         */
        fun onCancelMoreKeysPanel()
    }

    /**
     * Initializes the layout and event handling of this [MoreKeysPanel] and calls the
     * controller's onShowMoreKeysPanel to add the panel's container view.
     */
    fun showMoreKeysPanel(
        parentView: View,
        controller: Controller,
        pointX: Int,
        pointY: Int,
        listener: KeyboardActionListener
    )

    /**
     * Dismisses the more keys panel and calls the controller's onDismissMoreKeysPanel to remove
     * the panel's container view.
     */
    fun dismissMoreKeysPanel()

    fun onMoveEvent(x: Int, y: Int, pointerId: Int)

    fun onDownEvent(x: Int, y: Int, pointerId: Int)

    fun onUpEvent(x: Int, y: Int, pointerId: Int)

    fun translateX(x: Int): Int

    fun translateY(y: Int): Int

    fun showInParent(parentView: ViewGroup)

    fun removeFromParent()

    fun isShowingInParent(): Boolean

    companion object {
        @JvmField
        val EMPTY_CONTROLLER: Controller = object : Controller {
            override fun onShowMoreKeysPanel(panel: MoreKeysPanel) {}
            override fun onDismissMoreKeysPanel() {}
            override fun onCancelMoreKeysPanel() {}
        }
    }
}
