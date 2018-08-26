package com.jonaswanke.calendar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.AttrRes
import androidx.core.content.withStyledAttributes
import com.jonaswanke.calendar.utils.Day
import kotlin.properties.Delegates


/**
 * TODO: document your custom view class.
 */
class WeekIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = R.attr.weekIndicatorViewStyle,
    _start: Day? = null
) : RangeViewStartIndicator(context, attrs, defStyleAttr) {

    override var start: Day by Delegates.observable(_start ?: Day()) { _, old, new ->
        if (old == new)
            return@observable

        invalidate()
    }

    private var weekOffsetTop: Int = 0
    private var weekSize: Int = 0
    private var weekColor: Int = 0
    private lateinit var weekPaint: TextPaint

    init {
        context.withStyledAttributes(attrs, R.styleable.WeekIndicatorView,
                defStyleAttr, R.style.Calendar_WeekIndicatorViewStyle) {
            weekOffsetTop = getDimensionPixelSize(R.styleable.WeekIndicatorView_weekOffsetTop, 0)
            weekSize = getDimensionPixelSize(R.styleable.WeekIndicatorView_weekSize, 0)
            weekColor = getColor(R.styleable.WeekIndicatorView_weekColor, Color.BLACK)
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

        val weekText = start.week.toString()
        val weekWidth = weekPaint.measureText(weekText)
        canvas.drawText(weekText,
                left.toFloat() + (right - left - weekWidth) / 2,
                Math.min(top + weekOffsetTop, bottom).toFloat(),
                weekPaint)
    }
}
