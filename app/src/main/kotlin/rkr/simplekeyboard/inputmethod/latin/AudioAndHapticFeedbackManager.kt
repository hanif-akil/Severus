/*
 * Copyright (C) 2012 The Android Open Source Project
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

package rkr.simplekeyboard.inputmethod.latin

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.View
import rkr.simplekeyboard.inputmethod.latin.common.Constants
import rkr.simplekeyboard.inputmethod.latin.settings.SettingsValues
import java.util.concurrent.Executors

class AudioAndHapticFeedbackManager private constructor() {

    companion object {
        private const val TICK_FREQUENCY = 100L

        @JvmStatic
        val instance: AudioAndHapticFeedbackManager = AudioAndHapticFeedbackManager()

        @JvmStatic
        fun init(context: Context) {
            instance.initInternal(context)
        }
    }

    private var mBackgroundThread = Executors.newSingleThreadExecutor()
    private var mAudioManager: AudioManager? = null
    private var mVibrator: Vibrator? = null

    private var mSettingsValues: SettingsValues? = null
    private var mSoundOn = false
    private var mLastTickTime = 0L

    private fun initInternal(context: Context) {
        mBackgroundThread.execute {
            mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            mVibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun hasVibrator(): Boolean {
        return mVibrator?.hasVibrator() == true
    }

    private fun reevaluateIfSoundIsOn(): Boolean {
        if (mSettingsValues == null || mSettingsValues?.mSoundOn != true || mAudioManager == null) {
            return false
        }
        return mAudioManager?.ringerMode == AudioManager.RINGER_MODE_NORMAL
    }

    fun performAudioFeedback(code: Int) {
        if (mAudioManager == null) {
            return
        }
        if (!mSoundOn) {
            return
        }
        val sound = when (code) {
            Constants.CODE_DELETE -> AudioManager.FX_KEYPRESS_DELETE
            Constants.CODE_ENTER -> AudioManager.FX_KEYPRESS_RETURN
            Constants.CODE_SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR
            else -> AudioManager.FX_KEYPRESS_STANDARD
        }
        playSoundEffect(sound, mSettingsValues?.mKeypressSoundVolume ?: 0f)
    }

    fun playSoundEffect(effectType: Int, volume: Float) {
        if (mAudioManager == null) {
            return
        }

        mBackgroundThread.execute {
            mAudioManager?.playSoundEffect(effectType, volume)
        }
    }

    fun performHapticFeedback(viewToPerformHapticFeedbackOn: View?) {
        if (mSettingsValues?.mVibrateOn != true || mVibrator == null) {
            return
        }
        mBackgroundThread.execute {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (viewToPerformHapticFeedbackOn != null) {
                viewToPerformHapticFeedbackOn.performHapticFeedback(
                    HapticFeedbackConstants.KEYBOARD_TAP,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
            }
        }
    }

    fun performTickFeedback() {
        if (mSettingsValues?.mVibrateOn != true
            || mVibrator == null
            || System.currentTimeMillis() - mLastTickTime < TICK_FREQUENCY
        ) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mLastTickTime = System.currentTimeMillis()
            mBackgroundThread.execute {
                mVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            }
        }
    }

    fun onSettingsChanged(settingsValues: SettingsValues) {
        mSettingsValues = settingsValues
        mSoundOn = reevaluateIfSoundIsOn()
    }

    fun onRingerModeChanged() {
        mSoundOn = reevaluateIfSoundIsOn()
    }
}
