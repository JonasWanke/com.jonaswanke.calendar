package com.jonaswanke.calendar

import android.content.Context
import android.support.annotation.AttrRes
import android.support.v4.content.ContextCompat
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import com.jonaswanke.calendar.R.id.week
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
            (getChildAt(day) as DayView).setEvents(getEventsForDay(day, new))
    }

    init {
        orientation = HORIZONTAL
        dividerDrawable = ContextCompat.getDrawable(context, android.R.drawable.divider_horizontal_bright)
        showDividers = SHOW_DIVIDER_BEGINNING or SHOW_DIVIDER_MIDDLE

        for (i in 0..6)
            addView(DayView(context, _day = Day(week, mapBackDay(i))).also {
                it.onEventClickListener = onEventClickListener
                it.onEventLongClickListener = onEventLongClickListener
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
    }


    fun setWeek(week: Week, events: List<Event> = emptyList()) {
        this.week = week
        for (day in 0 until 7)
            (getChildAt(day) as DayView).setDay(Day(week, mapBackDay(day)),
                    getEventsForDay(day, events))
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
        for (i in 0 until childCount) {
            val view = getChildAt(i) as DayView

            view.onEventClickListener = onEventClickListener
            view.onEventLongClickListener = onEventLongClickListener
        }
    }

    private fun getEventsForDay(day: Int, events: List<Event>): List<Event> {
        val forDay = mutableListOf<Event>()
        for (event in events)
            if (((event.start - week.start) / DateUtils.DAY_IN_MILLIS).toInt() == day)
                forDay.add(event)
        return forDay
    }
}
