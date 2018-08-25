package com.jonaswanke.calendar

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat

/**
 * TODO: document your custom view class.
 */
class DayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    day: Day? = null
) : RangeView(context, attrs, defStyleAttr, day) {

    override var onScrollChangeListener: ((Int) -> Unit)?
        get() = scrollView.onScrollChangeListener
        set(value) {
            scrollView.onScrollChangeListener = value
        }

    override val range = 1

    private val allDayEventsView: AllDayEventsView
    private val scrollView: ReportingScrollView
    private val eventView: DayEventsView

    init {
        orientation = VERTICAL
        dividerDrawable = ContextCompat.getDrawable(context, android.R.drawable.divider_horizontal_bright)
        setWillNotDraw(false)

        allDayEventsView = AllDayEventsView(context, _start = start, _end = end)
        addView(allDayEventsView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        val dividerView = View(context).apply {
            setBackgroundResource(android.R.drawable.divider_horizontal_bright)
        }
        addView(dividerView, LayoutParams(LayoutParams.MATCH_PARENT, dividerView.background.intrinsicHeight))

        eventView = DayEventsView(context, defStyleAttr = R.attr.dayEventsViewOnlyDayStyle,
                defStyleRes = R.style.Calendar_DayEventsViewStyle_OnlyDay,
                _day = start)
        val daysWrapper = LinearLayout(context).apply {
            clipChildren = false
            addView(eventView, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
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

        headerHeight = allDayEventsView.measuredHeight
    }


    // RangeView
    override fun updateListeners(
        onEventClickListener: ((Event) -> Unit)?,
        onEventLongClickListener: ((Event) -> Unit)?,
        onAddEventListener: ((AddEvent) -> Boolean)?
    ) {
        allDayEventsView.onEventClickListener = onEventClickListener
        allDayEventsView.onEventLongClickListener = onEventLongClickListener

        eventView.onEventClickListener = onEventClickListener
        eventView.onEventLongClickListener = onEventLongClickListener

        eventView.onAddEventViewListener = onAddEventViewListener
        eventView.onAddEventListener = onAddEventListener
    }

    override fun onStartUpdated(start: Day, events: List<Event>) {
        allDayEventsView.setRange(start, end, events.filter { showAsAllDay(it) })

        eventView.setDay(start, events.filter { !showAsAllDay(it) })
    }

    override fun checkEvents(events: List<Event>) {
        if (events.any { it.end < start.start || it.start >= end.start })
            throw IllegalArgumentException("event starts must all be inside the set week")
    }

    override fun onEventsChanged(events: List<Event>) {
        allDayEventsView.setEvents(events.filter { showAsAllDay(it) })

        eventView.setEvents(events.filter { !showAsAllDay(it) })
    }

    override fun onHourHeightChanged(height: Float?, heightMin: Float?, heightMax: Float?) {
        if (height != null) eventView.hourHeight = height
        if (heightMin != null) eventView.hourHeightMin = heightMin
        if (heightMax != null) eventView.hourHeightMax = heightMax
    }

    override fun scrollTo(pos: Int) {
        scrollView.scrollY = pos
    }

    override fun removeAddEvent() {
        eventView.removeAddEvent()
    }
}
