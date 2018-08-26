package com.jonaswanke.calendar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.withStyledAttributes
import com.jonaswanke.calendar.utils.*
import java.util.*
import kotlin.properties.Delegates


/**
 * TODO: document your custom view class.
 */
class RangeHeaderView @JvmOverloads constructor(
    context: Context,
    private val attrs: AttributeSet? = null,
    @AttrRes private val defStyleAttr: Int = R.attr.rangeHeaderViewStyle,
    @StyleRes private val defStyleRes: Int = R.style.Calendar_RangeHeaderViewStyle,
    _range: DayRange? = null
) : RangeViewStartIndicator(ContextThemeWrapper(context, defStyleRes), attrs, defStyleAttr) {

    companion object {
        private const val DATE_BOTTOM_FACTOR = 1.3f
        private const val WEEK_DAY_BOTTOM_FACTOR = 1.4f
        private const val TEXT_LEFT_FACTOR = .3f
    }

    var range: DayRange by Delegates.observable(_range ?: Day().range(WEEK_IN_DAYS)) { _, _, new ->
        onUpdateRange(new)
    }
    override var start: Day
        get() = range.start
        set(value) {
            range = value.range(range.length)
        }

    private var dateSize: Int = 0
    private var datePaint: TextPaint? = null
    private var dateCurrentPaint: TextPaint? = null
    private var dateFuturePaint: TextPaint? = null
    private var weekDaySize: Int = 0
    private var weekDayPaint: TextPaint? = null
    private var weekDayCurrentPaint: TextPaint? = null
    private var weekDayFuturePaint: TextPaint? = null
    private var weekDayStrings: Map<Int, String>

    private val cal: Calendar

    init {
        setWillNotDraw(false)

        context.withStyledAttributes(attrs, R.styleable.RangeHeaderView, defStyleAttr, defStyleRes) {
            dateSize = getDimensionPixelSize(R.styleable.RangeHeaderView_dateSize, 0)
            weekDaySize = getDimensionPixelSize(R.styleable.RangeHeaderView_weekDaySize, 0)
        }

        cal = start.start.toCalendar()
        weekDayStrings = cal.getDisplayNames(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())
                .map { (key, value) -> value to key }
                .toMap()
        onUpdateRange(range)
    }


    // View
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = paddingTop + paddingBottom + minimumHeight
        setMeasuredDimension(View.getDefaultSize(suggestedMinimumWidth, widthMeasureSpec), height)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null)
            return

        val left = paddingLeft
        val top = paddingTop.toFloat()
        val width = width - paddingRight - left

        cal.timeInMillis = start.start

        val dateBottom = top + dateSize * DATE_BOTTOM_FACTOR
        val weekDayBottom = top + dateSize * WEEK_DAY_BOTTOM_FACTOR + weekDaySize

        var index = 0
        for (day in range) {
            val (datePaint, weekDayPaint) = when {
                cal.isToday -> dateCurrentPaint to weekDayCurrentPaint
                cal.isFuture -> dateFuturePaint to weekDayFuturePaint
                else -> datePaint to weekDayPaint
            }
            if (datePaint == null)
                throw IllegalStateException("datePaint is null")
            if (weekDayPaint == null)
                throw IllegalStateException("datePaint is null")

            val text = weekDayStrings[cal.get(Calendar.DAY_OF_WEEK)]
                    ?: throw IllegalStateException("weekDayString for day ${cal.get(Calendar.DAY_OF_WEEK)} not found")
            val textLeft = left + width * index / range.length + TEXT_LEFT_FACTOR * dateSize
            canvas.drawText(cal.get(Calendar.DAY_OF_MONTH).toString(), textLeft, dateBottom, datePaint)
            canvas.drawText(text, textLeft, weekDayBottom, weekDayPaint)

            cal.add(Calendar.DAY_OF_WEEK, 1)
            index++
        }
    }


    // Custom
    @Suppress("ComplexMethod")
    private fun onUpdateRange(range: DayRange) {
        context.withStyledAttributes(attrs, R.styleable.RangeHeaderView, defStyleAttr, defStyleRes) {
            if (!start.isFuture) {
                if (datePaint == null)
                    datePaint = TextPaint().apply {
                        color = getColor(R.styleable.RangeHeaderView_dateColor, Color.BLACK)
                        isAntiAlias = true
                        textSize = dateSize.toFloat()
                    }
                if (weekDayPaint == null)
                    weekDayPaint = TextPaint().apply {
                        color = getColor(R.styleable.RangeHeaderView_weekDayColor, Color.BLACK)
                        isAntiAlias = true
                        textSize = weekDaySize.toFloat()
                    }
            }
            if (start.isToday || (!start.isFuture && range.endExclusive.isFuture)) {
                if (dateCurrentPaint == null)
                    dateCurrentPaint = TextPaint().apply {
                        color = getColor(R.styleable.RangeHeaderView_dateCurrentColor, Color.BLACK)
                        isAntiAlias = true
                        textSize = dateSize.toFloat()
                    }
                if (weekDayCurrentPaint == null)
                    weekDayCurrentPaint = TextPaint().apply {
                        color = getColor(R.styleable.RangeHeaderView_weekDayCurrentColor, Color.BLACK)
                        isAntiAlias = true
                        textSize = weekDaySize.toFloat()
                    }
            }
            if (range.endInclusive.isFuture) {
                if (dateFuturePaint == null)
                    dateFuturePaint = TextPaint().apply {
                        color = getColor(R.styleable.RangeHeaderView_dateFutureColor, Color.BLACK)
                        isAntiAlias = true
                        textSize = dateSize.toFloat()
                    }
                if (weekDayFuturePaint == null)
                    weekDayFuturePaint = TextPaint().apply {
                        color = getColor(R.styleable.RangeHeaderView_weekDayFutureColor, Color.BLACK)
                        isAntiAlias = true
                        textSize = weekDaySize.toFloat()
                    }
            }
        }
    }
}
