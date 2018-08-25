package com.jonaswanke.calendar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.AttrRes
import androidx.core.content.withStyledAttributes
import com.jonaswanke.calendar.utils.DAY_IN_HOURS
import kotlin.properties.Delegates


/**
 * TODO: document your custom view class.
 */
class HoursView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = R.attr.hoursViewStyle
) : View(context, attrs, defStyleAttr) {
    companion object {
        private const val DIGIT_MAX = 9
    }

    private var _hourHeight: Float = 0f
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

    private var hourSize: Int = 0
    private var hourColor: Int = 0
    private lateinit var hourPaint: TextPaint

    init {
        context.withStyledAttributes(attrs, R.styleable.HoursView, defStyleAttr, R.style.Calendar_HoursViewStyle) {
            _hourHeight = getDimension(R.styleable.HoursView_hourHeight, 0f)
            hourHeightMin = getDimension(R.styleable.HoursView_hourHeightMin, 0f)
            hourHeightMax = getDimension(R.styleable.HoursView_hourHeightMax, 0f)
            hourSize = getDimensionPixelSize(R.styleable.HoursView_hourSize, 0)
            hourColor = getColor(R.styleable.HoursView_hourColor, Color.BLACK)
            hourPaint = TextPaint().apply {
                color = hourColor
                isAntiAlias = true
                textSize = hourSize.toFloat()
            }
        }
    }


    // View
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = paddingTop + paddingBottom + Math.max(suggestedMinimumHeight, (_hourHeight * DAY_IN_HOURS).toInt())
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
        val right = width - paddingRight

        fun getStartForCentered(width: Float): Float {
            return left.toFloat() + (right - left - width) / 2
        }

        fun getBottomForCentered(center: Float, height: Int): Float {
            return center + height / 2
        }

        for (hour in 1 until DAY_IN_HOURS) {
            val hourText = if (hour <= DIGIT_MAX) "0$hour:00" else "$hour:00"
            hourPaint.getTextBounds(hourText, 0, hourText.length, hourBounds)
            canvas.drawText(hourText,
                    getStartForCentered(hourBounds.width().toFloat()),
                    getBottomForCentered(top + _hourHeight * hour, hourBounds.height()),
                    hourPaint)
        }
    }
}
