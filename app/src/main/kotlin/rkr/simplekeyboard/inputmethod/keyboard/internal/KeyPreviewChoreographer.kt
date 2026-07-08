package rkr.simplekeyboard.inputmethod.keyboard.internal

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import android.view.ViewGroup
import rkr.simplekeyboard.inputmethod.keyboard.Key
import rkr.simplekeyboard.inputmethod.keyboard.KeyboardIconsSet
import rkr.simplekeyboard.inputmethod.latin.common.CoordinateUtils
import rkr.simplekeyboard.inputmethod.latin.utils.ViewLayoutUtils
import java.util.ArrayDeque
import java.util.HashMap

class KeyPreviewChoreographer(private val mParams: KeyPreviewDrawParams) {
    private val mFreeKeyPreviewViews = ArrayDeque<KeyPreviewView>()
    private val mShowingKeyPreviewViews = HashMap<Key, KeyPreviewView>()

    fun getKeyPreviewView(key: Key, placerView: ViewGroup): KeyPreviewView {
        var keyPreviewView = mShowingKeyPreviewViews.remove(key)
        if (keyPreviewView != null) {
            keyPreviewView.scaleX = 1f
            keyPreviewView.scaleY = 1f
            return keyPreviewView
        }
        keyPreviewView = mFreeKeyPreviewViews.poll()
        if (keyPreviewView != null) {
            keyPreviewView.scaleX = 1f
            keyPreviewView.scaleY = 1f
            return keyPreviewView
        }
        val context = placerView.context
        keyPreviewView = KeyPreviewView(context, null)
        keyPreviewView.setBackgroundResource(mParams.mPreviewBackgroundResId)
        placerView.addView(keyPreviewView, ViewLayoutUtils.newLayoutParam(placerView, 0, 0))
        return keyPreviewView
    }

    fun dismissKeyPreview(key: Key?, withAnimation: Boolean) {
        if (key == null) return
        val keyPreviewView = mShowingKeyPreviewViews[key] ?: return
        val tag = keyPreviewView.tag
        if (withAnimation && tag is KeyPreviewAnimators) {
            tag.startDismiss()
            return
        }
        mShowingKeyPreviewViews.remove(key)
        if (tag is Animator) tag.cancel()
        keyPreviewView.tag = null
        keyPreviewView.visibility = View.INVISIBLE
        mFreeKeyPreviewViews.add(keyPreviewView)
    }

    fun placeAndShowKeyPreview(
        key: Key, iconsSet: KeyboardIconsSet, drawParams: KeyDrawParams,
        keyboardOrigin: IntArray, placerView: ViewGroup, withAnimation: Boolean,
        backgroundColor: Int
    ) {
        val keyPreviewView = getKeyPreviewView(key, placerView)
        placeKeyPreview(key, keyPreviewView, iconsSet, drawParams, keyboardOrigin, backgroundColor)
        showKeyPreview(key, keyPreviewView, withAnimation)
    }

    private fun placeKeyPreview(
        key: Key, keyPreviewView: KeyPreviewView, iconsSet: KeyboardIconsSet,
        drawParams: KeyDrawParams, originCoords: IntArray, backgroundColor: Int
    ) {
        keyPreviewView.setPreviewVisual(key, iconsSet, drawParams, backgroundColor)
        keyPreviewView.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        mParams.setGeometry(keyPreviewView)
        val previewWidth = maxOf(keyPreviewView.measuredWidth, mParams.mMinPreviewWidth)
        val previewHeight = mParams.mPreviewHeight
        val keyWidth = key.width
        var previewX = key.x - (previewWidth - keyWidth) / 2 + CoordinateUtils.x(originCoords)
        val previewY = key.y - previewHeight + mParams.mPreviewOffset + CoordinateUtils.y(originCoords)
        ViewLayoutUtils.placeViewAt(keyPreviewView, previewX, previewY, previewWidth, previewHeight)
    }

    fun showKeyPreview(key: Key, keyPreviewView: KeyPreviewView, withAnimation: Boolean) {
        if (!withAnimation) {
            keyPreviewView.visibility = View.VISIBLE
            mShowingKeyPreviewViews[key] = keyPreviewView
            return
        }
        val dismissAnimator = createDismissAnimator(key, keyPreviewView)
        val animators = KeyPreviewAnimators(dismissAnimator)
        keyPreviewView.tag = animators
        showKeyPreview(key, keyPreviewView, false)
    }

    private fun createDismissAnimator(key: Key, keyPreviewView: KeyPreviewView): Animator {
        val dismissAnimator = mParams.createDismissAnimator(keyPreviewView)
        dismissAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animator: Animator) {
                dismissKeyPreview(key, false)
            }
        })
        return dismissAnimator
    }

    private class KeyPreviewAnimators(private val mDismissAnimator: Animator) : AnimatorListenerAdapter() {
        fun startDismiss() {
            mDismissAnimator.start()
        }
    }
}
