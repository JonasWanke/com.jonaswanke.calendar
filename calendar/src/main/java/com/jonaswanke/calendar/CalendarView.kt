package com.jonaswanke.calendar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.support.annotation.AttrRes
import android.support.annotation.IntDef
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.view_calendar.view.*
import java.util.*
import kotlin.properties.Delegates

/**
 * TODO: document your custom view class.
 */
class CalendarView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, @AttrRes defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        const val RANGE_DAY = 1
        const val RANGE_3_DAYS = 3
        const val RANGE_WEEK = 7
        val RANGE_VALUES = intArrayOf(RANGE_DAY, RANGE_3_DAYS, RANGE_WEEK)
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(RANGE_DAY, RANGE_3_DAYS, RANGE_WEEK)
    annotation class Range


    var onEventClickListener: ((String) -> Unit)? = null
    var onEventLongClickListener: ((String) -> Unit)? = null

    var eventProvider: EventProvider = object : EventProvider {
        override fun provideEvents(year: Int, week: Int): List<Event> {
            return emptyList()
        }
    }

    @get: Range
    var range: Int by Delegates.observable(RANGE_WEEK) { _, old, new ->
        if (old == new)
            return@observable
        if (new !in RANGE_VALUES)
            throw UnsupportedOperationException()

        onRangeUpdated()
    }

    private var currentWeek: Week = Calendar.getInstance().toWeek()

    private val pagerAdapter: InfinitePagerAdapter<Week> = object : InfinitePagerAdapter<Week>(currentWeek) {
        override val nextIndicator: Week
            get() = currentIndicator.toCalendar().apply { add(Calendar.WEEK_OF_YEAR, 1) }.toWeek()
        override val previousIndicator: Week
            get() = currentIndicator.toCalendar().apply { add(Calendar.WEEK_OF_YEAR, -1) }.toWeek()

        override fun instantiateItem(indicator: Week): ViewGroup {
            return FrameLayout(context).apply {
                addView(WeekView(context).apply { week = indicator })
            }
        }
    }

    init {
        View.inflate(context, R.layout.view_calendar, this)

        // Load attributes
        val a = context.obtainStyledAttributes(
                attrs, R.styleable.CalendarView, defStyleAttr, 0)

        range = a.getInteger(R.styleable.CalendarView_range, RANGE_WEEK)

        a.recycle()

        pager.adapter = pagerAdapter
    }

    private fun onRangeUpdated() {
        when (range) {
            RANGE_DAY -> TODO()
            RANGE_3_DAYS -> TODO()
            RANGE_WEEK -> {

            }
            else -> throw UnsupportedOperationException()
        }
    }


    private val _paddingLeft = paddingLeft
    private val _paddingTop = paddingTop
    private val _paddingRight = paddingRight
    private val _paddingBottom = paddingBottom

    private val _contentWidth = width - paddingLeft - paddingRight
    private val _contentHeight = height - paddingTop - paddingBottom

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawRect(0.0f, 0.0f, 200.0f, 200.0f, Paint().apply { color = Color.RED })
    }

    interface EventProvider {
        fun provideEvents(year: Int, week: Int): List<Event>
    }
}
