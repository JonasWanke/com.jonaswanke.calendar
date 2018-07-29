package com.jonaswanke.calendar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.support.annotation.AttrRes
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import kotlin.properties.Delegates


/**
 * TODO: document your custom view class.
 */
class HoursHeaderView @JvmOverloads constructor(context: Context,
                                                attrs: AttributeSet? = null,
                                                @AttrRes defStyleAttr: Int = R.attr.hoursHeaderViewStyle,
                                                _week: Week? = null)
    : View(context, attrs, defStyleAttr) {

    var week: Week by Delegates.observable(_week ?: Week()) { _, old, new ->
        if (old == new)
            return@observable

        invalidate()
    }

    private val weekOffsetTop: Int
    private val weekSize: Int
    private val weekColor: Int
    private val weekPaint: TextPaint

    init {
        val a = context.obtainStyledAttributes(
                attrs, R.styleable.HoursHeaderView, defStyleAttr, R.style.Calendar_HoursHeaderViewStyle)

        weekOffsetTop = a.getDimensionPixelSize(R.styleable.HoursHeaderView_weekOffsetTop, 48)
        weekSize = a.getDimensionPixelSize(R.styleable.HoursHeaderView_weekSize, 16)
        weekColor = a.getColor(R.styleable.HoursHeaderView_weekColor, Color.BLACK)
        weekPaint = TextPaint().apply {
            typeface = Typeface.DEFAULT_BOLD
            color = weekColor
            isAntiAlias = true
            textSize = weekSize.toFloat()
        }

        a.recycle()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null)
            return

        val left = paddingLeft
        val top = paddingTop
        val right = canvas.width - paddingRight
        val bottom = canvas.height - paddingBottom

        val weekText = week.week.toString()
        val weekWidth = weekPaint.measureText(weekText)
        canvas.drawText(weekText,
                left.toFloat() + (right - left - weekWidth) / 2,
                Math.min(top + weekOffsetTop, bottom).toFloat(),
                weekPaint)
    }
}
