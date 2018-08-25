package com.jonaswanke.calendar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.view.children
import androidx.core.view.get
import com.jonaswanke.calendar.RangeView.Companion.showAsAllDay
import com.jonaswanke.calendar.utils.DAY_IN_HOURS
import com.jonaswanke.calendar.utils.Day
import com.jonaswanke.calendar.utils.timeOfDay
import com.jonaswanke.calendar.utils.toCalendar
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.properties.Delegates

/**
 * TODO: document your custom view class.
 */
@Suppress("LargeClass")
class DayEventsView @JvmOverloads constructor(
    context: Context,
    private val attrs: AttributeSet? = null,
    @AttrRes private val defStyleAttr: Int = R.attr.dayEventsViewStyle,
    @StyleRes private val defStyleRes: Int = R.style.Calendar_DayEventsViewStyle,
    _day: Day? = null
) : ViewGroup(ContextThemeWrapper(context, defStyleRes), attrs, defStyleAttr) {
    companion object {
        private const val EVENT_POSITIONING_DEBOUNCE = 500L
    }

    var onEventClickListener: ((Event) -> Unit)?
            by Delegates.observable<((Event) -> Unit)?>(null) { _, _, new ->
                updateListeners(new, onEventLongClickListener)
            }
    var onEventLongClickListener: ((Event) -> Unit)?
            by Delegates.observable<((Event) -> Unit)?>(null) { _, _, new ->
                updateListeners(onEventClickListener, new)
            }
    internal var onAddEventViewListener: ((AddEvent) -> Unit)? = null
    var onAddEventListener: ((AddEvent) -> Boolean)?
            by Delegates.observable<((AddEvent) -> Boolean)?>(null) { _, _, new ->
                if (new == null)
                    removeAddEvent()
            }

    var day: Day = _day ?: Day()
        private set
    private var events: List<Event> = emptyList()
    private val eventData: MutableMap<Event, EventData> = mutableMapOf()
    private var addEventView: EventView? = null


    private var timeCircleRadius: Int = 0
    private var timeLineSize: Int = 0
    private lateinit var timePaint: Paint

    private var _hourHeight: Float = 0f
    var hourHeight: Float
        get() = _hourHeight
        set(value) {
            val v = value.coerceIn(if (hourHeightMin > 0) hourHeightMin else null,
                    if (hourHeightMax > 0) hourHeightMax else null)
            if (_hourHeight == v)
                return

            _hourHeight = v
            invalidate()
            requestPositionEventsAndLayout()
        }
    var hourHeightMin: Float by Delegates.observable(0f) { _, _, new ->
        if (new > 0 && hourHeight < new)
            hourHeight = new
    }
    var hourHeightMax: Float by Delegates.observable(0f) { _, _, new ->
        if (new > 0 && hourHeight > new)
            hourHeight = new
    }
    private var eventPositionRequired: Boolean = false
    private var eventPositionJob: Job? = null

    private var dividerPadding: Int = 0

    private var eventSpacing: Float = 0f
    private var eventStackOverlap: Float = 0f

    internal var divider by Delegates.observable<Drawable?>(null) { _, _, new ->
        dividerHeight = new?.intrinsicHeight ?: 0
    }
        private set
    private var dividerHeight: Int = 0

    private val cal: Calendar

    init {
        setWillNotDraw(false)

        context.withStyledAttributes(attrs, R.styleable.DayEventsView, defStyleAttr, defStyleRes) {
            _hourHeight = getDimension(R.styleable.DayEventsView_hourHeight, 0f)
            hourHeightMin = getDimension(R.styleable.DayEventsView_hourHeightMin, 0f)
            hourHeightMax = getDimension(R.styleable.DayEventsView_hourHeightMax, 0f)

            dividerPadding = getDimensionPixelOffset(R.styleable.DayEventsView_dividerPadding, 0)

            eventSpacing = getDimension(R.styleable.DayEventsView_eventSpacing, 0f)
            eventStackOverlap = getDimension(R.styleable.DayEventsView_eventStackOverlap, 0f)
        }

        onUpdateDay(day)
        cal = day.start.toCalendar()
        launch(UI) {
            divider = ContextCompat.getDrawable(context, android.R.drawable.divider_horizontal_bright)
            invalidate()
        }

        setOnTouchListener { _, motionEvent ->
            if (onAddEventListener == null)
                return@setOnTouchListener false

            if (motionEvent.action == MotionEvent.ACTION_UP) {
                fun hourToTime(hour: Int) = day.start + hour * DateUtils.HOUR_IN_MILLIS

                val hour = (motionEvent.y / hourHeight).toInt()
                val event = AddEvent(hourToTime(hour), hourToTime(hour + 1))
                eventData[event] = EventData((hour * DateUtils.HOUR_IN_MILLIS).toInt(),
                        ((hour + 1) * DateUtils.HOUR_IN_MILLIS).toInt())

                val view = addEventView
                if (view == null) {
                    addView(EventView(context,
                            defStyleAttr = R.attr.eventViewAddStyle,
                            defStyleRes = R.style.Calendar_EventViewStyle_Add,
                            _event = event).apply {
                        setOnClickListener {
                            if (onAddEventListener?.invoke(event) == true)
                                removeAddEvent()
                        }
                    })
                } else {
                    view.event = event
                    requestLayout()
                }
                onAddEventViewListener?.invoke(event)
            }
            return@setOnTouchListener motionEvent.action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)
        }
    }


    // View
    override fun addView(child: View?, index: Int, params: LayoutParams?) {
        if (child !is EventView)
            throw IllegalArgumentException("Only EventViews may be children of DayEventsView")
        if (child.event is AddEvent)
            if (addEventView != null && addEventView != child)
                throw  IllegalStateException("DayEventsView may only contain one add-EventView")
            else {
                addEventView = child
                onAddEventViewListener?.invoke(child.event as AddEvent)
            }
        super.addView(child, index, params)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = paddingTop + paddingBottom + max(suggestedMinimumHeight, (_hourHeight * DAY_IN_HOURS).toInt())
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

        fun getPosForTime(time: Long): Int {
            return when {
                time < day.start -> 0
                time >= day.end -> height
                else -> (height * cal.apply { timeInMillis = time }.timeOfDay / DateUtils.DAY_IN_MILLIS).toInt()
            }
        }

        for (view in children) {
            val eventView = view as EventView
            val event = eventView.event ?: continue

            val data = eventData[event] ?: continue
            val eventTop = min(top + getPosForTime(event.start), bottom - eventView.minHeight).toFloat()
            // Fix if event ends on next day
            val eventBottom = if (event.end >= day.nextDay.start) bottom.toFloat()
            else max(top + getPosForTime(event.end) - eventSpacing, eventTop + eventView.minHeight)
            val subGroupWidth = width / data.parallel
            val subGroupLeft = left + subGroupWidth * data.index + eventSpacing

            eventView.layout((subGroupLeft + data.subIndex * eventSpacing).toInt(), eventTop.toInt(),
                    (subGroupLeft + subGroupWidth - eventSpacing).toInt(), eventBottom.toInt())
        }
    }

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)
        if (canvas == null)
            return

        val left = (dividerPadding - timeCircleRadius).coerceAtLeast(0)
        val top = paddingTop
        val right = width - paddingRight
        val bottom = height - paddingBottom

        if (day.isToday) {
            val time = Calendar.getInstance().timeOfDay
            val posY = top + (bottom.toFloat() - top) * time / DateUtils.DAY_IN_MILLIS
            canvas.drawCircle(left.toFloat(), posY, timeCircleRadius.toFloat(), timePaint)
            canvas.drawRect(left.toFloat(), posY - timeLineSize / 2,
                    right.toFloat(), posY + timeLineSize / 2, timePaint)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null)
            return

        val left = dividerPadding
        val top = paddingTop
        val right = width - dividerPadding

        for (hour in 1 until DAY_IN_HOURS) {
            divider?.setBounds(left, (top + _hourHeight * hour).toInt(),
                    right, (top + _hourHeight * hour + dividerHeight).toInt())
            divider?.draw(canvas)
        }
    }


    // Custom
    fun setDay(day: Day, events: List<Event> = emptyList()) {
        this.day = day
        onUpdateDay(day)

        removeAddEvent()
        setEvents(events)
    }

    fun setEvents(events: List<Event>) {
        checkEvents(events)

        regenerateBaseEventData(events)
        this.events = events.sortedWith(compareBy({ eventData[it]?.start },
                { -(eventData[it]?.end ?: Int.MIN_VALUE) }))

        launch(UI) {
            @Suppress("NAME_SHADOWING")
            val events = this@DayEventsView.events
            positionEvents()

            if (addEventView != null)
                removeView(addEventView)

            val existing = childCount
            for (i in 0 until events.size) {
                val event = events[i]

                if (existing > i)
                    (this@DayEventsView[i] as EventView).event = event
                else
                    addView(EventView(this@DayEventsView.context).also {
                        it.event = event
                    })
            }
            if (events.size < existing)
                removeViews(events.size, existing - events.size)
            if (addEventView != null)
                addView(addEventView)

            updateListeners(onEventClickListener, onEventLongClickListener)
            requestLayout()
        }
    }

    fun removeAddEvent() {
        addEventView?.also {
            removeView(it)
            addEventView = null
        }
    }


    // Helpers
    private fun regenerateBaseEventData(events: List<Event>) {
        val view = if (childCount > 0) (this[0] as EventView) else EventView(context)
        val minLength = (view.minHeight / hourHeight * DateUtils.HOUR_IN_MILLIS).toLong()

        eventData.clear()
        for (event in events + addEventView?.event) {
            if (event == null)
                continue

            val start = (event.start - day.start).coerceIn(0, DateUtils.DAY_IN_MILLIS - minLength).toInt()
            eventData[event] = EventData(start,
                    (event.end - day.start).coerceIn(start + minLength, DateUtils.DAY_IN_MILLIS).toInt())
        }
    }

    @Suppress("ComplexMethod", "NestedBlockDepth")
    private fun positionEvents() {
        val view = if (childCount > 0) (this[0] as EventView) else EventView(context)
        val minLength = (view.minHeight / hourHeight * DateUtils.HOUR_IN_MILLIS).toLong()
        val spacing = eventSpacing / hourHeight * DateUtils.HOUR_IN_MILLIS
        val stackOverlap = eventStackOverlap / hourHeight * DateUtils.HOUR_IN_MILLIS

        regenerateBaseEventData(events)

        fun endOfNoSpacing(event: Event) = (event.end - day.start)
                .coerceIn((eventData[event]?.start ?: 0) + minLength - spacing.toLong(), DateUtils.DAY_IN_MILLIS)

        var currentGroup = mutableListOf<Event>()
        fun endGroup() {
            when (currentGroup.size) {
                0 -> return
                1 -> return
                else -> {
                    val columns = mutableListOf<MutableList<Event>>()
                    for (event in currentGroup) {
                        val data = eventData[event] ?: continue

                        var minIndex = Int.MIN_VALUE
                        var minSubIndex = Int.MAX_VALUE
                        var minTop = Int.MAX_VALUE
                        var minIsStacking = false
                        for (index in columns.indices) {
                            val column = columns[index]
                            for (subIndex in column.indices.reversed()) {
                                val other = column[subIndex]
                                val otherData = eventData[other] ?: continue

                                // No space in current subgroup
                                if (otherData.start + stackOverlap > data.start && endOfNoSpacing(other) >= data.start)
                                    break

                                // Stacking
                                val (top, isStacking) = if (otherData.start + stackOverlap <= data.start
                                        && endOfNoSpacing(other) >= data.start)
                                    (otherData.start + stackOverlap).toInt() to true
                                // Below other
                                else if (otherData.end <= data.start)
                                    otherData.end to false
                                // Too close
                                else
                                    continue

                                // Wider and further at the top
                                if (minSubIndex >= subIndex) {
                                    minIndex = index
                                    minSubIndex = subIndex
                                    minTop = top
                                    minIsStacking = isStacking

                                    if (otherData.end >= data.start)
                                        break
                                }
                            }
                        }

                        // If no column fits
                        if (minTop == Int.MAX_VALUE) {
                            eventData[event]?.index = columns.size
                            columns.add(mutableListOf(event))
                            continue
                        }

                        val subIndex = if (minIsStacking) minSubIndex + 1 else minSubIndex
                        eventData[event]?.also {
                            it.index = minIndex
                            it.subIndex = subIndex
                        }

                        val column = columns[minIndex]
                        if (column.size > subIndex)
                            column[subIndex] = event
                        else
                            column.add(event)
                    }
                    for (e in currentGroup)
                        eventData[e]?.parallel = columns.size
                }
            }
        }

        var currentEnd = 0
        loop@ for (event in events) {
            val data = eventData[event] ?: continue
            when {
                event is AddEvent -> continue@loop
                data.start <= currentEnd -> {
                    currentGroup.add(event)
                    currentEnd = max(currentEnd, data.end)
                }
                else -> {
                    endGroup()
                    currentGroup = mutableListOf(event)
                    currentEnd = data.end
                }
            }
        }
        endGroup()
    }

    private fun requestPositionEventsAndLayout() {
        requestLayout()

        if (eventPositionJob == null)
            eventPositionJob = launch(UI) {
                eventPositionRequired = false
                delay(EVENT_POSITIONING_DEBOUNCE)
                positionEvents()
                requestLayout()
                eventPositionJob = null
                if (eventPositionRequired)
                    requestPositionEventsAndLayout()
            }
        else
            eventPositionRequired = true
    }

    @Suppress("ThrowsCount")
    private fun checkEvents(events: List<Event>) {
        if (events.any { showAsAllDay(it) })
            throw IllegalArgumentException("all-day events cannot be shown inside DayEventsView")
        if (events.any { it is AddEvent })
            throw IllegalArgumentException("add events currently cannot be set from the outside")
        if (events.any { it.end < day.start || it.start >= day.end })
            throw IllegalArgumentException("event starts must all be inside the set day")
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

    private fun onUpdateDay(day: Day) {
        context.withStyledAttributes(attrs, R.styleable.DayEventsView, defStyleAttr, defStyleRes) {
            background = if (day.isToday)
                getDrawable(R.styleable.DayEventsView_dateCurrentBackground)
            else
                null

            if (day.isToday && !this@DayEventsView::timePaint.isInitialized) {
                timeCircleRadius = getDimensionPixelSize(R.styleable.DayEventsView_timeCircleRadius, 0)
                timeLineSize = getDimensionPixelSize(R.styleable.DayEventsView_timeLineSize, 0)
                val timeColor = getColor(R.styleable.DayEventsView_timeColor, Color.BLACK)
                timePaint = Paint().apply {
                    color = timeColor
                }
            }
        }
    }

    private data class EventData(
        val start: Int,
        val end: Int,
        var parallel: Int = 1,
        var index: Int = 0,
        var subIndex: Int = 0
    )
}
