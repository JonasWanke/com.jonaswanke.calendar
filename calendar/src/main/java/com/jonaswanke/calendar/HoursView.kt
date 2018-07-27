package com.jonaswanke.calendar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.support.annotation.AttrRes
import android.support.v4.content.ContextCompat
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import java.util.*
import kotlin.properties.Delegates


/**
 * TODO: document your custom view class.
 */
class HoursView @JvmOverloads constructor(context: Context,
                                          attrs: AttributeSet? = null,
                                          @AttrRes defStyleAttr: Int = R.attr.hoursViewStyle)
    : View(context, attrs, defStyleAttr) {

    var week: Week by Delegates.observable(Week()) { _, old, new ->
        if (old == new)
            return@observable

        invalidate()
    }

    private val weekMarginTop: Int
    private val weekSize: Int
    private val weekColor: Int
    private val weekPaint: TextPaint
    private val hourSize: Int
    private val hourColor: Int
    private val hourPaint: TextPaint
    private val headerHeight: Int

    internal var divider by Delegates.observable<Drawable?>(null) { _, _, new ->
        dividerHeight = new?.intrinsicWidth ?: 0
    }
        private set
    private var dividerHeight: Int = 0

    init {
        divider = ContextCompat.getDrawable(context, android.R.drawable.divider_horizontal_bright)

        val a = context.obtainStyledAttributes(
                attrs, R.styleable.HoursView, defStyleAttr, R.style.Calendar_HoursViewStyle)

        weekMarginTop = a.getDimensionPixelSize(R.styleable.HoursView_weekMarginTop, 40)
        weekSize = a.getDimensionPixelSize(R.styleable.HoursView_weekSize, 16)
        weekColor = a.getColor(R.styleable.HoursView_weekColor,
                ContextCompat.getColor(context, android.R.color.secondary_text_light))
        weekPaint = TextPaint().apply {
            typeface = Typeface.DEFAULT_BOLD
            color = weekColor
            isAntiAlias = true
            textSize = weekSize.toFloat()
        }

        hourSize = a.getDimensionPixelSize(R.styleable.HoursView_hourSize, 16)
        hourColor = a.getColor(R.styleable.HoursView_hourColor,
                ContextCompat.getColor(context, android.R.color.secondary_text_light))
        hourPaint = TextPaint().apply {
            color = hourColor
            isAntiAlias = true
            textSize = hourSize.toFloat()
        }

        a.recycle()

        headerHeight = context.resources.getDimensionPixelOffset(R.dimen.calendar_headerHeight)
    }

    private val hourBounds = Rect()
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null)
            return

        val left = paddingLeft
        var top = paddingTop
        val right = canvas.width - paddingRight
        val bottom = canvas.height - paddingBottom

        fun getStartForCentered(width: Float): Float {
            return left.toFloat() + (right - left - width) / 2
        }

        fun getBottomForCentered(center: Float, height: Int): Float {
            return center + height / 2
        }

        top += weekMarginTop + weekSize
        val weekText = week.week.toString()
        val weekWidth = weekPaint.measureText(weekText)
        canvas.drawText(weekText, getStartForCentered(weekWidth), top.toFloat(), weekPaint)

        divider?.setBounds(left, paddingTop + headerHeight - dividerHeight,
                right, paddingTop + headerHeight)
        divider?.draw(canvas)
        top = paddingTop + headerHeight

        val hourHeight = (bottom.toFloat() - top) / 24
        for (hour in 1..23) {
            val hourText = if (hour < 10) "0$hour:00" else "$hour:00"
            hourPaint.getTextBounds(hourText, 0, hourText.length, hourBounds)
            canvas.drawText(hourText,
                    getStartForCentered(hourBounds.width().toFloat()),
                    getBottomForCentered(top + hourHeight * hour, hourBounds.height()), hourPaint)
        }
    }
}
