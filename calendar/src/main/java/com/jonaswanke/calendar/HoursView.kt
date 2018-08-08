package com.jonaswanke.calendar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.support.annotation.AttrRes
import android.support.v4.content.ContextCompat
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import kotlin.properties.Delegates


/**
 * TODO: document your custom view class.
 */
class HoursView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = R.attr.hoursViewStyle
) : View(context, attrs, defStyleAttr) {

    private var _hourHeight: Float
    var hourHeight: Float
        get() = _hourHeight
        set(value) {
            val v = value.coerceIn(if (hourHeightMin > 0) hourHeightMin else null,
                    if (hourHeightMax > 0) hourHeightMax else null)
            if (_hourHeight == v)
                return

            _hourHeight = v
            requestLayout()
        }
    var hourHeightMin: Float by Delegates.observable(0f) { _, _, new ->
        if (new > 0 && hourHeight < new)
            hourHeight = new
    }
    var hourHeightMax: Float by Delegates.observable(0f) { _, _, new ->
        if (new > 0 && hourHeight > new)
            hourHeight = new
    }

    private val hourSize: Int
    private val hourColor: Int
    private val hourPaint: TextPaint

    init {
        val a = context.obtainStyledAttributes(
                attrs, R.styleable.HoursView, defStyleAttr, R.style.Calendar_HoursViewStyle)

        _hourHeight = a.getDimension(R.styleable.HoursView_hourHeight, 16f)
        hourHeightMin = a.getDimension(R.styleable.HoursView_hourHeightMin, 0f)
        hourHeightMax = a.getDimension(R.styleable.HoursView_hourHeightMax, 0f)
        hourSize = a.getDimensionPixelSize(R.styleable.HoursView_hourSize, 16)
        hourColor = a.getColor(R.styleable.HoursView_hourColor,
                ContextCompat.getColor(context, android.R.color.secondary_text_light))
        hourPaint = TextPaint().apply {
            color = hourColor
            isAntiAlias = true
            textSize = hourSize.toFloat()
        }

        a.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = paddingTop + paddingBottom + Math.max(suggestedMinimumHeight, (_hourHeight * 24).toInt())
        setMeasuredDimension(getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
                height)
    }

    private val hourBounds = Rect()
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null)
            return

        val left = paddingLeft
        val top = paddingTop
        val right = canvas.width - paddingRight

        fun getStartForCentered(width: Float): Float {
            return left.toFloat() + (right - left - width) / 2
        }

        fun getBottomForCentered(center: Float, height: Int): Float {
            return center + height / 2
        }

        for (hour in 1..23) {
            val hourText = if (hour < 10) "0$hour:00" else "$hour:00"
            hourPaint.getTextBounds(hourText, 0, hourText.length, hourBounds)
            canvas.drawText(hourText,
                    getStartForCentered(hourBounds.width().toFloat()),
                    getBottomForCentered(top + _hourHeight * hour, hourBounds.height()),
                    hourPaint)
        }
    }
}
