/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright (C) 2025 Raimondas Rimkus
 * Copyright (C) 2021 wittmane
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

import android.animation.AnimatorInflater
import android.animation.ObjectAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.keyboard.internal.DrawingPreviewPlacerView
import rkr.simplekeyboard.inputmethod.keyboard.internal.DrawingProxy
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeyDrawParams
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeyPreviewChoreographer
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeyPreviewDrawParams
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeyPreviewView
import rkr.simplekeyboard.inputmethod.keyboard.internal.MoreKeySpec
import rkr.simplekeyboard.inputmethod.keyboard.internal.NonDistinctMultitouchHelper
import rkr.simplekeyboard.inputmethod.keyboard.internal.TimerHandler
import rkr.simplekeyboard.inputmethod.latin.RichInputMethodManager
import rkr.simplekeyboard.inputmethod.latin.common.Constants
import rkr.simplekeyboard.inputmethod.latin.common.CoordinateUtils
import rkr.simplekeyboard.inputmethod.latin.utils.LanguageOnSpacebarUtils
import rkr.simplekeyboard.inputmethod.latin.utils.LocaleResourceUtils
import rkr.simplekeyboard.inputmethod.latin.utils.TypefaceUtils
import java.util.WeakHashMap

/**
 * A view that is responsible for detecting key presses and touch movements.
 */
class MainKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyle: Int = R.attr.mainKeyboardViewStyle
) : KeyboardView(context, attrs, defStyle), MoreKeysPanel.Controller, DrawingProxy {

    private var mKeyboardActionListener: KeyboardActionListener = KeyboardActionListener.EMPTY_LISTENER

    private var mSpaceKey: Key? = null
    private val mLanguageOnSpacebarFinalAlpha: Int
    private var mLanguageOnSpacebarFormatType: Int = 0
    private val mLanguageOnSpacebarTextRatio: Float
    private var mLanguageOnSpacebarTextSize: Float = 0f
    private val mLanguageOnSpacebarTextColor: Int

    private val mAltCodeKeyWhileTypingFadeoutAnimator: ObjectAnimator?
    private val mAltCodeKeyWhileTypingFadeinAnimator: ObjectAnimator?
    private var mAltCodeKeyWhileTypingAnimAlpha: Int = Constants.Color.ALPHA_OPAQUE

    private val mDrawingPreviewPlacerView: DrawingPreviewPlacerView
    private val mOriginCoords = CoordinateUtils.newInstance()

    private val mKeyPreviewDrawParams: KeyPreviewDrawParams
    private val mKeyPreviewChoreographer: KeyPreviewChoreographer

    private val mBackgroundDimAlphaPaint = Paint()
    private val mMoreKeysKeyboardContainer: View
    private val mMoreKeysKeyboardCache = WeakHashMap<Key, Keyboard>()
    private val mConfigShowMoreKeysKeyboardAtTouchedPoint: Boolean
    private var mMoreKeysPanel: MoreKeysPanel? = null

    private val mKeyDetector: KeyDetector
    private val mNonDistinctMultitouchHelper: NonDistinctMultitouchHelper?
    private val mTimerHandler: TimerHandler
    private val mLanguageOnSpacebarHorizontalMargin: Int

    init {
        val drawingPreviewPlacerView = DrawingPreviewPlacerView(context, attrs)

        val mainKeyboardViewAttr = context.obtainStyledAttributes(
            attrs, R.styleable.MainKeyboardView, defStyle, R.style.MainKeyboardView
        )
        val ignoreAltCodeKeyTimeout = mainKeyboardViewAttr.getInt(
            R.styleable.MainKeyboardView_ignoreAltCodeKeyTimeout, 0
        )
        mTimerHandler = TimerHandler(this, ignoreAltCodeKeyTimeout)

        val keyHysteresisDistance = mainKeyboardViewAttr.getDimension(
            R.styleable.MainKeyboardView_keyHysteresisDistance, 0.0f
        )
        val keyHysteresisDistanceForSlidingModifier = mainKeyboardViewAttr.getDimension(
            R.styleable.MainKeyboardView_keyHysteresisDistanceForSlidingModifier, 0.0f
        )
        mKeyDetector = KeyDetector(keyHysteresisDistance, keyHysteresisDistanceForSlidingModifier)

        PointerTracker.init(mainKeyboardViewAttr, mTimerHandler, this)

        val hasDistinctMultitouch = context.packageManager
            .hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT)
        mNonDistinctMultitouchHelper = if (hasDistinctMultitouch) null else NonDistinctMultitouchHelper()

        val backgroundDimAlpha = mainKeyboardViewAttr.getInt(
            R.styleable.MainKeyboardView_backgroundDimAlpha, 0
        )
        mBackgroundDimAlphaPaint.color = Color.BLACK
        mBackgroundDimAlphaPaint.alpha = backgroundDimAlpha
        mLanguageOnSpacebarTextRatio = mainKeyboardViewAttr.getFraction(
            R.styleable.MainKeyboardView_languageOnSpacebarTextRatio, 1, 1, 1.0f
        )
        mLanguageOnSpacebarTextColor = mainKeyboardViewAttr.getColor(
            R.styleable.MainKeyboardView_languageOnSpacebarTextColor, 0
        )
        mLanguageOnSpacebarFinalAlpha = mainKeyboardViewAttr.getInt(
            R.styleable.MainKeyboardView_languageOnSpacebarFinalAlpha,
            Constants.Color.ALPHA_OPAQUE
        )
        val altCodeKeyWhileTypingFadeoutAnimatorResId = mainKeyboardViewAttr.getResourceId(
            R.styleable.MainKeyboardView_altCodeKeyWhileTypingFadeoutAnimator, 0
        )
        val altCodeKeyWhileTypingFadeinAnimatorResId = mainKeyboardViewAttr.getResourceId(
            R.styleable.MainKeyboardView_altCodeKeyWhileTypingFadeinAnimator, 0
        )

        mKeyPreviewDrawParams = KeyPreviewDrawParams(mainKeyboardViewAttr)
        mKeyPreviewChoreographer = KeyPreviewChoreographer(mKeyPreviewDrawParams)

        val moreKeysKeyboardLayoutId = mainKeyboardViewAttr.getResourceId(
            R.styleable.MainKeyboardView_moreKeysKeyboardLayout, 0
        )
        mConfigShowMoreKeysKeyboardAtTouchedPoint = mainKeyboardViewAttr.getBoolean(
            R.styleable.MainKeyboardView_showMoreKeysKeyboardAtTouchedPoint, false
        )

        mainKeyboardViewAttr.recycle()

        mDrawingPreviewPlacerView = drawingPreviewPlacerView

        val inflater = LayoutInflater.from(context)
        mMoreKeysKeyboardContainer = inflater.inflate(moreKeysKeyboardLayoutId, null)
        mAltCodeKeyWhileTypingFadeoutAnimator = loadObjectAnimator(
            altCodeKeyWhileTypingFadeoutAnimatorResId, this
        )
        mAltCodeKeyWhileTypingFadeinAnimator = loadObjectAnimator(
            altCodeKeyWhileTypingFadeinAnimatorResId, this
        )

        mLanguageOnSpacebarHorizontalMargin = resources.getDimension(
            R.dimen.config_language_on_spacebar_horizontal_margin
        ).toInt()
    }

    private fun loadObjectAnimator(resId: Int, target: Any): ObjectAnimator? {
        if (resId == 0) return null
        val animator = AnimatorInflater.loadAnimator(context, resId) as? ObjectAnimator
        animator?.setTarget(target)
        return animator
    }

    override fun startWhileTypingAnimation(fadeInOrOut: Int) {
        when (fadeInOrOut) {
            DrawingProxy.FADE_IN -> cancelAndStartAnimators(
                mAltCodeKeyWhileTypingFadeoutAnimator, mAltCodeKeyWhileTypingFadeinAnimator
            )
            DrawingProxy.FADE_OUT -> cancelAndStartAnimators(
                mAltCodeKeyWhileTypingFadeinAnimator, mAltCodeKeyWhileTypingFadeoutAnimator
            )
        }
    }

    fun setKeyboardActionListener(listener: KeyboardActionListener) {
        mKeyboardActionListener = listener
        PointerTracker.setKeyboardActionListener(listener)
    }

    fun getKeyX(x: Int): Int = if (Constants.isValidCoordinate(x)) mKeyDetector.getTouchX(x) else x

    fun getKeyY(y: Int): Int = if (Constants.isValidCoordinate(y)) mKeyDetector.getTouchY(y) else y

    override fun setKeyboard(keyboard: Keyboard) {
        mTimerHandler.cancelLongPressTimers()
        super.setKeyboard(keyboard)
        mKeyDetector.setKeyboard(
            keyboard, -paddingLeft.toFloat(), (-paddingTop + verticalCorrection).toFloat()
        )
        PointerTracker.setKeyDetector(mKeyDetector)
        mMoreKeysKeyboardCache.clear()

        mSpaceKey = keyboard.getKey(Constants.CODE_SPACE)
        val keyHeight = keyboard.mMostCommonKeyHeight
        mLanguageOnSpacebarTextSize = keyHeight * mLanguageOnSpacebarTextRatio
    }

    fun setKeyPreviewPopupEnabled(previewEnabled: Boolean, delay: Int) {
        mKeyPreviewDrawParams.setPopupEnabled(previewEnabled, delay)
    }

    private fun locatePreviewPlacerView() {
        getLocationInWindow(mOriginCoords)
        mDrawingPreviewPlacerView.setKeyboardViewGeometry(mOriginCoords)
    }

    private fun installPreviewPlacerView() {
        val rootView = rootView
        if (rootView == null) {
            Log.w(TAG, "Cannot find root view")
            return
        }
        val windowContentView = rootView.findViewById<ViewGroup>(android.R.id.content) ?: run {
            Log.w(TAG, "Cannot find android.R.id.content view to add DrawingPreviewPlacerView")
            return
        }
        windowContentView.addView(mDrawingPreviewPlacerView)
    }

    override fun onKeyPressed(key: Key, withPreview: Boolean) {
        key.onPressed()
        invalidateKey(key)
        if (withPreview && !key.noKeyPreview) {
            showKeyPreview(key)
        }
    }

    private fun showKeyPreview(key: Key) {
        val keyboard = getKeyboard() ?: return
        val previewParams = mKeyPreviewDrawParams
        if (!previewParams.isPopupEnabled) {
            previewParams.setVisibleOffset(-Math.round(keyboard.mVerticalGap))
            return
        }

        locatePreviewPlacerView()
        getLocationInWindow(mOriginCoords)
        val backgroundColor = if (mTheme?.mCustomColorSupport == true) mCustomColor else Color.TRANSPARENT
        mKeyPreviewChoreographer.placeAndShowKeyPreview(
            key, keyboard.mIconsSet, getKeyDrawParams(),
            mOriginCoords, mDrawingPreviewPlacerView, isHardwareAccelerated, backgroundColor
        )
    }

    private fun dismissKeyPreviewWithoutDelay(key: Key) {
        mKeyPreviewChoreographer.dismissKeyPreview(key, false)
        invalidateKey(key)
    }

    override fun onKeyReleased(key: Key, withAnimation: Boolean) {
        key.onReleased()
        invalidateKey(key)
        if (!key.noKeyPreview) {
            if (withAnimation) {
                dismissKeyPreview(key)
            } else {
                dismissKeyPreviewWithoutDelay(key)
            }
        }
    }

    private fun dismissKeyPreview(key: Key) {
        if (isHardwareAccelerated) {
            mKeyPreviewChoreographer.dismissKeyPreview(key, true)
            return
        }
        mTimerHandler.postDismissKeyPreview(key, mKeyPreviewDrawParams.getLingerTimeout())
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        installPreviewPlacerView()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mDrawingPreviewPlacerView.removeAllViews()
    }

    fun showMoreKeysKeyboard(key: Key, tracker: PointerTracker): MoreKeysPanel? {
        val moreKeys = key.moreKeys ?: return null
        var moreKeysKeyboard = mMoreKeysKeyboardCache[key]
        if (moreKeysKeyboard == null) {
            val isSingleMoreKeyWithPreview = mKeyPreviewDrawParams.isPopupEnabled &&
                    !key.noKeyPreview && moreKeys.size == 1 &&
                    mKeyPreviewDrawParams.getVisibleWidth() > 0
            val builder = MoreKeysKeyboard.Builder(
                context, key, getKeyboard()!!, isSingleMoreKeyWithPreview,
                mKeyPreviewDrawParams.getVisibleWidth(),
                mKeyPreviewDrawParams.getVisibleHeight(), newLabelPaint(key)
            )
            moreKeysKeyboard = builder.build()
            mMoreKeysKeyboardCache[key] = moreKeysKeyboard
        }

        val moreKeysKeyboardView = mMoreKeysKeyboardContainer.findViewById<MoreKeysKeyboardView>(
            R.id.more_keys_keyboard_view
        )
        moreKeysKeyboardView.setKeyboard(moreKeysKeyboard)
        mMoreKeysKeyboardContainer.measure(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val lastCoords = CoordinateUtils.newInstance()
        tracker.getLastCoordinates(lastCoords)
        val keyPreviewEnabled = mKeyPreviewDrawParams.isPopupEnabled && !key.noKeyPreview
        val pointX = if (mConfigShowMoreKeysKeyboardAtTouchedPoint && !keyPreviewEnabled) {
            CoordinateUtils.x(lastCoords)
        } else {
            key.x + key.width / 2
        }
        val pointY = key.y + mKeyPreviewDrawParams.getVisibleOffset() +
                Math.round(moreKeysKeyboard.mBottomPadding)
        moreKeysKeyboardView.showMoreKeysPanel(this, this, pointX, pointY, mKeyboardActionListener)
        return moreKeysKeyboardView
    }

    fun isInDraggingFinger(): Boolean {
        return isShowingMoreKeysPanel() || PointerTracker.isAnyInDraggingFinger()
    }

    fun isInCursorMove(): Boolean = PointerTracker.isAnyInCursorMove()

    override fun onShowMoreKeysPanel(panel: MoreKeysPanel) {
        locatePreviewPlacerView()
        onDismissMoreKeysPanel()
        PointerTracker.setReleasedKeyGraphicsToAllKeys()
        panel.showInParent(mDrawingPreviewPlacerView)
        mMoreKeysPanel = panel
    }

    fun isShowingMoreKeysPanel(): Boolean {
        return mMoreKeysPanel?.isShowingInParent() == true
    }

    override fun onCancelMoreKeysPanel() {
        PointerTracker.dismissAllMoreKeysPanels()
    }

    override fun onDismissMoreKeysPanel() {
        if (isShowingMoreKeysPanel()) {
            mMoreKeysPanel?.removeFromParent()
            mMoreKeysPanel = null
        }
    }

    fun startDoubleTapShiftKeyTimer() {
        mTimerHandler.startDoubleTapShiftKeyTimer()
    }

    fun cancelDoubleTapShiftKeyTimer() {
        mTimerHandler.cancelDoubleTapShiftKeyTimer()
    }

    fun isInDoubleTapShiftKeyTimeout(): Boolean = mTimerHandler.isInDoubleTapShiftKeyTimeout()

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (getKeyboard() == null) return false
        if (mNonDistinctMultitouchHelper != null) {
            if (event.pointerCount > 1 && mTimerHandler.isInKeyRepeat()) {
                mTimerHandler.cancelKeyRepeatTimers()
            }
            mNonDistinctMultitouchHelper.processMotionEvent(event, mKeyDetector)
            return true
        }
        return processMotionEvent(event)
    }

    fun processMotionEvent(event: MotionEvent): Boolean {
        val index = event.actionIndex
        val id = event.getPointerId(index)
        val tracker = PointerTracker.getPointerTracker(id)
        if (isShowingMoreKeysPanel() && !tracker.isShowingMoreKeysPanel() &&
            PointerTracker.getActivePointerTrackerCount() == 1
        ) {
            return true
        }
        tracker.processMotionEvent(event, mKeyDetector)
        return true
    }

    fun cancelAllOngoingEvents() {
        mTimerHandler.cancelAllMessages()
        PointerTracker.setReleasedKeyGraphicsToAllKeys()
        PointerTracker.dismissAllMoreKeysPanels()
        PointerTracker.cancelAllPointerTrackers()
    }

    fun closing() {
        cancelAllOngoingEvents()
        mMoreKeysKeyboardCache.clear()
    }

    fun onHideWindow() {
        onDismissMoreKeysPanel()
    }

    fun startDisplayLanguageOnSpacebar(subtypeChanged: Boolean, languageOnSpacebarFormatType: Int) {
        if (subtypeChanged) {
            KeyPreviewView.clearTextCache()
        }
        mLanguageOnSpacebarFormatType = languageOnSpacebarFormatType
        invalidateKey(mSpaceKey)
    }

    override fun onDrawKeyTopVisuals(key: Key, canvas: Canvas, paint: Paint, params: KeyDrawParams) {
        if (key.isAltCodeWhileTyping) {
            params.mAnimAlpha = mAltCodeKeyWhileTypingAnimAlpha
        }
        super.onDrawKeyTopVisuals(key, canvas, paint, params)
        if (key.code == Constants.CODE_SPACE) {
            val imm = RichInputMethodManager.getInstance()
            if (imm.hasMultipleEnabledSubtypes()) {
                drawLanguageOnSpacebar(key, canvas, paint)
            }
        }
    }

    private fun fitsTextIntoWidth(width: Int, text: String, paint: Paint): Boolean {
        val maxTextWidth = width - mLanguageOnSpacebarHorizontalMargin * 2
        paint.textScaleX = 1.0f
        val textWidth = TypefaceUtils.getStringWidth(text, paint)
        if (textWidth < width) return true

        val scaleX = maxTextWidth / textWidth
        if (scaleX < MINIMUM_XSCALE_OF_LANGUAGE_NAME) return false

        paint.textScaleX = scaleX
        return TypefaceUtils.getStringWidth(text, paint) < maxTextWidth
    }

    private fun layoutLanguageOnSpacebar(paint: Paint, subtype: rkr.simplekeyboard.inputmethod.latin.Subtype, width: Int): String {
        if (mLanguageOnSpacebarFormatType == LanguageOnSpacebarUtils.FORMAT_TYPE_FULL_LOCALE) {
            val fullText = LocaleResourceUtils.getLocaleDisplayNameInLocale(subtype.getLocale())
            if (fitsTextIntoWidth(width, fullText, paint)) return fullText
        }

        val middleText = LocaleResourceUtils.getLanguageDisplayNameInLocale(subtype.getLocale())
        if (fitsTextIntoWidth(width, middleText, paint)) return middleText

        return ""
    }

    private fun drawLanguageOnSpacebar(key: Key, canvas: Canvas, paint: Paint) {
        val keyboard = getKeyboard() ?: return
        val width = key.width
        val height = key.height
        paint.textAlign = Align.CENTER
        paint.typeface = Typeface.DEFAULT
        paint.textSize = mLanguageOnSpacebarTextSize
        val language = layoutLanguageOnSpacebar(paint, keyboard.mId.mSubtype, width)
        val descent = paint.descent()
        val textHeight = -paint.ascent() + descent
        val baseline = height / 2 + textHeight / 2
        paint.color = mLanguageOnSpacebarTextColor
        paint.alpha = mLanguageOnSpacebarFinalAlpha
        canvas.drawText(language, (width / 2).toFloat(), baseline - descent, paint)
        paint.clearShadowLayer()
        paint.textScaleX = 1.0f
    }

    companion object {
        private const val TAG = "MainKeyboardView"
        private const val MINIMUM_XSCALE_OF_LANGUAGE_NAME = 0.8f

        private fun cancelAndStartAnimators(animatorToCancel: ObjectAnimator?, animatorToStart: ObjectAnimator?) {
            if (animatorToCancel == null || animatorToStart == null) return
            var startFraction = 0.0f
            if (animatorToCancel.isStarted) {
                animatorToCancel.cancel()
                startFraction = 1.0f - animatorToCancel.animatedFraction
            }
            val startTime = (animatorToStart.duration * startFraction).toLong()
            animatorToStart.start()
            animatorToStart.currentPlayTime = startTime
        }
    }
}
