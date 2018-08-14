package com.jonaswanke.calendar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import android.text.format.DateUtils
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.withStyledAttributes
import androidx.core.view.children
import androidx.core.view.get
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import java.util.*
import kotlin.math.max
import kotlin.properties.Delegates

/**
 * TODO: document your custom view class.
 */
class DayView @JvmOverloads constructor(
    context: Context,
    private val attrs: AttributeSet? = null,
    @AttrRes private val defStyleAttr: Int = R.attr.dayViewStyle,
    _day: Day? = null
) : ViewGroup(context, attrs, defStyleAttr) {

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


    private var _hourHeight: Float = 0f
    var hourHeight: Float
        get() = _hourHeight
        set(value) {
            val v = value.coerceIn(if (hourHeightMin > 0) hourHeightMin else null,
                    if (hourHeightMax > 0) hourHeightMax else null)
            if (_hourHeight == v)
                return

            _hourHeight = v
            positionEvents()
            requestLayout()
        }
    var hourHeightMin: Float by Delegates.observable(0f) { _, _, new ->
        if (new > 0 && hourHeight < new)
            hourHeight = new
    }
    var hourHeightMax: Float by Delegates.observable(0f) { _, _, new ->
        if (new > 0 && hourHeight > new)
            hourHeight = new
    }

    private var timeCircleRadius: Int = 0
    private var timeLineSize: Int = 0
    private lateinit var timePaint: Paint

    internal var divider by Delegates.observable<Drawable?>(null) { _, _, new ->
        dividerHeight = new?.intrinsicHeight ?: 0
    }
        private set
    private var dividerHeight: Int = 0

    private val cal: Calendar

    init {
        setWillNotDraw(false)

        context.withStyledAttributes(attrs, R.styleable.DayView, defStyleAttr, R.style.Calendar_DayViewStyle) {
            _hourHeight = getDimension(R.styleable.DayView_hourHeight, 16f)
            hourHeightMin = getDimension(R.styleable.DayView_hourHeightMin, 0f)
            hourHeightMax = getDimension(R.styleable.DayView_hourHeightMax, 0f)

            timeCircleRadius = getDimensionPixelSize(R.styleable.DayView_timeCircleRadius, 16)
        }

        onUpdateDay(day)
        cal = day.start.asCalendar()
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
                eventData[event] = EventData()

                val view = addEventView
                if (view == null) {
                    addView(EventView(context,
                            defStyleAttr = R.attr.eventViewAddStyle,
                            defStyleRes = R.style.Calendar_EventViewStyle_Add,
                            _event = event).also {
                        it.setOnClickListener {
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

    override fun addView(child: View?, index: Int, params: LayoutParams?) {
        if (child !is EventView)
            throw IllegalArgumentException("Only EventViews may be children of DayView")
        if (child.event is AddEvent)
            if (addEventView != null && addEventView != child)
                throw  IllegalStateException("DayView may only contain one add-EventView")
            else {
                addEventView = child
                onAddEventViewListener?.invoke(child.event as AddEvent)
            }
        super.addView(child, index, params)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = paddingTop + paddingBottom + Math.max(suggestedMinimumHeight, (_hourHeight * 24).toInt())
        setMeasuredDimension(View.getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
                height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val space = timeCircleRadius / 2
        val left = paddingLeft + space
        val top = paddingTop
        val right = r - l - paddingRight
        val bottom = b - t - paddingBottom
        val height = bottom - top
        val width = right - left

        fun getPosForTime(time: Long): Int {
            return (height * cal.apply { timeInMillis = time }.timeOfDay / DateUtils.DAY_IN_MILLIS).toInt()
        }

        for (view in children) {
            val eventView = view as EventView
            val event = eventView.event ?: continue

            val data = eventData[event] ?: continue
            val eventTop = top + getPosForTime(event.start)
            var eventBottom = top + getPosForTime(event.end)
            // Fix if event ends on next day
            eventBottom = if (eventBottom < eventTop && event.end > event.start) bottom
            else max(eventBottom, eventTop + eventView.minHeight)
            val eventWidth = width / data.parallel
            val eventLeft = left + eventWidth * data.index + space

            eventView.layout(eventLeft, eventTop, eventLeft + eventWidth - space, eventBottom)
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

        if (day.isToday) {
            val time = Calendar.getInstance().timeOfDay
            val posY = top + (bottom.toFloat() - top) * time / DateUtils.DAY_IN_MILLIS
            canvas.drawCircle(left.toFloat(), posY, timeCircleRadius.toFloat(), timePaint)
            canvas.drawRect(left.toFloat(), posY - timeLineSize / 2,
                    right.toFloat(), posY + timeLineSize / 2, timePaint)
        }

        for (hour in 1..23) {
            divider?.setBounds(left, (top + _hourHeight * hour).toInt(),
                    right, (top + _hourHeight * hour + dividerHeight).toInt())
            divider?.draw(canvas)
        }
    }


    fun setDay(day: Day, events: List<Event> = emptyList()) {
        this.day = day
        onUpdateDay(day)

        removeAddEvent()
        setEvents(events)
    }

    fun setEvents(events: List<Event>) {
        checkEvents(events)
        this.events = events.sortedBy { it.start }

        launch(UI) {
            @Suppress("NAME_SHADOWING")
            val events = this@DayView.events
            positionEvents()

            if (addEventView != null)
                removeView(addEventView)

            val existing = childCount
            for (i in 0 until events.size) {
                val event = events[i]

                if (existing > i)
                    (this@DayView[i] as EventView).event = event
                else
                    addView(EventView(this@DayView.context).also {
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


    private fun positionEvents() {
        eventData.clear()
        val view = if (childCount > 0) (this[0] as EventView) else EventView(context)
        val minLength = (view.minHeight / hourHeight * DateUtils.HOUR_IN_MILLIS).toLong()

        fun endOf(event: Event) = Math.max(event.end, event.start + minLength)

        var currentGroup = mutableListOf<Event>()
        var currentEnd = 0L
        fun endGroup() {
            when (currentGroup.size) {
                0 -> return
                1 -> eventData[currentGroup[0]] = EventData()
                else -> {
                    val ends = mutableListOf<Long>()
                    for (event in currentGroup) {
                        val min = ends.filter { it < event.start }.min()
                        val index = ends.indexOf(min)

                        if (index < 0) {
                            eventData[event] = EventData(index = ends.size)
                            ends.add(endOf(event))
                        } else {
                            eventData[event] = EventData(index = index)
                            ends[index] = endOf(event)
                        }
                    }
                    for (e in currentGroup)
                        eventData[e]?.parallel = ends.size
                }
            }
        }
        for (event in events)
            if (event is AddEvent) {
                eventData[event] = EventData()
            } else if (event.start <= currentEnd) {
                currentGroup.add(event)
                currentEnd = Math.max(currentEnd, endOf(event))
            } else {
                endGroup()
                currentGroup = mutableListOf(event)
                currentEnd = endOf(event)
            }
        endGroup()
    }

    private fun checkEvents(events: List<Event>) {
        if (events.any { event -> event.allDay })
            throw IllegalArgumentException("all-day events cannot be shown inside DayView")
        if (events.any { event -> event is AddEvent })
            throw IllegalArgumentException("add events currently cannot be set from the outside")
        if (events.any { event -> event.start < day.start || event.start >= day.end })
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
        context.withStyledAttributes(attrs, R.styleable.DayView, defStyleAttr, R.style.Calendar_DayViewStyle) {
            background = if (day.isToday)
                getDrawable(R.styleable.DayView_dateCurrentBackground)
            else
                null

            if (day.isToday && !this@DayView::timePaint.isInitialized) {
                timeLineSize = getDimensionPixelSize(R.styleable.DayView_timeLineSize, 16)
                val timeColor = getColor(R.styleable.DayView_timeColor, Color.BLACK)
                timePaint = Paint().apply {
                    color = timeColor
                }
            }
        }
    }

    private data class EventData(
        var parallel: Int = 1,
        val index: Int = 0
    )
}
