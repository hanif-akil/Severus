package rkr.simplekeyboard.inputmethod.keyboard

import android.animation.AnimatorInflater
import android.animation.ObjectAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.keyboard.internal.*
import rkr.simplekeyboard.inputmethod.latin.ClipboardHistory
import rkr.simplekeyboard.inputmethod.latin.RichInputMethodManager
import rkr.simplekeyboard.inputmethod.latin.common.Constants
import rkr.simplekeyboard.inputmethod.latin.common.CoordinateUtils
import rkr.simplekeyboard.inputmethod.latin.utils.LanguageOnSpacebarUtils
import rkr.simplekeyboard.inputmethod.latin.utils.LocaleResourceUtils
import rkr.simplekeyboard.inputmethod.latin.utils.TypefaceUtils
import java.util.WeakHashMap

class MainKeyboardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = R.attr.mainKeyboardViewStyle
) : KeyboardView(context, attrs, defStyle), MoreKeysPanel.Controller, DrawingProxy {

    private var mKeyboardActionListener: KeyboardActionListener = KeyboardActionListener.EMPTY_LISTENER
    private var mSpaceKey: Key? = null
    private val mLanguageOnSpacebarFinalAlpha: Int
    private var mLanguageOnSpacebarFormatType = 0
    private val mLanguageOnSpacebarTextRatio: Float
    private var mLanguageOnSpacebarTextSize = 0f
    private val mLanguageOnSpacebarTextColor: Int
    private val mAltCodeKeyWhileTypingFadeoutAnimator: ObjectAnimator?
    private val mAltCodeKeyWhileTypingFadeinAnimator: ObjectAnimator?
    private var mAltCodeKeyWhileTypingAnimAlpha = Constants.Color.ALPHA_OPAQUE
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
    private var mClipboardHistoryView: ClipboardHistoryView? = null

    init {
        val drawingPreviewPlacerView = DrawingPreviewPlacerView(context, attrs)
        val mainKeyboardViewAttr = context.obtainStyledAttributes(attrs, R.styleable.MainKeyboardView, defStyle, R.style.MainKeyboardView)
        val ignoreAltCodeKeyTimeout = mainKeyboardViewAttr.getInt(R.styleable.MainKeyboardView_ignoreAltCodeKeyTimeout, 0)
        mTimerHandler = TimerHandler(this, ignoreAltCodeKeyTimeout)
        val keyHysteresisDistance = mainKeyboardViewAttr.getDimension(R.styleable.MainKeyboardView_keyHysteresisDistance, 0f)
        val keyHysteresisDistanceForSlidingModifier = mainKeyboardViewAttr.getDimension(R.styleable.MainKeyboardView_keyHysteresisDistanceForSlidingModifier, 0f)
        mKeyDetector = KeyDetector(keyHysteresisDistance, keyHysteresisDistanceForSlidingModifier)
        PointerTracker.init(mainKeyboardViewAttr, mTimerHandler, this)
        val hasDistinctMultitouch = context.packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT)
        mNonDistinctMultitouchHelper = if (hasDistinctMultitouch) null else NonDistinctMultitouchHelper()
        val backgroundDimAlpha = mainKeyboardViewAttr.getInt(R.styleable.MainKeyboardView_backgroundDimAlpha, 0)
        mBackgroundDimAlphaPaint.color = Color.BLACK; mBackgroundDimAlphaPaint.alpha = backgroundDimAlpha
        mLanguageOnSpacebarTextRatio = mainKeyboardViewAttr.getFraction(R.styleable.MainKeyboardView_languageOnSpacebarTextRatio, 1, 1, 1f)
        mLanguageOnSpacebarTextColor = mainKeyboardViewAttr.getColor(R.styleable.MainKeyboardView_languageOnSpacebarTextColor, 0)
        mLanguageOnSpacebarFinalAlpha = mainKeyboardViewAttr.getInt(R.styleable.MainKeyboardView_languageOnSpacebarFinalAlpha, Constants.Color.ALPHA_OPAQUE)
        val altCodeKeyWhileTypingFadeoutAnimatorResId = mainKeyboardViewAttr.getResourceId(R.styleable.MainKeyboardView_altCodeKeyWhileTypingFadeoutAnimator, 0)
        val altCodeKeyWhileTypingFadeinAnimatorResId = mainKeyboardViewAttr.getResourceId(R.styleable.MainKeyboardView_altCodeKeyWhileTypingFadeinAnimator, 0)
        mKeyPreviewDrawParams = KeyPreviewDrawParams(mainKeyboardViewAttr)
        mKeyPreviewChoreographer = KeyPreviewChoreographer(mKeyPreviewDrawParams)
        val moreKeysKeyboardLayoutId = mainKeyboardViewAttr.getResourceId(R.styleable.MainKeyboardView_moreKeysKeyboardLayout, 0)
        mConfigShowMoreKeysKeyboardAtTouchedPoint = mainKeyboardViewAttr.getBoolean(R.styleable.MainKeyboardView_showMoreKeysKeyboardAtTouchedPoint, false)
        mainKeyboardViewAttr.recycle()
        mDrawingPreviewPlacerView = drawingPreviewPlacerView
        val inflater = LayoutInflater.from(context)
        mMoreKeysKeyboardContainer = inflater.inflate(moreKeysKeyboardLayoutId, null)
        mAltCodeKeyWhileTypingFadeoutAnimator = loadObjectAnimator(altCodeKeyWhileTypingFadeoutAnimatorResId, this)
        mAltCodeKeyWhileTypingFadeinAnimator = loadObjectAnimator(altCodeKeyWhileTypingFadeinAnimatorResId, this)
        mLanguageOnSpacebarHorizontalMargin = resources.getDimension(R.dimen.config_language_on_spacebar_horizontal_margin).toInt()
    }

    private fun loadObjectAnimator(resId: Int, target: Any): ObjectAnimator? {
        if (resId == 0) return null
        val animator = AnimatorInflater.loadAnimator(context, resId) as? ObjectAnimator ?: return null
        animator.setTarget(target); return animator
    }

    override fun startWhileTypingAnimation(fadeInOrOut: Int) {
        when (fadeInOrOut) {
            DrawingProxy.FADE_IN -> cancelAndStartAnimators(mAltCodeKeyWhileTypingFadeoutAnimator, mAltCodeKeyWhileTypingFadeinAnimator)
            DrawingProxy.FADE_OUT -> cancelAndStartAnimators(mAltCodeKeyWhileTypingFadeinAnimator, mAltCodeKeyWhileTypingFadeoutAnimator)
        }
    }

    fun setKeyboardActionListener(listener: KeyboardActionListener) { mKeyboardActionListener = listener; PointerTracker.setKeyboardActionListener(listener) }

    override fun getKeyPreviewDrawParams(): KeyPreviewDrawParams = mKeyPreviewDrawParams
    override fun getKeyPreviewChoreographer(): KeyPreviewChoreographer = mKeyPreviewChoreographer
    override fun getKeyDrawParams(): KeyDrawParams = super.getKeyDrawParams()

    override fun setKeyboard(keyboard: Keyboard) {
        mTimerHandler.cancelLongPressTimers()
        super.setKeyboard(keyboard)
        mKeyDetector.setKeyboard(keyboard, -paddingLeft.toFloat(), (-paddingTop + verticalCorrection).toFloat())
        PointerTracker.setKeyDetector(mKeyDetector); mMoreKeysKeyboardCache.clear()
        mSpaceKey = keyboard.getKey(Constants.CODE_SPACE)
        mLanguageOnSpacebarTextSize = keyboard.mMostCommonKeyHeight * mLanguageOnSpacebarTextRatio
    }

    fun setKeyPreviewPopupEnabled(previewEnabled: Boolean, delay: Int) { mKeyPreviewDrawParams.setPopupEnabled(previewEnabled, delay) }

    private fun locatePreviewPlacerView() { getLocationInWindow(mOriginCoords); mDrawingPreviewPlacerView.setKeyboardViewGeometry(mOriginCoords) }

    private fun installPreviewPlacerView() {
        val rootView = getRootView() ?: run { Log.w(TAG, "Cannot find root view"); return }
        val windowContentView = rootView.findViewById<ViewGroup>(android.R.id.content) ?: run { Log.w(TAG, "Cannot find android.R.id.content view to add DrawingPreviewPlacerView"); return }
        windowContentView.addView(mDrawingPreviewPlacerView)
    }

    override fun onKeyPressed(key: Key, withPreview: Boolean) { key.onPressed(); invalidateKey(key); if (withPreview && !key.noKeyPreview) showKeyPreview(key) }

    private fun showKeyPreview(key: Key) {
        val keyboard = getKeyboard() ?: return
        val previewParams = mKeyPreviewDrawParams
        if (!previewParams.isPopupEnabled()) { previewParams.setVisibleOffset(-Math.round(keyboard.mVerticalGap)); return }
        locatePreviewPlacerView(); getLocationInWindow(mOriginCoords)
        val backgroundColor = if (mTheme?.mCustomColorSupport == true) mCustomColor else Color.TRANSPARENT
        mKeyPreviewChoreographer.placeAndShowKeyPreview(key, keyboard.mIconsSet, getKeyDrawParams(), mOriginCoords, mDrawingPreviewPlacerView, isHardwareAccelerated, backgroundColor)
    }

    private fun dismissKeyPreviewWithoutDelay(key: Key) { mKeyPreviewChoreographer.dismissKeyPreview(key, false); invalidateKey(key) }

    override fun onKeyReleased(key: Key, withAnimation: Boolean) {
        key.onReleased(); invalidateKey(key)
        if (!key.noKeyPreview) { if (withAnimation) dismissKeyPreview(key) else dismissKeyPreviewWithoutDelay(key) }
    }

    private fun dismissKeyPreview(key: Key) {
        if (isHardwareAccelerated) { mKeyPreviewChoreographer.dismissKeyPreview(key, true); return }
        mTimerHandler.postDismissKeyPreview(key, mKeyPreviewDrawParams.getLingerTimeout())
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); installPreviewPlacerView() }
    override fun onDetachedFromWindow() { super.onDetachedFromWindow(); mDrawingPreviewPlacerView.removeAllViews() }

    override fun showMoreKeysKeyboard(key: Key, tracker: PointerTracker): MoreKeysPanel? {
        val moreKeys = key.getMoreKeys() ?: return null
        var moreKeysKeyboard = mMoreKeysKeyboardCache[key]
        if (moreKeysKeyboard == null) {
            val isSingleMoreKeyWithPreview = mKeyPreviewDrawParams.isPopupEnabled() && !key.noKeyPreview && moreKeys.size == 1 && mKeyPreviewDrawParams.getVisibleWidth() > 0
            val builder = MoreKeysKeyboard.Builder(context, key, getKeyboard()!!, isSingleMoreKeyWithPreview, mKeyPreviewDrawParams.getVisibleWidth(), mKeyPreviewDrawParams.getVisibleHeight(), newLabelPaint(key))
            moreKeysKeyboard = builder.build(); mMoreKeysKeyboardCache[key] = moreKeysKeyboard
        }
        val moreKeysKeyboardView = mMoreKeysKeyboardContainer.findViewById<MoreKeysKeyboardView>(R.id.more_keys_keyboard_view)
        moreKeysKeyboardView.keyboard = moreKeysKeyboard
        mMoreKeysKeyboardContainer.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val lastCoords = CoordinateUtils.newInstance(); tracker.getLastCoordinates(lastCoords)
        val keyPreviewEnabled = mKeyPreviewDrawParams.isPopupEnabled() && !key.noKeyPreview
        val pointX = if (mConfigShowMoreKeysKeyboardAtTouchedPoint && !keyPreviewEnabled) CoordinateUtils.x(lastCoords) else key.getX() + key.width / 2
        val pointY = key.y + mKeyPreviewDrawParams.getVisibleOffset() + Math.round(moreKeysKeyboard.mBottomPadding)
        moreKeysKeyboardView.showMoreKeysPanel(this, this, pointX, pointY, mKeyboardActionListener)
        return moreKeysKeyboardView
    }

    fun isInDraggingFinger(): Boolean = isShowingMoreKeysPanel() || PointerTracker.isAnyInDraggingFinger()
    fun isInCursorMove(): Boolean = PointerTracker.isAnyInCursorMove()

    override fun onShowMoreKeysPanel(panel: MoreKeysPanel) { locatePreviewPlacerView(); onDismissMoreKeysPanel(); PointerTracker.setReleasedKeyGraphicsToAllKeys(); panel.showInParent(mDrawingPreviewPlacerView); mMoreKeysPanel = panel }
    fun isShowingMoreKeysPanel(): Boolean = mMoreKeysPanel?.isShowingInParent() == true
    override fun onCancelMoreKeysPanel() { PointerTracker.dismissAllMoreKeysPanels() }
    override fun onDismissMoreKeysPanel() { if (isShowingMoreKeysPanel()) { mMoreKeysPanel?.removeFromParent(); mMoreKeysPanel = null } }

    fun startDoubleTapShiftKeyTimer() { mTimerHandler.startDoubleTapShiftKeyTimer() }
    fun cancelDoubleTapShiftKeyTimer() { mTimerHandler.cancelDoubleTapShiftKeyTimer() }
    fun isInDoubleTapShiftKeyTimeout(): Boolean = mTimerHandler.isInDoubleTapShiftKeyTimeout()

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (getKeyboard() == null) return false
        if (mNonDistinctMultitouchHelper != null) {
            if (event.pointerCount > 1 && mTimerHandler.isInKeyRepeat()) mTimerHandler.cancelKeyRepeatTimers()
            mNonDistinctMultitouchHelper.processMotionEvent(event, mKeyDetector); return true
        }
        return processMotionEvent(event)
    }

    fun processMotionEvent(event: MotionEvent): Boolean {
        val index = event.actionIndex; val id = event.getPointerId(index)
        val tracker = PointerTracker.getPointerTracker(id)
        if (isShowingMoreKeysPanel() && !tracker.isShowingMoreKeysPanel() && PointerTracker.getActivePointerTrackerCount() == 1) return true
        tracker.processMotionEvent(event, mKeyDetector); return true
    }

    fun cancelAllOngoingEvents() { mTimerHandler.cancelAllMessages(); PointerTracker.setReleasedKeyGraphicsToAllKeys(); PointerTracker.dismissAllMoreKeysPanels(); PointerTracker.cancelAllPointerTrackers() }
    fun closing() { cancelAllOngoingEvents(); mMoreKeysKeyboardCache.clear(); dismissClipboardHistoryPanel() }

    fun toggleClipboardHistoryPanel() {
        if (mClipboardHistoryView?.isShowing == true) dismissClipboardHistoryPanel() else showClipboardHistoryPanel()
    }

    private fun showClipboardHistoryPanel() {
        if (mClipboardHistoryView == null) { mClipboardHistoryView = LayoutInflater.from(context).inflate(R.layout.clipboard_history_panel, mDrawingPreviewPlacerView, false) as ClipboardHistoryView; mClipboardHistoryView!!.setListener(mKeyboardActionListener) }
        locatePreviewPlacerView()
        val keyboard = getKeyboard()
        mClipboardHistoryView!!.show(mDrawingPreviewPlacerView)
        if (keyboard != null) { val height = keyboard.mOccupiedHeight + paddingTop; mClipboardHistoryView!!.post { mClipboardHistoryView?.y = (height - mClipboardHistoryView!!.measuredHeight).toFloat() } }
    }

    private fun dismissClipboardHistoryPanel() { mClipboardHistoryView?.dismiss() }
    fun onHideWindow() { onDismissMoreKeysPanel() }

    fun startDisplayLanguageOnSpacebar(subtypeChanged: Boolean, languageOnSpacebarFormatType: Int) {
        if (subtypeChanged) KeyPreviewView.clearTextCache()
        mLanguageOnSpacebarFormatType = languageOnSpacebarFormatType; invalidateKey(mSpaceKey)
    }

    override fun onDrawKeyTopVisuals(key: Key, canvas: Canvas, paint: Paint, params: KeyDrawParams) {
        if (key.altCodeWhileTyping()) params.mAnimAlpha = mAltCodeKeyWhileTypingAnimAlpha
        super.onDrawKeyTopVisuals(key, canvas, paint, params)
        if (key.getCode() == Constants.CODE_SPACE) { val imm = RichInputMethodManager.getInstance(); if (imm.hasMultipleEnabledSubtypes()) drawLanguageOnSpacebar(key, canvas, paint) }
    }

    private fun fitsTextIntoWidth(width: Int, text: String, paint: Paint): Boolean {
        val maxTextWidth = width - mLanguageOnSpacebarHorizontalMargin * 2; paint.textScaleX = 1f
        val textWidth = TypefaceUtils.getStringWidth(text, paint)
        if (textWidth < width) return true
        val scaleX = maxTextWidth / textWidth; if (scaleX < MINIMUM_XSCALE_OF_LANGUAGE_NAME) return false
        paint.textScaleX = scaleX; return TypefaceUtils.getStringWidth(text, paint) < maxTextWidth
    }

    private fun layoutLanguageOnSpacebar(paint: Paint, subtype: Subtype, width: Int): String {
        if (mLanguageOnSpacebarFormatType == LanguageOnSpacebarUtils.FORMAT_TYPE_FULL_LOCALE) { val fullText = LocaleResourceUtils.getLocaleDisplayNameInLocale(subtype.locale); if (fitsTextIntoWidth(width, fullText, paint)) return fullText }
        val middleText = LocaleResourceUtils.getLanguageDisplayNameInLocale(subtype.locale)
        return if (fitsTextIntoWidth(width, middleText, paint)) middleText else ""
    }

    private fun drawLanguageOnSpacebar(key: Key, canvas: Canvas, paint: Paint) {
        val keyboard = getKeyboard() ?: return; val width = key.width; val height = key.height
        paint.textAlign = Paint.Align.CENTER; paint.typeface = Typeface.DEFAULT; paint.textSize = mLanguageOnSpacebarTextSize
        val language = layoutLanguageOnSpacebar(paint, keyboard.mId.mSubtype, width)
        val descent = paint.descent(); val textHeight = -paint.ascent() + descent; val baseline = height / 2 + textHeight / 2
        paint.color = mLanguageOnSpacebarTextColor; paint.alpha = mLanguageOnSpacebarFinalAlpha
        canvas.drawText(language, (width / 2).toFloat(), baseline - descent, paint)
        paint.clearShadowLayer(); paint.textScaleX = 1f
    }

    companion object {
        private const val TAG = "MainKeyboardView"
        private const val MINIMUM_XSCALE_OF_LANGUAGE_NAME = 0.8f
        private fun cancelAndStartAnimators(animatorToCancel: ObjectAnimator?, animatorToStart: ObjectAnimator?) {
            if (animatorToCancel == null || animatorToStart == null) return
            var startFraction = 0f
            if (animatorToCancel.isStarted) { animatorToCancel.cancel(); startFraction = 1f - animatorToCancel.animatedFraction }
            val startTime = (animatorToStart.duration * startFraction).toLong(); animatorToStart.start(); animatorToStart.currentPlayTime = startTime
        }
    }
}
