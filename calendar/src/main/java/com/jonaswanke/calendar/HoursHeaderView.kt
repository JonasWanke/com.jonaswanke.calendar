package com.jonaswanke.calendar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import androidx.annotation.AttrRes
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes
import kotlin.properties.Delegates


/**
 * TODO: document your custom view class.
 */
class HoursHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = R.attr.hoursHeaderViewStyle,
    _week: Week? = null
) : View(context, attrs, defStyleAttr) {

    var week: Week by Delegates.observable(_week ?: Week()) { _, old, new ->
        if (old == new)
            return@observable

        invalidate()
    }

    private var weekOffsetTop: Int = 0
    private var weekSize: Int = 0
    private var weekColor: Int = 0
    private lateinit var weekPaint: TextPaint

    init {
        context.withStyledAttributes(attrs, R.styleable.HoursHeaderView, defStyleAttr, R.style.Calendar_HoursHeaderViewStyle) {
            weekOffsetTop = getDimensionPixelSize(R.styleable.HoursHeaderView_weekOffsetTop, 48)
            weekSize = getDimensionPixelSize(R.styleable.HoursHeaderView_weekSize, 16)
            weekColor = getColor(R.styleable.HoursHeaderView_weekColor, Color.BLACK)
            weekPaint = TextPaint().apply {
                typeface = Typeface.DEFAULT_BOLD
                color = weekColor
                isAntiAlias = true
                textSize = weekSize.toFloat()
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = paddingTop + paddingBottom + minimumHeight
        setMeasuredDimension(View.getDefaultSize(suggestedMinimumWidth, widthMeasureSpec), height)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null)
            return

        val left = paddingLeft
        val top = paddingTop
        val right = width - paddingRight
        val bottom = height - paddingBottom

        val weekText = week.week.toString()
        val weekWidth = weekPaint.measureText(weekText)
        canvas.drawText(weekText,
                left.toFloat() + (right - left - weekWidth) / 2,
                Math.min(top + weekOffsetTop, bottom).toFloat(),
                weekPaint)
    }
}
