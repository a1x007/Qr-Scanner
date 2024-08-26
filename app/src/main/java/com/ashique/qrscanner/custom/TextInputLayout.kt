package com.ashique.qrscanner.custom

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import androidx.core.content.res.ResourcesCompat
import com.ashique.qrscanner.R

class TextInputLayout @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    private val defStyleRes: Int = 0
) : FrameLayout(context, attributeSet, defStyleRes) {

    private val path = Path()

    var cornerRadius = 16f
    private var collapsedTextHeight = 0f
    private var collapsedTextWidth = 0f
    var collapsedTextSize = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        10f,
        context.resources.displayMetrics
    )
    var animationDuration = 300
    private var animator: Animator? = null
    var hint: String = ""
    private var originalHint: String? = null
    private var editText: EditText? = null
    private val expandedHintPoint = PointF()
    private var spacing = dpToPx(2f, context)
    private var hintBaseLine = 0f
    private var hideHintText = true
    var hintTextColor: Int = Color.WHITE

    private val hintTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        style = Paint.Style.FILL
        textSize = collapsedTextSize
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 4f
        textSize = collapsedTextSize
    }

    init {
        val padding = dpToPx(8f, context).toInt()
        setPadding(padding, padding, padding, padding)
        setWillNotDraw(false)
        attributeSet?.let(::initAttrs)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        editText?.let {
            val editTextHeight = it.height
            expandedHintPoint.apply {
                x = paddingLeft + it.paddingLeft.toFloat()
                y = it.top + (editTextHeight / 2f + it.textSize / 2)
            }
            if (changed) {
                updateHintVisibility(it)
            }
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        borderPaint.style = Paint.Style.STROKE
        canvas.drawPath(path, borderPaint)
        borderPaint.style = Paint.Style.FILL
        if (!hideHintText) {
            hintTextPaint.color = hintTextColor
            canvas.drawText(hint, expandedHintPoint.x, hintBaseLine, hintTextPaint)
        }
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        super.addView(child, index, params)
        if (child is EditText) {
            child.background = null
            child.setOnFocusChangeListener { _, hasFocus ->
                // Update originalHint with current hint when gaining focus
                if (hasFocus) {
                    originalHint = child.hint.toString()
                    hint = originalHint ?: ""
                }
                animateHint(child, hasFocus)
            }
            //originalHint = child.hint.toString()
            hint = child.hint.toString()
            updateHintProperties()
            editText = child
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    private fun updateHintVisibility(editText: EditText) {
        if (editText.hasFocus()) {
            hideHintText = false
            hint = editText.hint.toString()
            updateBorderPath(-spacing)
        } else {
            hint = originalHint ?: ""
            hideHintText = true
            updateBorderPath(collapsedTextWidth)
        }
        invalidate()
    }

    private fun updateHintProperties() {
        val rect = Rect()
        hintTextPaint.getTextBounds(hint, 0, hint.length, rect)
        hintTextPaint.color = hintTextColor
        collapsedTextHeight = rect.height().toFloat()
        collapsedTextWidth = rect.width() + spacing * 2
    }

    private fun updateBorderPath(textWidth: Float) {
        path.reset()
        val strokeHalf = borderPaint.strokeWidth / 2
        path.apply {
            moveTo(expandedHintPoint.x - spacing, strokeHalf + collapsedTextHeight / 2)
            lineTo(cornerRadius, strokeHalf + collapsedTextHeight / 2)
            quadTo(strokeHalf, strokeHalf + collapsedTextHeight / 2, strokeHalf, cornerRadius + collapsedTextHeight / 2)
            lineTo(strokeHalf, height - strokeHalf - cornerRadius)
            quadTo(strokeHalf, height - strokeHalf, cornerRadius, height - strokeHalf)
            lineTo(width - cornerRadius, height - strokeHalf)
            quadTo(width - strokeHalf, height - strokeHalf, width - strokeHalf, height - cornerRadius)
            lineTo(width - strokeHalf, strokeHalf + collapsedTextHeight / 2 + cornerRadius)
            quadTo(width - strokeHalf, strokeHalf + collapsedTextHeight / 2, width - cornerRadius, strokeHalf + collapsedTextHeight / 2)
            lineTo(expandedHintPoint.x + textWidth, strokeHalf + collapsedTextHeight / 2)
        }
    }

    private fun initAttrs(attributeSet: AttributeSet) {
        context.theme.obtainStyledAttributes(attributeSet, R.styleable.TextInputLayout, defStyleRes, 0).apply {
            try {
                borderPaint.color = getColor(R.styleable.TextInputLayout_BorderColor, borderPaint.color)
                borderPaint.strokeWidth = getDimension(R.styleable.TextInputLayout_BorderWidth, borderPaint.strokeWidth)
                collapsedTextSize = getDimension(R.styleable.TextInputLayout_animatedTextSize, collapsedTextSize).also {
                    hintTextPaint.textSize = it
                }
                cornerRadius = getDimension(R.styleable.TextInputLayout_CornerRadius, cornerRadius)
                animationDuration = getInteger(R.styleable.TextInputLayout_animationDuration, animationDuration)
                hintTextColor = getColor(R.styleable.TextInputLayout_hintColor, hintTextPaint.color)
                hintTextPaint.color = hintTextColor
                val fontId = getResourceId(R.styleable.TextInputLayout_hintTextFont, -1)
                if (fontId != -1) {
                    val typeface = ResourcesCompat.getFont(context, fontId)
                    hintTextPaint.typeface = typeface
                }
            } finally {
                recycle()
            }
        }
    }

    private fun animateHint(editText: EditText, hasFocus: Boolean) {
        if (editText.editableText.isNotBlank()) {
            return
        }

        animator = if (hasFocus) {
            editText.hint = ""
            hideHintText = false
            getHintAnimator(
                editText.textSize,
                collapsedTextSize,
                expandedHintPoint.y,
                collapsedTextHeight,
                -spacing,
                collapsedTextWidth
            )
        } else {
            getHintAnimator(
                collapsedTextSize,
                editText.textSize,
                collapsedTextHeight,
                expandedHintPoint.y,
                collapsedTextWidth,
                -spacing
            ).apply {
                addListener(object : DefaultAnimatorListener() {
                    override fun onAnimationEnd(animation: Animator) {
                        editText.hint = originalHint ?: ""
                        hideHintText = true
                    }
                })
            }
        }
        animator?.start()
    }

    private fun getHintAnimator(fromTextSize: Float, toTextSize: Float, fromY: Float, toY: Float, fromTextWidth: Float, toTextWidth: Float): Animator {
        val textSizeAnimator = ValueAnimator.ofFloat(fromTextSize, toTextSize).apply {
            duration = animationDuration.toLong()
            interpolator = LinearInterpolator()
            addUpdateListener {
                hintTextPaint.textSize = it.animatedValue as Float
                invalidate()
            }
        }

        val translateAnimator = ValueAnimator.ofFloat(fromY, toY).apply {
            duration = animationDuration.toLong()
            interpolator = LinearInterpolator()
            addUpdateListener {
                hintBaseLine = (it.animatedValue as Float) + collapsedTextHeight / 2
                invalidate()
            }
        }

        val textWidthAnimator = ValueAnimator.ofFloat(fromTextWidth, toTextWidth).apply {
            duration = animationDuration.toLong()
            interpolator = LinearInterpolator()
            addUpdateListener {
                updateBorderPath(it.animatedValue as Float)
                invalidate()
            }
        }

        return AnimatorSet().apply {
            playTogether(textSizeAnimator, translateAnimator, textWidthAnimator)
        }
    }

    private fun dpToPx(dp: Float, context: Context): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        )
    }

    open inner class DefaultAnimatorListener : Animator.AnimatorListener {
        override fun onAnimationRepeat(animation: Animator) {}
        override fun onAnimationEnd(animation: Animator) {}
        override fun onAnimationCancel(animation: Animator) {}
        override fun onAnimationStart(animation: Animator) {}
    }
}
