package com.jonaswanke.calendar

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import com.jonaswanke.calendar.utils.Day
import com.jonaswanke.calendar.utils.DayRange

/**
 * TODO: document your custom view class.
 */
class DayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    day: Day? = null
) : RangeView(context, attrs, defStyleAttr, 1, day) {

    override var onScrollChangeListener: ((Int) -> Unit)?
        get() = scrollView.onScrollChangeListener
        set(value) {
            scrollView.onScrollChangeListener = value
        }

    private val allDayEventsView: AllDayEventsView
    private val scrollView: ReportingScrollView
    private val eventView: DayEventsView

    init {
        orientation = VERTICAL
        dividerDrawable = ContextCompat.getDrawable(context, android.R.drawable.divider_horizontal_bright)
        setWillNotDraw(false)

        allDayEventsView = AllDayEventsView(context,
                defStyleAttr = R.attr.allDayEventsViewForDayStyle,
                defStyleRes = R.style.Calendar_AllDayEventsViewStyle_ForDay,
                _range = range)
        addView(allDayEventsView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        val dividerView = View(context).apply {
            setBackgroundResource(android.R.drawable.divider_horizontal_bright)
        }
        addView(dividerView, LayoutParams(LayoutParams.MATCH_PARENT, dividerView.background.intrinsicHeight))

        eventView = DayEventsView(context,
                defStyleAttr = R.attr.dayEventsViewForDayStyle,
                defStyleRes = R.style.Calendar_DayEventsViewStyle_ForDay,
                _day = range.start)
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
    override fun updateListeners() {
        allDayEventsView.setListeners(onEventClickListener, onEventLongClickListener)

        eventView.setListeners(onEventClickListener, onEventLongClickListener,
                onAddEventViewListener, onAddEventListener)
    }

    override fun onRangeUpdated(range: DayRange, events: List<Event>) {
        allDayEventsView.setRange(range, events.filter { showAsAllDay(it) })

        eventView.setDay(range.start, events.filter { !showAsAllDay(it) })
    }

    override fun checkEvents(events: List<Event>) {
        if (events.any { it.end < range.start.start || it.start >= range.endExclusive.start })
            throw IllegalArgumentException("event starts must all be inside the set start")
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
