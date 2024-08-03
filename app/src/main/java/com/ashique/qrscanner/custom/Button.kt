package com.ashique.qrscanner.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.ashique.qrscanner.R

class Button @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatButton(context, attrs, defStyleAttr) {
    private var width: Int = 0
    private var height: Int = 0
    private var radius: Float = 0f
    private var fillColor: Int = 0
    private var borderColor: Int = 0
    private var borderWidth: Float = 0f
    private var iconDrawable: Drawable? = null
    private var iconColor: Int = 0
    private val rect = RectF()
    private val textBounds = Rect()
    private val borderRect = RectF()
    private var iconWidth: Int = 0
    private var iconHeight: Int = 0
    private var isChecked: Boolean = false // Add this line for checked state
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private var onCheckedChangeListener: ((Boolean) -> Unit)? = null

    private var textMarginTop: Int = 0
    private var textMarginBottom: Int = 0
    private var textMarginLeft: Int = 0
    private var textMarginRight: Int = 0

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.Button,
            0, 0
        ).apply {
            try {
                radius = getDimension(R.styleable.Button__radius, 0f)
                fillColor = getColor(R.styleable.Button__fillColor, ContextCompat.getColor(context, android.R.color.transparent))
                borderColor = getColor(R.styleable.Button__borderColor, ContextCompat.getColor(context, android.R.color.black))
                borderWidth = getDimension(R.styleable.Button__borderWidth, 0f)
                iconDrawable = getDrawable(R.styleable.Button__icon)
                iconColor = getColor(R.styleable.Button__iconColor, context.getColor(R.color.white))
                iconWidth = getDimensionPixelSize(R.styleable.Button__iconWidth, iconDrawable?.intrinsicWidth ?: 0)
                iconHeight = getDimensionPixelSize(R.styleable.Button__iconHeight, iconDrawable?.intrinsicHeight ?: 0)
                textMarginTop = getDimensionPixelSize(R.styleable.Button__textMarginTop, 0)
                textMarginBottom = getDimensionPixelSize(R.styleable.Button__textMarginBottom, 0)
                textMarginLeft = getDimensionPixelSize(R.styleable.Button__textMarginLeft, 0)
                textMarginRight = getDimensionPixelSize(R.styleable.Button__textMarginRight, 0)
                isChecked = getBoolean(R.styleable.Button__checked, false) // Read the checked attribute
            } finally {
                recycle()
            }
        }

        fillPaint.color = fillColor
        borderPaint.color = borderColor
        borderPaint.strokeWidth = borderWidth
        // Set text alignment to center
        gravity = Gravity.CENTER

        // Apply icon color if icon is available
        iconDrawable?.colorFilter = PorterDuffColorFilter(iconColor, PorterDuff.Mode.SRC_IN)


        // Apply the checked state if necessary
        updateCheckedState()

        // Set a click listener to toggle checked state
        setOnClickListener {
            toggle()
        }
    }



    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        paint.getTextBounds(text.toString(), 0, text.length, textBounds)
        val textHeight = textBounds.height()
        val textWidth = paint.measureText(text.toString())

        val desiredWidth = (paddingLeft + paddingRight +
                iconWidth + textWidth + (2 * borderWidth)).toInt()
        val desiredHeight = (paddingTop + paddingBottom +
                maxOf(iconHeight, textHeight) + (2 * borderWidth)).toInt()

        val measuredWidth = resolveSize(desiredWidth, widthMeasureSpec)
        val measuredHeight = resolveSize(desiredHeight, heightMeasureSpec)

        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        width = w
        height = h
    }

    override fun onDraw(canvas: Canvas) {
        // Calculate the center position of the button
        val centerX = width / 2f
        val centerY = height / 2f

        // Adjust rectangle size for smooth rounded corners
        val borderWidthHalf = borderWidth / 2
        val rectRight = width.toFloat() - borderWidthHalf
        val rectBottom = height.toFloat() - borderWidthHalf
        rect.set(borderWidthHalf, borderWidthHalf, rectRight, rectBottom)

        // Draw the rounded rectangle
        canvas.drawRoundRect(rect, radius, radius, fillPaint)

        // Draw border if borderWidth is greater than 0
        if (borderWidth > 0) {
            borderRect.set(
                borderWidthHalf + borderWidthHalf,
                borderWidthHalf + borderWidthHalf,
                rectRight - borderWidthHalf,
                rectBottom - borderWidthHalf
            )
            canvas.drawRoundRect(borderRect, radius, radius, borderPaint)
        }

        // Draw icon if available
        iconDrawable?.let {
            val iconLeft = (width - iconWidth) / 2
            val iconTop = (height - iconHeight) / 2
            it.setBounds(iconLeft, iconTop, iconLeft + iconWidth, iconTop + iconHeight)
            it.draw(canvas)
        }

        // Draw text
        paint.color = currentTextColor
        val textWidth = paint.measureText(text.toString())
        val textHeight = paint.descent() - paint.ascent()
        val textX = centerX - textWidth / 2 + textMarginLeft - textMarginRight
        val textY = centerY - textHeight / 2 - paint.ascent() + textMarginTop - textMarginBottom
        canvas.drawText(text.toString(), textX, textY, paint)
    }

    fun setChecked(checked: Boolean) {
        if (isChecked != checked) {
            isChecked = checked
            updateCheckedState()
            onCheckedChangeListener?.invoke(isChecked)
        }
    }

    fun isChecked(): Boolean {
        return isChecked
    }

    private fun toggle() {
        setChecked(!isChecked)
    }

    fun setOnCheckedListener(listener: (Boolean) -> Unit) {
        onCheckedChangeListener = listener
    }

    private fun updateCheckedState() {
         invalidate()

    }


}
