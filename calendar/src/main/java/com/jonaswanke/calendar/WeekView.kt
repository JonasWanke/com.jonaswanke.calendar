package com.jonaswanke.calendar

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import java.util.*
import kotlin.properties.Delegates

/**
 * TODO: document your custom view class.
 */
class WeekView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    _week: Week? = null
) : LinearLayout(context, attrs, defStyleAttr) {

    var onEventClickListener: ((Event) -> Unit)?
            by Delegates.observable<((Event) -> Unit)?>(null) { _, _, new ->
                updateListeners(new, onEventLongClickListener, onAddEventListener)
            }
    var onEventLongClickListener: ((Event) -> Unit)?
            by Delegates.observable<((Event) -> Unit)?>(null) { _, _, new ->
                updateListeners(onEventClickListener, new, onAddEventListener)
            }
    internal var onAddEventViewListener: ((AddEvent) -> Unit)? = null
    var onAddEventListener: ((AddEvent) -> Boolean)?
            by Delegates.observable<((AddEvent) -> Boolean)?>(null) { _, _, new ->
                updateListeners(onEventClickListener, onEventLongClickListener, new)
            }
    var onHeaderHeightChangeListener: ((Int) -> Unit)? = null
    var onScrollChangeListener: ((Int) -> Unit)?
        get() = scrollView.onScrollChangeListener
        set(value) {
            scrollView.onScrollChangeListener = value
        }

    var week: Week = _week ?: Week()
        private set
    private val range: Pair<Day, Day>
        get() {
            return Day(week.year, week.week, cal.firstDayOfWeek) to
                    Day(week.nextWeek.year, week.nextWeek.week, cal.firstDayOfWeek)
        }
    var events: List<Event> by Delegates.observable(emptyList()) { _, old, new ->
        if (old == new)
            return@observable
        checkEvents(new)

        allDayEventsView.setEvents(new.filter { it.allDay })

        val byDays = distributeEvents(new.filter { !it.allDay })
        for (day in 0 until 7)
            dayViews[day].setEvents(byDays[day])
    }

    private var cal: Calendar

    var headerHeight: Int = 0
        private set
    var hourHeight: Float
        get() = dayViews[0].hourHeight
        set(value) {
            for (day in dayViews)
                day.hourHeight = value
        }
    var hourHeightMin: Float
        get() = dayViews[0].hourHeightMin
        set(value) {
            for (day in dayViews)
                day.hourHeightMin = value
        }
    var hourHeightMax: Float
        get() = dayViews[0].hourHeightMax
        set(value) {
            for (day in dayViews)
                day.hourHeightMax = value
        }

    private val headerView: WeekHeaderView
    private val allDayEventsView: AllDayEventsView
    private val scrollView: ReportingScrollView
    private val dayViews: List<DayView>

    init {
        orientation = VERTICAL
        dividerDrawable = ContextCompat.getDrawable(context, android.R.drawable.divider_horizontal_bright)
        setWillNotDraw(false)

        cal = week.toCalendar()

        headerView = WeekHeaderView(context, _week = week)
        addView(headerView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        val range = range
        allDayEventsView = AllDayEventsView(context, _start = range.first, _end = range.second)
        addView(allDayEventsView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        val dividerView = View(context).apply {
            setBackgroundResource(android.R.drawable.divider_horizontal_bright)
        }
        addView(dividerView, LayoutParams(LayoutParams.MATCH_PARENT, dividerView.background.intrinsicHeight))

        dayViews = (0 until 7).map {
            DayView(context, _day = Day(week, mapBackDay(it))).also {
                it.onEventClickListener = onEventClickListener
                it.onEventLongClickListener = onEventLongClickListener
            }
        }
        dayViews.forEach {
            it.onAddEventViewListener = { event ->
                for (view in dayViews)
                    if (view != it)
                        view.removeAddEvent()
                onAddEventViewListener?.invoke(event)
            }
        }
        val daysWrapper = LinearLayout(context).apply {
            clipChildren = false
            for (day in dayViews)
                addView(day, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        }
        scrollView = ReportingScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            addView(daysWrapper, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }
        addView(scrollView, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)

        val headerHeightNew = headerView.measuredHeight + allDayEventsView.measuredHeight
        if (headerHeight != headerHeightNew) {
            headerHeight = headerHeightNew
            onHeaderHeightChangeListener?.invoke(headerHeight)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null)
            return

        val left = paddingLeft
        val top = paddingTop
        val right = width - paddingRight
        val bottom = height - paddingBottom

        val dayWidth = (right.toFloat() - left) / 7
        dividerDrawable?.also { divider ->
            for (day in 0 until 7) {
                divider.setBounds((left + dayWidth * day).toInt(), top,
                        (left + dayWidth * day + divider.intrinsicWidth).toInt(), bottom)
                divider.draw(canvas)
            }
        }
    }


    fun setWeek(week: Week, events: List<Event> = emptyList()) {
        this.week = week
        cal = week.toCalendar()

        removeAddEvent()
        checkEvents(events)
        headerView.week = week

        val range = range
        allDayEventsView.setRange(range.first, range.second, events.filter { it.allDay })

        val byDays = distributeEvents(events.filter { !it.allDay })
        for (day in 0 until 7)
            dayViews[day].setDay(Day(week, mapBackDay(day)), byDays[day])
        this.events = events
    }

    fun scrollTo(pos: Int) {
        scrollView.scrollY = pos
    }

    fun removeAddEvent() {
        for (view in dayViews)
            view.removeAddEvent()
    }


    private fun checkEvents(events: List<Event>) {
        if (events.any { event -> event.start < week.start || event.start >= week.end })
            throw IllegalArgumentException("event starts must all be inside the set week")
    }

    /**
     * Maps a [Calendar] weekday ([Calendar.SUNDAY] through [Calendar.SATURDAY]) to the index of that day.
     */
    private fun mapDay(day: Int): Int = (day + 7 - cal.firstDayOfWeek) % 7

    /**
     * Maps the index of a day back to the [Calendar] weekday ([Calendar.SUNDAY] through [Calendar.SATURDAY]).
     */
    private fun mapBackDay(day: Int): Int = (day + cal.firstDayOfWeek) % 7

    private fun updateListeners(
        onEventClickListener: ((Event) -> Unit)?,
        onEventLongClickListener: ((Event) -> Unit)?,
        onAddEventListener: ((AddEvent) -> Boolean)?
    ) {
        allDayEventsView.onEventClickListener = onEventClickListener
        allDayEventsView.onEventLongClickListener = onEventLongClickListener
        for (view in dayViews) {
            view.onEventClickListener = onEventClickListener
            view.onEventLongClickListener = onEventLongClickListener
            view.onAddEventListener = onAddEventListener
        }
    }

    private fun distributeEvents(events: List<Event>): List<List<Event>> {
        val days = (0 until 7).map { mutableListOf<Event>() }

        for (event in events)
            days[cal.daysUntil(event.start)] += event

        return days
    }
}
