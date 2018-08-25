package com.jonaswanke.calendar

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.content.withStyledAttributes
import androidx.core.view.children
import androidx.core.view.get
import com.jonaswanke.calendar.RangeView.Companion.showAsAllDay
import com.jonaswanke.calendar.utils.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import java.util.*
import kotlin.math.max
import kotlin.properties.Delegates

/**
 * TODO: document your custom view class.
 */
class AllDayEventsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes private val defStyleAttr: Int = R.attr.allDayEventsViewStyle,
    @StyleRes private val defStyleRes: Int = R.style.Calendar_AllDayEventsViewStyle,
    _range: DayRange? = null
) : ViewGroup(ContextThemeWrapper(context, defStyleRes), attrs, defStyleAttr) {

    var onEventClickListener: ((Event) -> Unit)?
            by Delegates.observable<((Event) -> Unit)?>(null) { _, _, new ->
                updateListeners(new, onEventLongClickListener)
            }
    var onEventLongClickListener: ((Event) -> Unit)?
            by Delegates.observable<((Event) -> Unit)?>(null) { _, _, new ->
                updateListeners(onEventClickListener, new)
            }

    var range: DayRange = _range ?: Day().range(1)
        private set

    private var events: List<Event> = emptyList()
    private val eventData: MutableMap<Event, EventData> = mutableMapOf()
    private var rows: Int = 0

    private var spacing: Float = 0f

    private lateinit var calStart: Calendar
    private lateinit var calEnd: Calendar

    init {
        context.withStyledAttributes(attrs, R.styleable.AllDayEventsView, defStyleAttr, defStyleRes) {
            spacing = getDimension(R.styleable.AllDayEventsView_eventSpacing, 0f)
        }

        onUpdateRange(range)
    }

    override fun addView(child: View?, index: Int, params: LayoutParams?) {
        if (child !is EventView)
            throw IllegalArgumentException("Only EventViews may be children of AllDayEventsView")
        super.addView(child, index, params)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val rowsHeight = (rows * (((getChildAt(0) as? EventView)?.minHeight ?: 0) + spacing)).toInt()
        val height = paddingTop + paddingBottom + max(suggestedMinimumHeight, rowsHeight)
        setMeasuredDimension(View.getDefaultSize(suggestedMinimumWidth, widthMeasureSpec), height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val left = paddingLeft
        val top = paddingTop
        val right = r - l - paddingRight
        val bottom = b - t - paddingBottom
        val width = right - left
        val eventHeight = (getChildAt(0) as? EventView)?.minHeight?.plus(spacing)
                ?.coerceAtMost((bottom - top).toFloat() / rows)
                ?: return

        fun getX(index: Int) = left + width * index / range.length
        fun getY(index: Int) = top + eventHeight * index

        for (view in children) {
            val eventView = view as EventView
            val event = eventView.event ?: continue
            val data = eventData[event] ?: continue

            eventView.layout((getX(data.start) + spacing).toInt(), getY(data.index).toInt(),
                    getX(data.end + 1), (getY(data.index + 1) - spacing).toInt())
        }
    }

    fun setRange(range: DayRange, events: List<Event> = emptyList()) {
        this.range = range
        onUpdateRange(range)

        setEvents(events)
    }

    fun setEvents(events: List<Event>) {
        checkEvents(events)
        eventData.clear()
        for (event in events) {
            val start = calStart.daysUntil(event.start).coerceAtLeast(0)
            val end = calStart.daysUntil(event.end).coerceIn(start until WEEK_IN_DAYS)
            eventData[event] = EventData(start, end)
        }
        val sortedEvents = events.sortedWith(compareBy({ eventData[it]?.start },
                { -(eventData[it]?.end ?: Int.MIN_VALUE) }))
        this.events = sortedEvents

        launch(UI) {
            @Suppress("NAME_SHADOWING")
            positionEvents()

            val existing = childCount
            for (i in 0 until sortedEvents.size) {
                val event = sortedEvents[i]

                if (existing > i)
                    (this@AllDayEventsView[i] as EventView).event = event
                else
                    addView(EventView(
                            this@AllDayEventsView.context,
                            defStyleAttr = R.attr.eventViewAllDayStyle,
                            defStyleRes = R.style.Calendar_EventViewStyle_AllDay).also {
                        it.event = event
                    })
            }
            if (sortedEvents.size < existing)
                removeViews(sortedEvents.size, existing - sortedEvents.size)
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
                currentEnd = max(currentEnd, data.end)
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
        if (events.any { !showAsAllDay(it) })
            throw IllegalArgumentException("only all-day events can be shown inside AllDayEventsView")
        if (events.any { it.end < range.start.start || it.start >= range.endExclusive.start })
            throw IllegalArgumentException("event must all partly be inside the set length")
    }

    private fun updateListeners(
        onEventClickListener: ((Event) -> Unit)?,
        onEventLongClickListener: ((Event) -> Unit)?
    ) {
        for (view in children) {
            val eventView = view as EventView
            val event = eventView.event
            if (event == null) {
                eventView.setOnClickListener(null)
                eventView.setOnLongClickListener(null)
                continue
            }

            onEventClickListener?.let { listener ->
                eventView.setOnClickListener {
                    listener(event)
                }
            } ?: eventView.setOnClickListener(null)
            onEventLongClickListener?.let { listener ->
                eventView.setOnLongClickListener {
                    listener(event)
                    true
                }
            } ?: eventView.setOnLongClickListener(null)
        }
    }

    private fun onUpdateRange(range: DayRange) {
        calStart = range.start.toCalendar()
        calEnd = range.endExclusive.toCalendar()
        requestLayout()
    }

    private data class EventData(
        val start: Int,
        val end: Int,
        var index: Int = 0
    )
}
