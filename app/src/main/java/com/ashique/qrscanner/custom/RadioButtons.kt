package com.ashique.qrscanner.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.core.content.ContextCompat
import com.ashique.qrscanner.R

class RadioButtons @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatRadioButton(context, attrs, defStyleAttr) {

    private var backgroundColor: Int = Color.TRANSPARENT
    private var cornerRadius: Float = 0f
    private var icon: Drawable? = null
    private var iconColor: Int = 0
    private var borderColor: Int = Color.TRANSPARENT
    private var borderWidth: Float = 0f
    private var activeColor: Int = Color.TRANSPARENT
    private var activeIconColor: Int = Color.TRANSPARENT
    private var activeTextColor: Int = Color.TRANSPARENT
    private var iconWidth: Int = 0
    private var iconHeight: Int = 0

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val defaultTextColor: Int = ContextCompat.getColor(context, R.color.text)
    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CustomRadioButton,
            0, 0
        ).apply {
            try {
                backgroundColor =
                    getColor(R.styleable.CustomRadioButton_customBackgroundColor, Color.TRANSPARENT)
                cornerRadius = getDimension(R.styleable.CustomRadioButton_customCornerRadius, 0f)
                icon = getDrawable(R.styleable.CustomRadioButton_customIcon)
                iconColor = getColor(R.styleable.CustomRadioButton_customIconColor, Color.WHITE)
                iconWidth = getDimensionPixelSize(
                    R.styleable.CustomRadioButton_customIconWidth,
                    icon?.intrinsicWidth ?: 0
                )
                iconHeight = getDimensionPixelSize(
                    R.styleable.CustomRadioButton_customIconWidth,
                    icon?.intrinsicHeight ?: 0
                )
                borderColor =
                    getColor(R.styleable.CustomRadioButton_customBorderColor, Color.TRANSPARENT)
                borderWidth = getDimension(R.styleable.CustomRadioButton_customBorderWidth, 0f)
                activeColor =
                    getColor(R.styleable.CustomRadioButton_customActiveColor, Color.TRANSPARENT)
                activeIconColor =
                    getColor(R.styleable.CustomRadioButton_customActiveIconColor, Color.TRANSPARENT)
                activeTextColor =
                    getColor(R.styleable.CustomRadioButton_customActiveTextColor, Color.TRANSPARENT)
            } finally {
                recycle()
            }
        }


        // Apply icon color if icon is available
        icon?.colorFilter = PorterDuffColorFilter(
            if (isChecked) activeIconColor else iconColor,
            PorterDuff.Mode.SRC_IN
        )

        backgroundPaint.color = backgroundColor
        borderPaint.color = borderColor
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = borderWidth

        isClickable = true
        isFocusable = true
        setOnCheckedChangeListener { _, _ ->
            icon?.colorFilter = PorterDuffColorFilter(
                if (isChecked) activeIconColor else iconColor,
                PorterDuff.Mode.SRC_IN
            )
            setTextColor(if (isChecked) activeTextColor else defaultTextColor)
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()

        // Draw the custom background
        backgroundPaint.color = if (isChecked) activeColor else backgroundColor
        canvas.drawRoundRect(
            borderWidth / 2,
            borderWidth / 2,
            width - borderWidth / 2,
            height - borderWidth / 2,
            cornerRadius,
            cornerRadius,
            backgroundPaint
        )

        // Draw the border
        if (borderWidth > 0) {
            canvas.drawRoundRect(
                borderWidth / 2,
                borderWidth / 2,
                width - borderWidth / 2,
                height - borderWidth / 2,
                cornerRadius,
                cornerRadius,
                borderPaint
            )
        }


        // Draw the icon if available
        icon?.let {
            //   val iconSize = height * 0.5f
            val left = (width - iconWidth) / 2
            val top = (height - iconHeight) / 2
            it.setBounds(
                left.toInt(),
                top.toInt(),
                (left + iconWidth).toInt(),
                (top + iconHeight).toInt()
            )
            it.draw(canvas)
        }

        super.onDraw(canvas)
    }


}
