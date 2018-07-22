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

    var eventRequestCallback: (Week) -> Unit = {}


    @get: Range
    var range: Int by Delegates.observable(RANGE_WEEK) { _, old, new ->
        if (old == new)
            return@observable
        if (new !in RANGE_VALUES)
            throw UnsupportedOperationException()

        onRangeUpdated()
    }

    private val events: MutableMap<Week, List<Event>> = mutableMapOf()
    private val weekViews: MutableMap<Week, WeekView> = mutableMapOf()

    private var currentWeek: Week = Week()

    private val pagerAdapter: InfinitePagerAdapter<Week> = object : InfinitePagerAdapter<Week>(currentWeek) {
        override fun nextIndicator(current: Week): Week {
            return currentIndicator.toCalendar().apply { add(Calendar.WEEK_OF_YEAR, 1) }.toWeek()
        }

        override fun previousIndicator(current: Week): Week {
            return currentIndicator.toCalendar().apply { add(Calendar.WEEK_OF_YEAR, -1) }.toWeek()
        }

        override var currentIndicatorString: String
            get() = "${currentIndicator.year}-${currentIndicator.week}"
            set(value) {
                val parts = value.split("-")
                currentIndicator = Week(parts[0].toInt(), parts[1].toInt())
            }

        override fun instantiateItem(indicator: Week): ViewGroup {
            val view = WeekView(context).apply {
                week = indicator
                events = this@CalendarView.events[week] ?: emptyList()
            }
            weekViews[indicator] = view
            eventRequestCallback(indicator)
            return view
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


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawRect(0.0f, 0.0f, 200.0f, 200.0f, Paint().apply { color = Color.RED })
    }


    fun setEventsForWeek(week: Week, events: List<Event>) {
        this.events[week] = events
        weekViews[week]?.events = events
    }
}
