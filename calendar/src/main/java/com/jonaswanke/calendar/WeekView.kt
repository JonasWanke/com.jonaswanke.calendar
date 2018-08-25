package com.jonaswanke.calendar

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import java.util.*

/**
 * TODO: document your custom view class.
 */
class WeekView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    week: Week? = null
) : RangeView(context, attrs, defStyleAttr, week?.firstDay) {

    override var onScrollChangeListener: ((Int) -> Unit)?
        get() = scrollView.onScrollChangeListener
        set(value) {
            scrollView.onScrollChangeListener = value
        }

    override val range = WEEK_IN_DAYS

    private val headerView: RangeHeaderView
    private val allDayEventsView: AllDayEventsView
    private val scrollView: ReportingScrollView
    private val dayViews: List<DayEventsView>

    init {
        orientation = VERTICAL
        dividerDrawable = ContextCompat.getDrawable(context, android.R.drawable.divider_horizontal_bright)
        setWillNotDraw(false)

        headerView = RangeHeaderView(context, _start = start)
        addView(headerView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        allDayEventsView = AllDayEventsView(context, _start = start, _end = end)
        addView(allDayEventsView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        val dividerView = View(context).apply {
            setBackgroundResource(android.R.drawable.divider_horizontal_bright)
        }
        addView(dividerView, LayoutParams(LayoutParams.MATCH_PARENT, dividerView.background.intrinsicHeight))

        dayViews = WEEK_DAYS.map {
            DayEventsView(context, _day = Day(start.weekObj, mapBackDay(it)))
        }
        val daysWrapper = LinearLayout(context).apply {
            clipChildren = false
            for (day in dayViews)
                addView(day, LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        }
        scrollView = ReportingScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            addView(daysWrapper, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }
        addView(scrollView, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        onInitialized()
    }


    // View
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)

        headerHeight = headerView.measuredHeight + allDayEventsView.measuredHeight
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null)
            return

        val left = paddingLeft
        val top = paddingTop
        val right = width - paddingRight
        val bottom = height - paddingBottom

        val dayWidth = (right.toFloat() - left) / WEEK_IN_DAYS
        dividerDrawable?.also { divider ->
            for (day in WEEK_DAYS) {
                divider.setBounds((left + dayWidth * day).toInt(), top,
                        (left + dayWidth * day + divider.intrinsicWidth).toInt(), bottom)
                divider.draw(canvas)
            }
        }
    }


    // RangeView
    override fun updateListeners(
        onEventClickListener: ((Event) -> Unit)?,
        onEventLongClickListener: ((Event) -> Unit)?,
        onAddEventListener: ((AddEvent) -> Boolean)?
    ) {
        allDayEventsView.onEventClickListener = onEventClickListener
        allDayEventsView.onEventLongClickListener = onEventLongClickListener

        for (view in dayViews) {
            view.onEventClickListener = onEventClickListener
            view.onEventLongClickListener = onEventLongClickListener

            view.onAddEventViewListener = { event ->
                for (otherView in dayViews)
                    if (otherView != view)
                        otherView.removeAddEvent()
                onAddEventViewListener?.invoke(event)
            }
            view.onAddEventListener = onAddEventListener
        }
    }

    override fun onStartUpdated(start: Day, events: List<Event>) {
        headerView.start = start
        allDayEventsView.setRange(start, end, events.filter { showAsAllDay(it) })

        val byDays = distributeEvents(events.filter { !showAsAllDay(it) })
        for (day in WEEK_DAYS)
            dayViews[day].setDay(Day(start.weekObj, mapBackDay(day)), byDays[day])
    }

    override fun checkEvents(events: List<Event>) {
        if (events.any { it.end < start.start || it.start >= end.start })
            throw IllegalArgumentException("event starts must all be inside the set start")
    }

    override fun onEventsChanged(events: List<Event>) {
        allDayEventsView.setEvents(events.filter { showAsAllDay(it) })

        val byDays = distributeEvents(events.filter { !showAsAllDay(it) })
        for (day in WEEK_DAYS)
            dayViews[day].setEvents(byDays[day])
    }

    override fun onHourHeightChanged(height: Float?, heightMin: Float?, heightMax: Float?) {
        for (day in dayViews) {
            if (height != null) day.hourHeight = height
            if (heightMin != null) day.hourHeightMin = heightMin
            if (heightMax != null) day.hourHeightMax = heightMax
        }
    }

    override fun scrollTo(pos: Int) {
        scrollView.scrollY = pos
    }

    override fun removeAddEvent() {
        for (view in dayViews)
            view.removeAddEvent()
    }


    // Helpers
    /**
     * Maps a [Calendar] weekday ([Calendar.SUNDAY] through [Calendar.SATURDAY]) to the index of that day.
     */
    private fun mapDay(day: Int): Int = (day + WEEK_IN_DAYS - cal.firstDayOfWeek) % WEEK_IN_DAYS

    /**
     * Maps the index of a day back to the [Calendar] weekday ([Calendar.SUNDAY] through [Calendar.SATURDAY]).
     */
    private fun mapBackDay(day: Int): Int = (day + cal.firstDayOfWeek) % WEEK_IN_DAYS

    private fun distributeEvents(events: List<Event>): List<List<Event>> {
        val days = WEEK_DAYS.map { mutableListOf<Event>() }

        for (event in events) {
            val start = cal.daysUntil(event.start).coerceAtLeast(0)
            val end = cal.daysUntil(event.end).coerceIn(start until WEEK_IN_DAYS)
            for (day in start..end)
                days[day] += event
        }

        return days
    }
}
