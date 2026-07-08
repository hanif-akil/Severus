package rkr.simplekeyboard.inputmethod.keyboard

import android.content.res.TypedArray
import android.util.Log
import android.view.MotionEvent
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.keyboard.internal.BogusMoveEventDetector
import rkr.simplekeyboard.inputmethod.keyboard.internal.DrawingProxy
import rkr.simplekeyboard.inputmethod.keyboard.internal.PointerTrackerQueue
import rkr.simplekeyboard.inputmethod.keyboard.internal.TimerProxy
import rkr.simplekeyboard.inputmethod.latin.common.Constants
import rkr.simplekeyboard.inputmethod.latin.common.CoordinateUtils
import rkr.simplekeyboard.inputmethod.latin.define.DebugFlags
import rkr.simplekeyboard.inputmethod.latin.settings.Settings
import kotlin.math.hypot
import kotlin.math.round

class PointerTracker private constructor(val mPointerId: Int) : PointerTrackerQueue.Element {
    private var mKeyDetector: KeyDetector = KeyDetector()
    private var mKeyboard: Keyboard? = null
    private val mBogusMoveEventDetector = BogusMoveEventDetector()
    private val mDownCoordinates = CoordinateUtils.newInstance()
    var mCurrentKey: Key? = null
        private set
    private var mKeyX = 0
    private var mKeyY = 0
    private var mLastX = 0
    private var mLastY = 0
    private var mStartX = 0
    private var mStartTime: Long = 0
    private var mCursorMoved = false
    private var mKeyboardLayoutHasBeenChanged = false
    private var mIsTrackingForActionDisabled = false
    private var mMoreKeysPanel: MoreKeysPanel? = null
    var mIsInDraggingFinger = false
        internal set
    var mIsInSlidingKeyInput = false
        internal set
    private var mCurrentRepeatingKeyCode = Constants.NOT_A_CODE
    private var mIsAllowedDraggingFinger = false

    override fun isInDraggingFinger(): Boolean = mIsInDraggingFinger
    override fun isInCursorMove(): Boolean = mCursorMoved
    override fun isModifier(): Boolean = mCurrentKey?.isModifier == true

    fun getKey(): Key? = mCurrentKey
    fun getKeyOn(x: Int, y: Int): Key? = mKeyDetector.detectHitKey(x, y)

    private fun setReleasedKeyGraphics(key: Key?, withAnimation: Boolean) {
        if (key == null) return
        sDrawingProxy.onKeyReleased(key, withAnimation)
        if (key.isShift) {
            for (shiftKey in mKeyboard!!.mShiftKeys) {
                if (shiftKey != key) sDrawingProxy.onKeyReleased(shiftKey, false)
            }
        }
        if (key.altCodeWhileTyping) {
            val altCode = key.getAltCode()
            val altKey = mKeyboard!!.getKey(altCode)
            if (altKey != null) sDrawingProxy.onKeyReleased(altKey, false)
            for (k in mKeyboard!!.mAltCodeKeysWhileTyping) {
                if (k != key && k.getAltCode() == altCode) sDrawingProxy.onKeyReleased(k, false)
            }
        }
    }

    private fun setPressedKeyGraphics(key: Key?) {
        if (key == null) return
        sDrawingProxy.onKeyPressed(key, true)
        if (key.isShift) {
            for (shiftKey in mKeyboard!!.mShiftKeys) {
                if (shiftKey != key) sDrawingProxy.onKeyPressed(shiftKey, false)
            }
        }
        if (key.altCodeWhileTyping && sTimerProxy.isTypingState()) {
            val altCode = key.getAltCode()
            val altKey = mKeyboard!!.getKey(altCode)
            if (altKey != null) sDrawingProxy.onKeyPressed(altKey, false)
            for (k in mKeyboard!!.mAltCodeKeysWhileTyping) {
                if (k != key && k.getAltCode() == altCode) sDrawingProxy.onKeyPressed(k, false)
            }
        }
    }

