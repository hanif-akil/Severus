/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.compat.PreferenceManagerCompat
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeyDrawParams
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeyVisualAttributes
import rkr.simplekeyboard.inputmethod.latin.common.Constants
import rkr.simplekeyboard.inputmethod.latin.settings.Settings
import rkr.simplekeyboard.inputmethod.latin.utils.TypefaceUtils
import java.util.HashSet

/**
 * A view that renders a virtual [Keyboard].
 */
open class KeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyle: Int = R.attr.keyboardViewStyle
) : View(context, attrs, defStyle) {
    // XML attributes
    private val mKeyVisualAttributes: KeyVisualAttributes
    private val mDefaultKeyLabelFlags: Int
    private val mKeyHintLetterPadding: Float
    private val mKeyShiftedLetterHintPadding: Float
    private val mKeyTextShadowRadius: Float
    private val mVerticalCorrection: Float
    private val mKeyBackground: Drawable
    private val mFunctionalKeyBackground: Drawable
    private val mSpacebarBackground: Drawable
    private val mKeyBackgroundPadding = Rect()

    protected var mCustomColor: Int = 0
    protected var mTheme: KeyboardTheme? = null

    private var mKeyboard: Keyboard? = null
    private val mKeyDrawParams = KeyDrawParams()

    private var mInvalidateAllKeys: Boolean = false
    private val mInvalidatedKeys = HashSet<Key>()
    private val mClipRect = Rect()
    private var mOffscreenBuffer: Bitmap? = null
    private val mOffscreenCanvas = Canvas()
    private val mPaint = Paint()
    private val mFontMetrics = Paint.FontMetrics()

    init {
        val keyboardViewAttr = context.obtainStyledAttributes(
            attrs, R.styleable.KeyboardView, defStyle, R.style.KeyboardView
        )
        mKeyBackground = keyboardViewAttr.getDrawable(R.styleable.KeyboardView_keyBackground)!!
        mKeyBackground.getPadding(mKeyBackgroundPadding)
        val functionalKeyBackground = keyboardViewAttr.getDrawable(
            R.styleable.KeyboardView_functionalKeyBackground
        )
        mFunctionalKeyBackground = functionalKeyBackground ?: mKeyBackground
        val spacebarBackground = keyboardViewAttr.getDrawable(
            R.styleable.KeyboardView_spacebarBackground
        )
        mSpacebarBackground = spacebarBackground ?: mKeyBackground
        mKeyHintLetterPadding = keyboardViewAttr.getDimension(
            R.styleable.KeyboardView_keyHintLetterPadding, 0.0f
        )
        mKeyShiftedLetterHintPadding = keyboardViewAttr.getDimension(
            R.styleable.KeyboardView_keyShiftedLetterHintPadding, 0.0f
        )
        mKeyTextShadowRadius = keyboardViewAttr.getFloat(
            R.styleable.KeyboardView_keyTextShadowRadius, KET_TEXT_SHADOW_RADIUS_DISABLED
        )
        mVerticalCorrection = keyboardViewAttr.getDimension(
            R.styleable.KeyboardView_verticalCorrection, 0.0f
        )
        keyboardViewAttr.recycle()

        val keyAttr = context.obtainStyledAttributes(
            attrs, R.styleable.Keyboard_Key, defStyle, R.style.KeyboardView
        )
        mDefaultKeyLabelFlags = keyAttr.getInt(R.styleable.Keyboard_Key_keyLabelFlags, 0)
        mKeyVisualAttributes = KeyVisualAttributes.newInstance(keyAttr)
        keyAttr.recycle()

        mPaint.isAntiAlias = true
    }

    fun setKeyboard(keyboard: Keyboard) {
        mKeyboard = keyboard
        val keyHeight = keyboard.mMostCommonKeyHeight
        mKeyDrawParams.updateParams(keyHeight, mKeyVisualAttributes)
        mKeyDrawParams.updateParams(keyHeight, keyboard.mKeyVisualAttributes)
        val prefs = PreferenceManagerCompat.getDeviceSharedPreferences(context)
        mCustomColor = Settings.readKeyboardColor(prefs, context)
        mTheme = Settings.getKeyboardTheme(context)
        invalidateAllKeys()
        requestLayout()
    }

    fun getKeyboard(): Keyboard? = mKeyboard

    protected open fun getVerticalCorrection(): Float = mVerticalCorrection

    protected fun getKeyDrawParams(): KeyDrawParams = mKeyDrawParams

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val keyboard = getKeyboard()
        if (keyboard == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }
        val width = keyboard.mOccupiedWidth + paddingLeft + paddingRight
        val height = keyboard.mOccupiedHeight + paddingTop + paddingBottom
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (canvas.isHardwareAccelerated) {
            onDrawKeyboard(canvas)
            return
        }

        val bufferNeedsUpdates = mInvalidateAllKeys || mInvalidatedKeys.isNotEmpty()
        if (bufferNeedsUpdates || mOffscreenBuffer == null) {
            if (maybeAllocateOffscreenBuffer()) {
                mInvalidateAllKeys = true
                mOffscreenCanvas.setBitmap(mOffscreenBuffer)
            }
            onDrawKeyboard(mOffscreenCanvas)
        }
        canvas.drawBitmap(mOffscreenBuffer!!, 0.0f, 0.0f, null)
    }

    private fun maybeAllocateOffscreenBuffer(): Boolean {
        val width = width
        val height = height
        if (width == 0 || height == 0) return false
        if (mOffscreenBuffer != null && mOffscreenBuffer!!.width == width
            && mOffscreenBuffer!!.height == height
        ) return false
        freeOffscreenBuffer()
        mOffscreenBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        return true
    }

    private fun freeOffscreenBuffer() {
        mOffscreenCanvas.setBitmap(null)
        mOffscreenCanvas.setMatrix(null)
        mOffscreenBuffer?.recycle()
        mOffscreenBuffer = null
    }

    private fun onDrawKeyboard(canvas: Canvas) {
        val keyboard = getKeyboard() ?: return

        val paint = mPaint
        val background = background
        if (background != null && mTheme?.mCustomColorSupport == true) {
            if (keyboard.javaClass == MoreKeysKeyboard::class.java) {
                background.setColorFilter(mCustomColor, PorterDuff.Mode.OVERLAY)
            } else {
                setBackgroundColor(mCustomColor)
            }
        }
        val drawAllKeys = mInvalidateAllKeys || mInvalidatedKeys.isEmpty()
        val isHardwareAccelerated = canvas.isHardwareAccelerated
        if (drawAllKeys || isHardwareAccelerated) {
            if (!isHardwareAccelerated && background != null) {
                canvas.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR)
                background.draw(canvas)
            }
            for (key in keyboard.getSortedKeys()) {
                onDrawKey(key, canvas, paint)
            }
        } else {
            for (key in mInvalidatedKeys) {
                if (!keyboard.hasKey(key)) continue
                if (background != null) {
                    val x = key.x + paddingLeft
                    val y = key.y + paddingTop
                    mClipRect.set(x, y, x + key.width, y + key.height)
                    canvas.save()
                    canvas.clipRect(mClipRect)
                    canvas.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR)
                    background.draw(canvas)
                    canvas.restore()
                }
                onDrawKey(key, canvas, paint)
            }
        }

        mInvalidatedKeys.clear()
        mInvalidateAllKeys = false
    }

    private fun onDrawKey(key: Key, canvas: Canvas, paint: Paint) {
        val keyDrawX = key.x + paddingLeft
        val keyDrawY = key.y + paddingTop
        canvas.translate(keyDrawX.toFloat(), keyDrawY.toFloat())

        val attr = key.visualAttributes
        val params = mKeyDrawParams.mayCloneAndUpdateParams(key.height, attr)
        params.mAnimAlpha = Constants.Color.ALPHA_OPAQUE

        if (!key.isSpacer) {
            val bg = key.selectBackgroundDrawable(mKeyBackground, mFunctionalKeyBackground, mSpacebarBackground)
            if (bg != null) {
                onDrawKeyBackground(key, canvas, bg)
            }
        }
        onDrawKeyTopVisuals(key, canvas, paint, params)

        canvas.translate(-keyDrawX.toFloat(), -keyDrawY.toFloat())
    }

    protected open fun onDrawKeyBackground(key: Key, canvas: Canvas, background: Drawable) {
        val keyWidth = key.width
        val keyHeight = key.height
        val padding = mKeyBackgroundPadding
        val bgWidth = keyWidth + padding.left + padding.right
        val bgHeight = keyHeight + padding.top + padding.bottom
        val bgX = -padding.left.toFloat()
        val bgY = -padding.top.toFloat()
        val bounds = background.bounds
        if (bgWidth != bounds.right || bgHeight != bounds.bottom) {
            background.setBounds(0, 0, bgWidth, bgHeight)
        }
        canvas.translate(bgX, bgY)
        background.draw(canvas)
        canvas.translate(-bgX, -bgY)
    }

    protected open fun onDrawKeyTopVisuals(key: Key, canvas: Canvas, paint: Paint, params: KeyDrawParams) {
        val keyWidth = key.width
        val keyHeight = key.height
        val centerX = keyWidth * 0.5f
        val centerY = keyHeight * 0.5f

        val keyboard = getKeyboard()
        val icon = keyboard?.let { key.getIcon(it.mIconsSet, params.mAnimAlpha) }
        var labelX = centerX
        var labelBaseline = centerY
        val label = key.label
        if (label != null) {
            paint.typeface = key.selectTypeface(params)
            paint.textSize = key.selectTextSize(params).toFloat()
            val labelCharHeight = TypefaceUtils.getReferenceCharHeight(paint)
            val labelCharWidth = TypefaceUtils.getReferenceCharWidth(paint)

            labelBaseline = centerY + labelCharHeight / 2.0f

            if (key.isAlignLabelOffCenter) {
                labelX = centerX + params.mLabelOffCenterRatio * labelCharWidth
                paint.textAlign = Align.LEFT
            } else {
                labelX = centerX
                paint.textAlign = Align.CENTER
            }
            if (key.needsAutoXScale) {
                val ratio = Math.min(
                    1.0f, (keyWidth * MAX_LABEL_RATIO) /
                            TypefaceUtils.getStringWidth(label, paint)
                )
                if (key.needsAutoScale) {
                    val autoSize = paint.textSize * ratio
                    paint.textSize = autoSize
                } else {
                    paint.textScaleX = ratio
                }
            }

            paint.color = key.selectTextColor(params)
            if (mKeyTextShadowRadius > 0.0f) {
                paint.setShadowLayer(mKeyTextShadowRadius, 0.0f, 0.0f, params.mTextShadowColor)
            } else {
                paint.clearShadowLayer()
            }

            blendAlpha(paint, params.mAnimAlpha)
            canvas.drawText(label, 0, label.length, labelX, labelBaseline, paint)
            paint.clearShadowLayer()
            paint.textScaleX = 1.0f
        }

        val hintLabel = key.hintLabel
        if (hintLabel != null) {
            paint.textSize = key.selectHintTextSize(params).toFloat()
            paint.color = key.selectHintTextColor(params)
            paint.typeface = Typeface.DEFAULT_BOLD
            blendAlpha(paint, params.mAnimAlpha)
            val labelCharHeight = TypefaceUtils.getReferenceCharHeight(paint)
            val labelCharWidth = TypefaceUtils.getReferenceCharWidth(paint)
            val hintX: Float
            val hintBaseline: Float
            if (key.hasHintLabel) {
                hintX = labelX + params.mHintLabelOffCenterRatio * labelCharWidth
                hintBaseline = if (key.isAlignHintLabelToBottom(mDefaultKeyLabelFlags)) {
                    labelBaseline
                } else {
                    centerY + labelCharHeight / 2.0f
                }
                paint.textAlign = Align.LEFT
            } else if (key.hasShiftedLetterHint) {
                hintX = keyWidth - mKeyShiftedLetterHintPadding - labelCharWidth / 2.0f
                paint.getFontMetrics(mFontMetrics)
                hintBaseline = -mFontMetrics.top
                paint.textAlign = Align.CENTER
            } else {
                val hintDigitWidth = TypefaceUtils.getReferenceDigitWidth(paint)
                val hintLabelWidth = TypefaceUtils.getStringWidth(hintLabel, paint)
                hintX = keyWidth - mKeyHintLetterPadding -
                        Math.max(hintDigitWidth, hintLabelWidth) / 2.0f
                hintBaseline = -paint.ascent()
                paint.textAlign = Align.CENTER
            }
            val adjustmentY = params.mHintLabelVerticalAdjustment * labelCharHeight
            canvas.drawText(
                hintLabel, 0, hintLabel.length, hintX, hintBaseline + adjustmentY, paint
            )
        }

        if (label == null && icon != null) {
            val iconWidth = Math.min(icon.intrinsicWidth, keyWidth)
            val iconHeight = icon.intrinsicHeight
            val iconY = if (key.isAlignIconToBottom) {
                keyHeight - iconHeight
            } else {
                (keyHeight - iconHeight) / 2
            }
            val iconX = (keyWidth - iconWidth) / 2
            drawIcon(canvas, icon, iconX, iconY, iconWidth, iconHeight)
        }
    }

    fun newLabelPaint(key: Key?): Paint {
        val paint = Paint()
        paint.isAntiAlias = true
        if (key == null) {
            paint.typeface = mKeyDrawParams.mTypeface
            paint.textSize = mKeyDrawParams.mLabelSize.toFloat()
        } else {
            paint.color = key.selectTextColor(mKeyDrawParams)
            paint.typeface = key.selectTypeface(mKeyDrawParams)
            paint.textSize = key.selectTextSize(mKeyDrawParams).toFloat()
        }
        return paint
    }

    fun invalidateAllKeys() {
        mInvalidatedKeys.clear()
        mInvalidateAllKeys = true
        invalidate()
    }

    fun invalidateKey(key: Key?) {
        if (mInvalidateAllKeys || key == null) return
        mInvalidatedKeys.add(key)
        val x = key.x + paddingLeft
        val y = key.y + paddingTop
        invalidate(x, y, x + key.width, y + key.height)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        freeOffscreenBuffer()
    }

    fun deallocateMemory() {
        freeOffscreenBuffer()
    }

    companion object {
        private const val KET_TEXT_SHADOW_RADIUS_DISABLED = -1.0f
        private const val MAX_LABEL_RATIO = 0.90f

        @JvmStatic
        protected fun drawIcon(canvas: Canvas, icon: Drawable, x: Int, y: Int, width: Int, height: Int) {
            canvas.translate(x.toFloat(), y.toFloat())
            icon.setBounds(0, 0, width, height)
            icon.draw(canvas)
            canvas.translate(-x.toFloat(), -y.toFloat())
        }

        private fun blendAlpha(paint: Paint, alpha: Int) {
            val color = paint.color
            paint.setARGB(
                (paint.alpha * alpha) / Constants.Color.ALPHA_OPAQUE,
                Color.red(color), Color.green(color), Color.blue(color)
            )
        }
    }
}
