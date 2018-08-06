package com.jonaswanke.calendar

import android.content.Context
import android.support.annotation.AttrRes
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import java.util.*
import kotlin.properties.Delegates

/**
 * TODO: document your custom view class.
 */
class AllDayEventsView @JvmOverloads constructor(
    context: Context,
    private val attrs: AttributeSet? = null,
    @AttrRes private val defStyleAttr: Int = R.attr.allDayEventsViewStyle,
    _start: Day? = null,
    _end: Day? = null
) : ViewGroup(context, attrs, defStyleAttr) {

    var onEventClickListener: ((Event) -> Unit)?
            by Delegates.observable<((Event) -> Unit)?>(null) { _, _, new ->
                updateListeners(new, onEventLongClickListener)
            }
    var onEventLongClickListener: ((Event) -> Unit)?
            by Delegates.observable<((Event) -> Unit)?>(null) { _, _, new ->
                updateListeners(onEventClickListener, new)
            }

    var start: Day = _start ?: Day()
        private set
    var end: Day = _end ?: start.nextDay
        private set
    private var days: Int = 0

    private var events: List<Event> = emptyList()
    private val eventRows: MutableMap<Event, Int> = mutableMapOf()
    private val eventTimes: MutableMap<Event, Pair<Int, Int>> = mutableMapOf()
    private var rows: Int = 0

    private val spacing: Float

    private lateinit var calStart: Calendar
    private lateinit var calEnd: Calendar

    init {
        val a = context.obtainStyledAttributes(
                attrs, R.styleable.AllDayEventsView, defStyleAttr, R.style.Calendar_AllDayEventsViewStyle)

        spacing = a.getDimension(R.styleable.AllDayEventsView_spacing, 0f)

        a.recycle()

        onUpdateRange(start, end)
    }

    override fun addView(child: View?, index: Int, params: LayoutParams?) {
        if (child !is EventView)
            throw IllegalArgumentException("Only EventViews may be children of AllDayEventsView")
        super.addView(child, index, params)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val view = if (childCount > 0)
            (getChildAt(0) as EventView)
        else
            EventView(context,
                    defStyleAttr = R.attr.eventViewAllDayStyle,
                    defStyleRes = R.style.Calendar_AllDayEventsViewStyle)
        val rowsHeight = (rows * (view.minHeight + spacing)).toInt()
        val height = paddingTop + paddingBottom + Math.max(suggestedMinimumHeight, rowsHeight)
        setMeasuredDimension(View.getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
                height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val left = paddingLeft + spacing
        val top = paddingTop
        val right = r - l - paddingRight
        val bottom = b - t - paddingBottom
        val height = bottom - top
        val width = right - left

        fun getX(index: Int) = top + height * index / rows
        fun getY(index: Int) = top + height * index / rows

        for (viewIndex in 0 until childCount) {
            val view = getChildAt(viewIndex) as EventView
            val event = view.event ?: continue
            val parallel = eventRows[event]!!
            val times = eventTimes[event]!!

            val eventWidth = width / parallel.first
            val eventLeft = left + eventWidth * parallel.second + space

            view.layout(eventLeft, getY(parallel), eventLeft + eventWidth - space, getY(parallel + 1))
        }
    }

    fun setRange(start: Day = this.start, end: Day = this.end, events: List<Event> = emptyList()) {
        this.start = start
        this.end = end
        onUpdateRange(start, end)

        setEvents(events)
    }

    fun setEvents(events: List<Event>) {
        checkEvents(events)
        this.events = events.sortedBy { it.start }

        launch(UI) {
            @Suppress("NAME_SHADOWING")
            val events = this@AllDayEventsView.events
            positionEvents()

            val existing = childCount
            for (i in 0 until events.size) {
                val event = events[i]

                if (existing > i)
                    (getChildAt(i) as EventView).event = event
                else
                    addView(EventView(this@AllDayEventsView.context, defStyleAttr = R.attr.eventViewAllDayStyle).also {
                        it.event = event
                    })
            }
            if (events.size < existing)
                removeViews(events.size, existing - events.size)
            updateListeners(onEventClickListener, onEventLongClickListener)
            requestLayout()
        }
    }

    private fun positionEvents() {
        eventRows.clear()
        eventTimes.clear()
        for (event in events)
            eventTimes[event] = calStart.daysUntil(event.start) to calStart.daysUntil(event.end)

        var currentGroup = mutableListOf<Event>()
        var currentEnd = 0
        fun endGroup() {
            when (currentGroup.size) {
                0 -> return
                1 -> eventRows[currentGroup[0]] = 0
                else -> {
                    val ends = mutableListOf<Int>()
                    for (event in currentGroup) {
                        val min = ends.filter { it < event.start }.min()
                        val index = ends.indexOf(min)

                        if (index < 0) {
                            eventRows[event] = ends.size
                            ends.add(eventTimes[event]!!.second)
                        } else {
                            eventRows[event] = index
                            ends[index] = eventTimes[event]!!.second
                        }
                    }
                }
            }
        }
        for (event in events)
            if (eventTimes[event]!!.first <= currentEnd) {
                currentGroup.add(event)
                currentEnd = Math.max(currentEnd, eventTimes[event]!!.second)
            } else {
                endGroup()
                currentGroup = mutableListOf(event)
                currentEnd = eventTimes[event]!!.second
            }
        endGroup()
        rows = (eventRows.maxBy { it.value }?.value ?: -1) + 1
    }

    private fun checkEvents(events: List<Event>) {
        if (events.any { event -> !event.allDay })
            throw IllegalArgumentException("only all-day events can be shown inside AllDayEventsView")
        if (events.any { event -> event.start >= end.start || event.end < start.start })
            throw IllegalArgumentException("event must all partly be inside the set range")
    }

    private fun updateListeners(
        onEventClickListener: ((Event) -> Unit)?,
        onEventLongClickListener: ((Event) -> Unit)?
    ) {
        for (i in 0 until childCount) {
            val view = getChildAt(i) as EventView
            val event = view.event
            if (event == null) {
                view.setOnClickListener(null)
                view.setOnLongClickListener(null)
                continue
            }

            onEventClickListener?.let { listener ->
                view.setOnClickListener {
                    listener(event)
                }
            } ?: view.setOnClickListener(null)
            onEventLongClickListener?.let { listener ->
                view.setOnLongClickListener {
                    listener(event)
                    true
                }
            } ?: view.setOnLongClickListener(null)
        }
    }

    private fun onUpdateRange(start: Day, end: Day) {
        calStart = start.start.asCalendar()
        calEnd = end.start.asCalendar()
        requestLayout()
    }
}