    fun getLastCoordinates(outCoords: IntArray) {
        CoordinateUtils.set(outCoords, mLastX, mLastY)
    }

    private fun onDownKey(x: Int, y: Int): Key? {
        CoordinateUtils.set(mDownCoordinates, x, y)
        mBogusMoveEventDetector.onActualDownEvent(x, y)
        return onMoveToNewKey(onMoveKeyInternal(x, y), x, y)
    }

    private fun onMoveKeyInternal(x: Int, y: Int): Key? {
        mBogusMoveEventDetector.onMoveKey(getDistance(x, y, mLastX, mLastY))
        mLastX = x
        mLastY = y
        return mKeyDetector.detectHitKey(x, y)
    }

    private fun onMoveToNewKey(newKey: Key?, x: Int, y: Int): Key? {
        mCurrentKey = newKey
        mKeyX = x
        mKeyY = y
        return newKey
    }

    fun processMotionEvent(me: MotionEvent, keyDetector: KeyDetector) {
        val action = me.actionMasked
        val eventTime = me.eventTime
        if (action == MotionEvent.ACTION_MOVE) {
            val shouldIgnoreOtherPointers = isShowingMoreKeysPanel() && getActivePointerTrackerCount() == 1
            val pointerCount = me.pointerCount
            for (index in 0 until pointerCount) {
                val id = me.getPointerId(index)
                if (shouldIgnoreOtherPointers && id != mPointerId) continue
                val x = me.getX(index).toInt()
                val y = me.getY(index).toInt()
                getPointerTracker(id).onMoveEvent(x, y, eventTime)
            }
            return
        }
        val index = me.actionIndex
        val x = me.getX(index).toInt()
        val y = me.getY(index).toInt()
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> onDownEvent(x, y, eventTime, keyDetector)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> onUpEvent(x, y, eventTime)
            MotionEvent.ACTION_CANCEL -> onCancelEvent(x, y, eventTime)
        }
    }

    private fun onDownEvent(x: Int, y: Int, eventTime: Long, keyDetector: KeyDetector) {
        setKeyDetectorInner(keyDetector)
        val key = getKeyOn(x, y)
        if (key != null && key.isModifier) sPointerTrackerQueue.releaseAllPointers(eventTime)
        sPointerTrackerQueue.add(this)
        onDownEventInternal(x, y)
    }

    private fun onDownEventInternal(x: Int, y: Int) {
        var key = onDownKey(x, y)
        mIsAllowedDraggingFinger = sParams.mKeySelectionByDraggingFinger || (key?.isModifier == true) || mKeyDetector.alwaysAllowsKeySelectionByDraggingFinger()
        mKeyboardLayoutHasBeenChanged = false
        mIsTrackingForActionDisabled = false
        resetKeySelectionByDraggingFinger()
        if (key != null) {
            if (callListenerOnPressAndCheckKeyboardLayoutChange(key, 0)) {
                key = onDownKey(x, y)
            }
            startRepeatKey(key)
            startLongPressTimer(key)
            setPressedKeyGraphics(key)
            mStartX = x
            mStartTime = System.currentTimeMillis()
        }
    }

    private fun onMoveEvent(x: Int, y: Int, eventTime: Long) {
        if (mIsTrackingForActionDisabled) return
        if (isShowingMoreKeysPanel()) {
            mMoreKeysPanel?.onMoveEvent(mMoreKeysPanel!!.translateX(x), mMoreKeysPanel!!.translateY(y), mPointerId)
            onMoveKeyInternal(x, y)
            return
        }
        onMoveEventInternal(x, y, eventTime)
    }

    private fun onMoveEventInternal(x: Int, y: Int, eventTime: Long) {
        val oldKey = mCurrentKey
        val settings = Settings.getInstance().current
        if (oldKey != null && oldKey.getCode() == Constants.CODE_SPACE && settings!!.mSpaceSwipeEnabled) {
            val steps = (x - mStartX) / sPointerStep
            val swipeIgnoreTime = settings.mKeyLongpressTimeout / MULTIPLIER_FOR_LONG_PRESS_TIMEOUT_IN_SLIDING_INPUT
            if (steps != 0 && mStartTime + swipeIgnoreTime < System.currentTimeMillis()) {
                mCursorMoved = true
                mStartX += steps * sPointerStep
                sListener.onMoveCursorPointer(steps)
            }
            return
        }
        if (oldKey != null && oldKey.getCode() == Constants.CODE_DELETE && settings!!.mDeleteSwipeEnabled) {
            val steps = (x - mStartX) / sPointerStep
            if (steps != 0) {
                sTimerProxy.cancelKeyTimersOf(this)
                mCursorMoved = true
                mStartX += steps * sPointerStep
                sListener.onMoveDeletePointer(steps)
            }
            return
        }
        val newKey = onMoveKeyInternal(x, y)
        if (newKey != null) {
            if (oldKey != null && isMajorEnoughMoveToBeOnNewKey(x, y, newKey)) {
                processDraggingFingerOutFromOldKey(oldKey)
                startRepeatKey(newKey)
                if (mIsAllowedDraggingFinger) {
                    processDraggingFingerInToNewKey(newKey, x, y)
                } else if (getActivePointerTrackerCount() > 1 && !sPointerTrackerQueue.hasModifierKeyOlderThan(this)) {
                    onUpEvent(x, y, eventTime)
                    cancelTrackingForAction()
                    setReleasedKeyGraphics(oldKey, true)
                } else {
                    cancelTrackingForAction()
                    setReleasedKeyGraphics(oldKey, true)
                }
            } else if (oldKey == null) {
                processDraggingFingerInToNewKey(newKey, x, y)
            }
        } else if (oldKey != null && isMajorEnoughMoveToBeOnNewKey(x, y, newKey)) {
            processDraggingFingerOutFromOldKey(oldKey)
            if (mIsAllowedDraggingFinger) onMoveToNewKey(null, x, y) else cancelTrackingForAction()
        }
    }

    private fun processDraggingFingerInToNewKey(newKey: Key, x: Int, y: Int) {
        var key: Key? = newKey
        if (callListenerOnPressAndCheckKeyboardLayoutChange(key!!, 0)) {
            key = onMoveKeyInternal(x, y)
        }
        onMoveToNewKey(key, x, y)
        if (mIsTrackingForActionDisabled) return
        startLongPressTimer(key)
        setPressedKeyGraphics(key)
    }

    private fun processDraggingFingerOutFromOldKey(oldKey: Key) {
        setReleasedKeyGraphics(oldKey, true)
        sListener.onReleaseKey(oldKey.getCode(), true)
        if (!mIsInDraggingFinger) mIsInSlidingKeyInput = oldKey.isModifier
        mIsInDraggingFinger = true
        sTimerProxy.cancelKeyTimersOf(this)
    }

    private fun onUpEvent(x: Int, y: Int, eventTime: Long) {
        if (mCurrentKey?.isModifier == true) {
            sPointerTrackerQueue.releaseAllPointersExcept(this, eventTime)
        } else {
            sPointerTrackerQueue.releaseAllPointersOlderThan(this, eventTime)
        }
        onUpEventInternal(x, y)
        sPointerTrackerQueue.remove(this)
    }

    override fun onPhantomUpEvent(eventTime: Long) {
        onUpEventInternal(mLastX, mLastY)
        cancelTrackingForAction()
    }

    private fun onUpEventInternal(x: Int, y: Int) {
        sTimerProxy.cancelKeyTimersOf(this)
        val isInDraggingFinger = mIsInDraggingFinger
        val isInSlidingKeyInput = mIsInSlidingKeyInput
        resetKeySelectionByDraggingFinger()
        val currentKey = mCurrentKey
        mCurrentKey = null
        val currentRepeatingKeyCode = mCurrentRepeatingKeyCode
        mCurrentRepeatingKeyCode = Constants.NOT_A_CODE
        setReleasedKeyGraphics(currentKey, true)
        if (mCursorMoved && currentKey?.getCode() == Constants.CODE_DELETE) sListener.onUpWithDeletePointerActive()
        if (mCursorMoved && currentKey?.getCode() == Constants.CODE_SPACE) sListener.onUpWithSpacePointerActive()
        if (isShowingMoreKeysPanel()) {
            if (!mIsTrackingForActionDisabled) {
                mMoreKeysPanel?.onUpEvent(mMoreKeysPanel!!.translateX(x), mMoreKeysPanel!!.translateY(y), mPointerId)
            }
            dismissMoreKeysPanel()
            return
        }
        if (mCursorMoved) { mCursorMoved = false; return }
        if (mIsTrackingForActionDisabled) return
        if (currentKey != null && currentKey.isRepeatable && currentKey.getCode() == currentRepeatingKeyCode && !isInDraggingFinger) return
        detectAndSendKey(currentKey, mKeyX, mKeyY)
        if (isInSlidingKeyInput) sListener.onFinishSlidingInput()
    }

    override fun cancelTrackingForAction() {
        if (isShowingMoreKeysPanel()) return
        mIsTrackingForActionDisabled = true
    }

    fun onLongPressed() {
        sTimerProxy.cancelLongPressTimersOf(this)
        if (isShowingMoreKeysPanel() || mCursorMoved) return
        val key = getKey() ?: return
        if (key.hasNoPanelAutoMoreKey()) {
            cancelKeyTracking()
            val moreKeyCode = key.getMoreKeys()!![0].mCode
            sListener.onPressKey(moreKeyCode, 0, true)
            sListener.onCodeInput(moreKeyCode, Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE, false)
            sListener.onReleaseKey(moreKeyCode, false)
            return
        }
        val code = key.getCode()
        if (code == Constants.CODE_SPACE || code == Constants.CODE_LANGUAGE_SWITCH) {
            if (sListener.onCustomRequest(Constants.CUSTOM_CODE_SHOW_INPUT_METHOD_PICKER)) {
                cancelKeyTracking()
                sListener.onReleaseKey(code, false)
                return
            }
        }
        setReleasedKeyGraphics(key, false)
        val moreKeysPanel = sDrawingProxy.showMoreKeysKeyboard(key, this) ?: return
        moreKeysPanel.onDownEvent(moreKeysPanel.translateX(mLastX), moreKeysPanel.translateY(mLastY), mPointerId)
        mMoreKeysPanel = moreKeysPanel
    }

    private fun cancelKeyTracking() {
        resetKeySelectionByDraggingFinger()
        cancelTrackingForAction()
        setReleasedKeyGraphics(mCurrentKey, true)
        sPointerTrackerQueue.remove(this)
    }

    private fun onCancelEvent(x: Int, y: Int, eventTime: Long) {
        cancelAllPointerTrackers()
        sPointerTrackerQueue.releaseAllPointers(eventTime)
        sTimerProxy.cancelKeyTimersOf(this)
        setReleasedKeyGraphics(mCurrentKey, true)
        resetKeySelectionByDraggingFinger()
        dismissMoreKeysPanel()
    }

    private fun isMajorEnoughMoveToBeOnNewKey(x: Int, y: Int, newKey: Key?): Boolean {
        val curKey = mCurrentKey
        if (newKey == curKey) return false
        if (curKey == null) return true
        val keyHysteresisDistanceSquared = mKeyDetector.getKeyHysteresisDistanceSquared(mIsInSlidingKeyInput)
        val distanceFromKeyEdgeSquared = curKey.squaredDistanceToHitboxEdge(x, y)
        if (distanceFromKeyEdgeSquared >= keyHysteresisDistanceSquared) return true
        if (!mIsAllowedDraggingFinger && mBogusMoveEventDetector.hasTraveledLongDistance(x, y)) return true
        return false
    }

    private fun startLongPressTimer(key: Key?) {
        sTimerProxy.cancelLongPressShiftKeyTimer()
        if (key == null || !key.isLongPressEnabled) return
        if (mIsInDraggingFinger && key.getMoreKeys() == null) return
        val delay = getLongPressTimeout(key.getCode())
        if (delay <= 0) return
        sTimerProxy.startLongPressTimerOf(this, delay)
    }

    private fun getLongPressTimeout(code: Int): Int {
        if (code == Constants.CODE_SHIFT) return sParams.mLongPressShiftLockTimeout
        val longpressTimeout = Settings.getInstance().current!!.mKeyLongpressTimeout
        if (mIsInSlidingKeyInput || code == Constants.CODE_SPACE) return longpressTimeout * MULTIPLIER_FOR_LONG_PRESS_TIMEOUT_IN_SLIDING_INPUT
        return longpressTimeout
    }

    private fun detectAndSendKey(key: Key?, x: Int, y: Int) {
        if (key == null) return
        val code = key.getCode()
        callListenerOnCodeInput(key, code, x, y, false)
        sListener.onReleaseKey(code, false)
    }

    private fun startRepeatKey(key: Key?) {
        if (key == null || !key.isRepeatable || mIsInDraggingFinger) return
        startKeyRepeatTimer(1)
    }

    fun onKeyRepeat(code: Int, repeatCount: Int) {
        val key = getKey()
        if (key == null || key.getCode() != code) { mCurrentRepeatingKeyCode = Constants.NOT_A_CODE; return }
        mCurrentRepeatingKeyCode = code
        startKeyRepeatTimer(repeatCount + 1)
        callListenerOnPressAndCheckKeyboardLayoutChange(key, repeatCount)
        callListenerOnCodeInput(key, code, mKeyX, mKeyY, true)
    }

    private fun startKeyRepeatTimer(repeatCount: Int) {
        val delay = if (repeatCount == 1) sParams.mKeyRepeatStartTimeout else sParams.mKeyRepeatInterval
        sTimerProxy.startKeyRepeatTimerOf(this, repeatCount, delay)
    }

    private fun callListenerOnPressAndCheckKeyboardLayoutChange(key: Key, repeatCount: Int): Boolean {
        val ignoreModifierKey = mIsInDraggingFinger && key.isModifier
        if (ignoreModifierKey) return false
        sListener.onPressKey(key.getCode(), repeatCount, getActivePointerTrackerCount() == 1)
        val result = mKeyboardLayoutHasBeenChanged
        mKeyboardLayoutHasBeenChanged = false
        sTimerProxy.startTypingStateTimer(key)
        return result
    }

    private fun callListenerOnCodeInput(key: Key, primaryCode: Int, x: Int, y: Int, isKeyRepeat: Boolean) {
        val ignoreModifierKey = mIsInDraggingFinger && key.isModifier
        val altersCode = key.altCodeWhileTyping && sTimerProxy.isTypingState()
        val code = if (altersCode) key.getAltCode() else primaryCode
        if (ignoreModifierKey) return
        if (code == Constants.CODE_OUTPUT_TEXT) sListener.onTextInput(key.getOutputText()!!)
        else if (code != Constants.CODE_UNSPECIFIED) sListener.onCodeInput(code, Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE, isKeyRepeat)
    }

    private fun setKeyDetectorInner(keyDetector: KeyDetector) {
        val keyboard = keyDetector.getKeyboard() ?: return
        if (keyDetector == mKeyDetector && keyboard == mKeyboard) return
        mKeyDetector = keyDetector
        mKeyboard = keyboard
        mKeyboardLayoutHasBeenChanged = true
        val keyPaddedWidth = keyboard.mMostCommonKeyWidth + round(keyboard.mHorizontalGap).toInt()
        val keyPaddedHeight = keyboard.mMostCommonKeyHeight + round(keyboard.mVerticalGap).toInt()
        mBogusMoveEventDetector.setKeyboardGeometry(keyPaddedWidth, keyPaddedHeight)
    }

    fun isShowingMoreKeysPanel(): Boolean = mMoreKeysPanel != null

    private fun dismissMoreKeysPanel() {
        if (isShowingMoreKeysPanel()) {
            mMoreKeysPanel?.dismissMoreKeysPanel()
            mMoreKeysPanel = null
        }
    }

    private fun resetKeySelectionByDraggingFinger() {
        mIsInDraggingFinger = false
        mIsInSlidingKeyInput = false
    }

    companion object {
        private const val TAG = "PointerTracker"
        private const val DEBUG_EVENT = false
        private const val DEBUG_MOVE_EVENT = false
        private const val DEBUG_LISTENER = false
        private val DEBUG_MODE = DebugFlags.DEBUG_ENABLED || DEBUG_EVENT
        private const val MULTIPLIER_FOR_LONG_PRESS_TIMEOUT_IN_SLIDING_INPUT = 3

        class PointerTrackerParams(mainKeyboardViewAttr: TypedArray) {
            val mKeySelectionByDraggingFinger: Boolean = mainKeyboardViewAttr.getBoolean(R.styleable.MainKeyboardView_keySelectionByDraggingFinger, false)
            val mTouchNoiseThresholdTime: Int = mainKeyboardViewAttr.getInt(R.styleable.MainKeyboardView_touchNoiseThresholdTime, 0)
            val mTouchNoiseThresholdDistance: Int = mainKeyboardViewAttr.getDimensionPixelSize(R.styleable.MainKeyboardView_touchNoiseThresholdDistance, 0)
            val mKeyRepeatStartTimeout: Int = mainKeyboardViewAttr.getInt(R.styleable.MainKeyboardView_keyRepeatStartTimeout, 0)
            val mKeyRepeatInterval: Int = mainKeyboardViewAttr.getInt(R.styleable.MainKeyboardView_keyRepeatInterval, 0)
            val mLongPressShiftLockTimeout: Int = mainKeyboardViewAttr.getInt(R.styleable.MainKeyboardView_longPressShiftLockTimeout, 0)
        }

        private lateinit var sParams: PointerTrackerParams
        private var sPointerStep = (10.0 * android.content.res.Resources.getSystem().displayMetrics.density).toInt()
        private val sTrackers = ArrayList<PointerTracker>()
        private val sPointerTrackerQueue = PointerTrackerQueue()
        private lateinit var sDrawingProxy: DrawingProxy
        private lateinit var sTimerProxy: TimerProxy
        private var sListener: KeyboardActionListener = KeyboardActionListener.EMPTY_LISTENER

        fun init(mainKeyboardViewAttr: TypedArray, timerProxy: TimerProxy, drawingProxy: DrawingProxy) {
            sParams = PointerTrackerParams(mainKeyboardViewAttr)
            sTimerProxy = timerProxy
            sDrawingProxy = drawingProxy
        }

        fun getPointerTracker(id: Int): PointerTracker {
            for (i in sTrackers.size..id) {
                sTrackers.add(PointerTracker(i))
            }
            return sTrackers[id]
        }

        fun isAnyInDraggingFinger(): Boolean = sPointerTrackerQueue.isAnyInDraggingFinger()
        fun isAnyInCursorMove(): Boolean = sPointerTrackerQueue.isAnyInCursorMove()
        fun cancelAllPointerTrackers() { sPointerTrackerQueue.cancelAllPointerTrackers() }
        fun setKeyboardActionListener(listener: KeyboardActionListener) { sListener = listener }

        fun setKeyDetector(keyDetector: KeyDetector) {
            val keyboard = keyDetector.getKeyboard() ?: return
            for (tracker in sTrackers) tracker.setKeyDetectorInner(keyDetector)
        }

        fun setReleasedKeyGraphicsToAllKeys() {
            for (tracker in sTrackers) tracker.setReleasedKeyGraphics(tracker.getKey(), true)
        }

        fun dismissAllMoreKeysPanels() {
            for (tracker in sTrackers) tracker.dismissMoreKeysPanel()
        }

        fun getActivePointerTrackerCount(): Int = sPointerTrackerQueue.size

        private fun getDistance(x1: Int, y1: Int, x2: Int, y2: Int): Int = hypot((x1 - x2).toDouble(), (y1 - y2).toDouble()).toInt()
    }
}
