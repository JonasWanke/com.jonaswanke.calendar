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
import java.util.*
import kotlin.properties.Delegates

/**
 * TODO: document your custom view class.
 */
class WeekHeaderView @JvmOverloads constructor(
    context: Context,
    private val attrs: AttributeSet? = null,
    @AttrRes private val defStyleAttr: Int = R.attr.weekHeaderViewStyle,
    @StyleRes defStyleRes: Int = R.style.Calendar_WeekHeaderViewStyle,
    _week: Week? = null
) : View(ContextThemeWrapper(context, defStyleRes), attrs, defStyleAttr) {


    var week: Week by Delegates.observable(_week ?: Week()) { _, _, new ->
        onUpdateWeek(new)
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

        cal = week.start.asCalendar()
        weekDayStrings = cal.getDisplayNames(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())
                .map { (key, value) -> value to key }
                .toMap()
        onUpdateWeek(week)
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
        val top = paddingTop.toFloat()
        val width = width - paddingRight - left

        cal.timeInMillis = week.start


        val dateBottom = top + dateSize * 1.3f
        val weekDayBottom = top + dateSize * 1.4f + weekDaySize

        for (day in 0 until 7) {
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
            val textLeft = left + width * day / 7 + .3f * dateSize
            canvas.drawText(cal.get(Calendar.DAY_OF_MONTH).toString(), textLeft, dateBottom, datePaint)
            canvas.drawText(text, textLeft, weekDayBottom, weekDayPaint)

            cal.add(Calendar.DAY_OF_WEEK, 1)
        }

        cal.timeInMillis = week.start
    }


    private fun onUpdateWeek(week: Week) {
        context.withStyledAttributes(attrs, R.styleable.WeekHeaderView, defStyleAttr, R.style.Calendar_WeekHeaderViewStyle) {
            dateSize = getDimensionPixelSize(R.styleable.WeekHeaderView_dateSize, 16)
            weekDaySize = getDimensionPixelSize(R.styleable.WeekHeaderView_weekDaySize, 16)

            if (!week.isFuture) {
                if (datePaint == null)
                    datePaint = TextPaint().apply {
                        color = getColor(R.styleable.WeekHeaderView_dateColor, Color.BLACK)
                        isAntiAlias = true
                        textSize = dateSize.toFloat()
                    }
                if (weekDayPaint == null)
                    weekDayPaint = TextPaint().apply {
                        color = getColor(R.styleable.WeekHeaderView_weekDayColor, Color.BLACK)
                        isAntiAlias = true
                        textSize = weekDaySize.toFloat()
                    }
            }
            if (week.isToday || (!week.isFuture && week.nextWeek.isFuture)) {
                if (dateCurrentPaint == null)
                    dateCurrentPaint = TextPaint().apply {
                        color = getColor(R.styleable.WeekHeaderView_dateCurrentColor, Color.BLACK)
                        isAntiAlias = true
                        textSize = dateSize.toFloat()
                    }
                if (weekDayCurrentPaint == null)
                    weekDayCurrentPaint = TextPaint().apply {
                        color = getColor(R.styleable.WeekHeaderView_weekDayCurrentColor, Color.BLACK)
                        isAntiAlias = true
                        textSize = weekDaySize.toFloat()
                    }
            }
            // False-positive if today's the last day of the week
            if (week.nextWeek.isFuture) {
                if (dateFuturePaint == null)
                    dateFuturePaint = TextPaint().apply {
                        color = getColor(R.styleable.WeekHeaderView_dateFutureColor, Color.BLACK)
                        isAntiAlias = true
                        textSize = dateSize.toFloat()
                    }
                if (weekDayFuturePaint == null)
                    weekDayFuturePaint = TextPaint().apply {
                        color = getColor(R.styleable.WeekHeaderView_weekDayFutureColor, Color.BLACK)
                        isAntiAlias = true
                        textSize = weekDaySize.toFloat()
                    }
            }
        }
    }
}
