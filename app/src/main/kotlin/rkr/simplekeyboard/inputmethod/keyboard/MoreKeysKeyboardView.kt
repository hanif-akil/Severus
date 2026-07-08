package rkr.simplekeyboard.inputmethod.keyboard

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.latin.common.Constants
import rkr.simplekeyboard.inputmethod.latin.common.CoordinateUtils

open class MoreKeysKeyboardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = R.attr.moreKeysKeyboardViewStyle
) : KeyboardView(context, attrs, defStyle), MoreKeysPanel {

    private val mCoordinates = CoordinateUtils.newInstance()
    protected val mKeyDetector: KeyDetector
    private var mController: MoreKeysPanel.Controller = MoreKeysPanel.EMPTY_CONTROLLER
    protected var mListener: KeyboardActionListener? = null
    private var mOriginX = 0
    private var mOriginY = 0
    private var mCurrentKey: Key? = null
    private var mActivePointerId = 0

    init {
        mKeyDetector = MoreKeysDetector(resources.getDimension(R.dimen.config_more_keys_keyboard_slide_allowance))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val keyboard = keyboard
        if (keyboard != null) {
            val width = keyboard.mOccupiedWidth + paddingLeft + paddingRight
            val height = keyboard.mOccupiedHeight + paddingTop + paddingBottom
            setMeasuredDimension(width, height)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    override fun setKeyboard(keyboard: Keyboard) {
        super.setKeyboard(keyboard)
        mKeyDetector.setKeyboard(keyboard, -paddingLeft.toFloat(), (-paddingTop + verticalCorrection).toFloat())
    }

    override fun showMoreKeysPanel(parentView: View, controller: MoreKeysPanel.Controller, pointX: Int, pointY: Int, listener: KeyboardActionListener) {
        mController = controller
        mListener = listener
        val container = getContainerView()
        val x = pointX - getDefaultCoordX() - container.paddingLeft - paddingLeft
        val y = pointY - container.measuredHeight + container.paddingBottom + paddingBottom

        parentView.getLocationInWindow(mCoordinates)
        val maxX = parentView.measuredWidth - container.measuredWidth
        val panelX = maxOf(0, minOf(maxX, x)) + CoordinateUtils.x(mCoordinates)
        val panelY = y + CoordinateUtils.y(mCoordinates)
        container.x = panelX.toFloat()
        container.y = panelY.toFloat()

        mOriginX = x + container.paddingLeft
        mOriginY = y + container.paddingTop
        controller.onShowMoreKeysPanel(this)
    }

    protected open fun getDefaultCoordX(): Int = (keyboard as? MoreKeysKeyboard)?.getDefaultCoordX() ?: 0

    override fun onDownEvent(x: Int, y: Int, pointerId: Int) {
        mActivePointerId = pointerId
        mCurrentKey = detectKey(x, y)
    }

    override fun onMoveEvent(x: Int, y: Int, pointerId: Int) {
        if (mActivePointerId != pointerId) return
        val hasOldKey = mCurrentKey != null
        mCurrentKey = detectKey(x, y)
        if (hasOldKey && mCurrentKey == null) {
            mController.onCancelMoreKeysPanel()
        }
    }

    override fun onUpEvent(x: Int, y: Int, pointerId: Int) {
        if (mActivePointerId != pointerId) return
        mCurrentKey = detectKey(x, y)
        if (mCurrentKey != null) {
            updateReleaseKeyGraphics(mCurrentKey!!)
            onKeyInput(mCurrentKey!!)
            mCurrentKey = null
        }
    }

    protected open fun onKeyInput(key: Key) {
        val code = key.getCode()
        if (code == Constants.CODE_OUTPUT_TEXT) {
            mListener?.onTextInput(mCurrentKey!!.getOutputText()!!)
        } else if (code != Constants.CODE_UNSPECIFIED) {
            mListener?.onCodeInput(code, Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE, false)
        }
    }

    private fun detectKey(x: Int, y: Int): Key? {
        val oldKey = mCurrentKey
        val newKey = mKeyDetector.detectHitKey(x, y)
        if (newKey == oldKey) return newKey
        if (oldKey != null) {
            updateReleaseKeyGraphics(oldKey)
            invalidateKey(oldKey)
        }
        if (newKey != null) {
            updatePressKeyGraphics(newKey)
            invalidateKey(newKey)
        }
        return newKey
    }

    private fun updateReleaseKeyGraphics(key: Key) {
        key.onReleased()
        invalidateKey(key)
    }

    private fun updatePressKeyGraphics(key: Key) {
        key.onPressed()
        invalidateKey(key)
    }

    override fun dismissMoreKeysPanel() {
        if (!isShowingInParent()) return
        mController.onDismissMoreKeysPanel()
    }

    override fun translateX(x: Int): Int = x - mOriginX
    override fun translateY(y: Int): Int = y - mOriginY

    override fun onTouchEvent(me: MotionEvent): Boolean {
        val action = me.actionMasked
        val index = me.actionIndex
        val x = me.getX(index).toInt()
        val y = me.getY(index).toInt()
        val pointerId = me.getPointerId(index)
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> onDownEvent(x, y, pointerId)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> onUpEvent(x, y, pointerId)
            MotionEvent.ACTION_MOVE -> onMoveEvent(x, y, pointerId)
        }
        return true
    }

    private fun getContainerView(): View = parent as View

    override fun showInParent(parentView: ViewGroup) {
        removeFromParent()
        parentView.addView(getContainerView())
    }

    override fun removeFromParent() {
        val containerView = getContainerView()
        val currentParent = containerView.parent as? ViewGroup
        currentParent?.removeView(containerView)
    }

    override fun isShowingInParent(): Boolean = getContainerView().parent != null
}
