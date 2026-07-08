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
    private var mBackgroundThread = Executors.newSingleThreadExecutor()
    private var mAudioManager: AudioManager? = null
    private var mVibrator: Vibrator? = null
    private var mSettingsValues: SettingsValues? = null
    private var mSoundOn = false
    private var mLastTickTime: Long = 0

    fun init(context: Context) {
        mBackgroundThread.execute {
            mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
            mVibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        }
    }

    fun hasVibrator(): Boolean = mVibrator?.hasVibrator() == true

    private fun reevaluateIfSoundIsOn(): Boolean {
        if (mSettingsValues == null || !mSettingsValues!!.mSoundOn || mAudioManager == null) return false
        return mAudioManager!!.ringerMode == AudioManager.RINGER_MODE_NORMAL
    }

    fun performAudioFeedback(code: Int) {
        if (mAudioManager == null || !mSoundOn) return
        val sound = when (code) {
            Constants.CODE_DELETE -> AudioManager.FX_KEYPRESS_DELETE
            Constants.CODE_ENTER -> AudioManager.FX_KEYPRESS_RETURN
            Constants.CODE_SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR
            else -> AudioManager.FX_KEYPRESS_STANDARD
        }
        playSoundEffect(sound, mSettingsValues!!.mKeypressSoundVolume)
    }

    fun playSoundEffect(effectType: Int, volume: Float) {
        if (mAudioManager == null) return
        mBackgroundThread.execute { mAudioManager!!.playSoundEffect(effectType, volume) }
    }

    fun performHapticFeedback(viewToPerformHapticFeedbackOn: View?) {
        if (mSettingsValues?.mVibrateOn != true || mVibrator == null) return
        mBackgroundThread.execute {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mVibrator!!.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (viewToPerformHapticFeedbackOn != null) {
                viewToPerformHapticFeedbackOn.performHapticFeedback(
                    HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
            }
        }
    }

    fun performTickFeedback() {
        if (mSettingsValues?.mVibrateOn != true || mVibrator == null ||
            System.currentTimeMillis() - mLastTickTime < TICK_FREQUENCY
        ) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mLastTickTime = System.currentTimeMillis()
            mBackgroundThread.execute { mVibrator!!.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)) }
        }
    }

    fun onSettingsChanged(settingsValues: SettingsValues) {
        mSettingsValues = settingsValues
        mSoundOn = reevaluateIfSoundIsOn()
    }

    fun onRingerModeChanged() {
        mSoundOn = reevaluateIfSoundIsOn()
    }

    companion object {
        private const val TICK_FREQUENCY = 100L
        private val sInstance = AudioAndHapticFeedbackManager()
        fun getInstance(): AudioAndHapticFeedbackManager = sInstance
        fun init(context: Context) { sInstance.init(context) }
    }
}
