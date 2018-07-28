package com.jonaswanke.calendar

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.support.annotation.AttrRes
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import com.jonaswanke.calendar.R.attr.hourHeight
import java.util.*
import kotlin.properties.Delegates

/**
 * TODO: document your custom view class.
 */
class WeekView @JvmOverloads constructor(context: Context,
                                         attrs: AttributeSet? = null,
                                         @AttrRes defStyleAttr: Int = 0,
                                         _week: Week? = null)
    : LinearLayout(context, attrs, defStyleAttr) {

    var onEventClickListener: ((Event) -> Unit)?
            by Delegates.observable<((Event) -> Unit)?>(null) { _, _, new ->
                updateListeners(new, onEventLongClickListener)
            }
    var onEventLongClickListener: ((Event) -> Unit)?
            by Delegates.observable<((Event) -> Unit)?>(null) { _, _, new ->
                updateListeners(onEventClickListener, new)
            }

    var week: Week = _week ?: Week()
        private set
    var events: List<Event> by Delegates.observable(emptyList()) { _, old, new ->
        if (old == new)
            return@observable
        if (new.any { event -> event.start < week.start || event.start >= week.end })
            throw IllegalArgumentException("event starts must all be inside the set week")

        for (day in 0 until 7)
            dayViews[day].setEvents(getEventsForDay(day, new))
    }

    private val headerView: WeekHeaderView
    private val dayViews: List<DayView>

    init {
        orientation = VERTICAL
        dividerDrawable = ContextCompat.getDrawable(context, android.R.drawable.divider_horizontal_bright)
        showDividers = SHOW_DIVIDER_MIDDLE

        headerView = WeekHeaderView(context, _week = week)

        dayViews = (0 until 7).map {
            DayView(context, _day = Day(week, mapBackDay(it))).also {
                it.onEventClickListener = onEventClickListener
                it.onEventLongClickListener = onEventLongClickListener
            }
        }

        val headerHeight = context.resources.getDimensionPixelOffset(R.dimen.calendar_headerHeight)
        addView(headerView, LayoutParams(LayoutParams.MATCH_PARENT, headerHeight))
        val daysWrapper = LinearLayout(context).apply {
            for (day in dayViews)
                addView(day, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        }
        addView(daysWrapper, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null)
            return

        val left = paddingLeft
        val top = paddingTop
        val right = canvas.width - paddingRight
        val bottom = canvas.height - paddingBottom

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
        this.cal = week.toCalendar()

        this.headerView.week = week
        for (day in 0 until 7)
            dayViews[day].setDay(Day(week, mapBackDay(day)), getEventsForDay(day, events))
        this.events = events
    }


    /**
     * Maps a [Calendar] weekday ([Calendar.SUNDAY] through [Calendar.SATURDAY]) to the index of that day.
     */
    private fun mapDay(day: Int): Int = (day + 7 - CAL_START_OF_WEEK) % 7

    /**
     * Maps the index of a day back to the [Calendar] weekday ([Calendar.SUNDAY] through [Calendar.SATURDAY]).
     */
    private fun mapBackDay(day: Int): Int = (day + CAL_START_OF_WEEK) % 7

    private fun updateListeners(onEventClickListener: ((Event) -> Unit)?,
                                onEventLongClickListener: ((Event) -> Unit)?) {
        for (day in dayViews) {
            day.onEventClickListener = onEventClickListener
            day.onEventLongClickListener = onEventLongClickListener
        }
    }

    var cal = week.toCalendar()
    private fun getEventsForDay(day: Int, events: List<Event>): List<Event> {
        val start = cal.apply { add(Calendar.DAY_OF_WEEK, day) }.timeInMillis
        val end = cal.apply { add(Calendar.DAY_OF_WEEK, 1) }.timeInMillis
        cal.add(Calendar.DAY_OF_WEEK, -(day + 1))

        val forDay = mutableListOf<Event>()
        for (event in events)
            if (event.start in start until end - 1)
                forDay.add(event)
        return forDay
    }
}
