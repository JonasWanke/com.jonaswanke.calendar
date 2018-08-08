package com.jonaswanke.calendar

import android.content.Context
import androidx.annotation.AttrRes
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
    attrs: AttributeSet? = null,
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
    private val eventData: MutableMap<Event, EventData> = mutableMapOf()
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
        val rowsHeight = (rows * (((getChildAt(0) as? EventView)?.minHeight ?: 0) + spacing)).toInt()
        val height = paddingTop + paddingBottom + Math.max(suggestedMinimumHeight, rowsHeight)
        setMeasuredDimension(View.getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
                height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val left = paddingLeft
        val top = paddingTop
        val right = r - l - paddingRight
        val bottom = b - t - paddingBottom
        val height = bottom - top
        val width = right - left

        fun getX(index: Int) = left + width * index / days
        fun getY(index: Int) = top + height * index / rows

        for (viewIndex in 0 until childCount) {
            val view = getChildAt(viewIndex) as EventView
            val event = view.event ?: continue
            val data = eventData[event] ?: continue

            view.layout((getX(data.start) + spacing).toInt(), getY(data.index),
                    getX(data.end + 1), (getY(data.index + 1) - spacing).toInt())
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
        eventData.clear()
        for (event in events) {
            val start = calStart.daysUntil(event.start).coerceAtLeast(0)
            val end = calStart.daysUntil(event.end).coerceIn(start, 6)
            eventData[event] = EventData(start, end)
        }
        this.events = events.sortedWith(compareBy({ eventData[it]?.start }, { eventData[it]?.length }))

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
                    addView(EventView(
                            this@AllDayEventsView.context,
                            defStyleAttr = R.attr.eventViewAllDayStyle,
                            defStyleRes = R.style.Calendar_EventViewStyle_AllDay).also {
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
        var currentGroup = mutableListOf<Event>()
        var currentEnd = 0
        fun endGroup() {
            when (currentGroup.size) {
                0 -> return
                1 -> eventData[currentGroup[0]]?.index = 0
                else -> {
                    val ends = mutableListOf<Int>()
                    for (event in currentGroup) {
                        val data = eventData[event] ?: continue
                        val min = ends.filter { it < data.start }.min()
                        val index = ends.indexOf(min)

                        if (index < 0) {
                            data.index = ends.size
                            ends.add(data.end)
                        } else {
                            data.index = index
                            ends[index] = data.end
                        }
                    }
                }
            }
        }
        for (event in events) {
            val data = eventData[event] ?: continue
            if (data.start <= currentEnd) {
                currentGroup.add(event)
                currentEnd = Math.max(currentEnd, data.end)
            } else {
                endGroup()
                currentGroup = mutableListOf(event)
                currentEnd = data.end
            }
        }
        endGroup()
        rows = (eventData.maxBy { it.value.index }?.value?.index ?: -1) + 1
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
        days = calStart.daysUntil(end.start)
        requestLayout()
    }

    private data class EventData(
        val start: Int,
        val end: Int,
        var index: Int = 0
    ) {
        val length = end - start
    }
}
