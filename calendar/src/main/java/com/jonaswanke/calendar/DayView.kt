package com.jonaswanke.calendar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.support.annotation.AttrRes
import android.support.v4.content.ContextCompat
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
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

    var day: Day = _day ?: Day()
        private set
    private var events: List<Event> = emptyList()
    private val eventsParallel: MutableMap<Event, Pair<Int, Int>> = mutableMapOf()


    private var _hourHeight: Float
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
    private var timePaint: Paint? = null

    internal var divider by Delegates.observable<Drawable?>(null) { _, _, new ->
        dividerHeight = new?.intrinsicHeight ?: 0
    }
        private set
    private var dividerHeight: Int = 0

    private val cal: Calendar

    init {
        setWillNotDraw(false)

        val a = context.obtainStyledAttributes(
                attrs, R.styleable.DayView, defStyleAttr, R.style.Calendar_DayViewStyle)

        _hourHeight = a.getDimension(R.styleable.DayView_hourHeight, 16f)
        hourHeightMin = a.getDimension(R.styleable.DayView_hourHeightMin, 0f)
        hourHeightMax = a.getDimension(R.styleable.DayView_hourHeightMax, 0f)

        timeCircleRadius = a.getDimensionPixelSize(R.styleable.DayView_timeCircleRadius, 16)

        a.recycle()

        onUpdateDay(day)
        cal = day.start.asCalendar()
        launch(UI) {
            divider = ContextCompat.getDrawable(context, android.R.drawable.divider_horizontal_bright)
            invalidate()
        }

        setOnLongClickListener {
            positionEvents()
            true
        }
    }

    override fun addView(child: View?, index: Int, params: LayoutParams?) {
        if (child !is EventView)
            throw IllegalArgumentException("Only EventViews may be children of DayView")
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

        for (viewIndex in 0 until childCount) {
            val view = getChildAt(viewIndex) as EventView
            val event = view.event ?: continue
            val parallel = eventsParallel[event]!!

            val eventTop = top + getPosForTime(event.start)
            val eventBottom = max(top + getPosForTime(event.end), eventTop + view.minHeight)
            val eventWidth = width / parallel.first
            val eventLeft = left + eventWidth * parallel.second + space

            view.layout(eventLeft, eventTop, eventLeft + eventWidth - space, eventBottom)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null)
            return

        val left = paddingLeft
        val top = paddingTop
        val right = canvas.width - paddingRight
        val bottom = canvas.height - paddingBottom

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

        setEvents(events)
    }

    fun setEvents(events: List<Event>) {
        checkEvents(events)
        this.events = events.sortedBy { it.start }

        launch(UI) {
            @Suppress("NAME_SHADOWING")
            val events = this@DayView.events
            positionEvents()

            val existing = childCount
            for (i in 0 until events.size) {
                val event = events[i]

                if (existing > i)
                    (getChildAt(i) as EventView).event = event
                else
                    addView(EventView(this@DayView.context).also {
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
        eventsParallel.clear()
        val view = if (childCount > 0) (getChildAt(0) as EventView) else EventView(context)
        val minLength = (view.minHeight / hourHeight * DateUtils.HOUR_IN_MILLIS).toLong()

        fun endOf(event: Event) = Math.max(event.end, event.start + minLength)

        var currentGroup = mutableListOf<Event>()
        var currentEnd = 0L
        fun endGroup() {
            when (currentGroup.size) {
                0 -> return
                1 -> eventsParallel[currentGroup[0]] = 1 to 0
                else -> {
                    val ends = mutableListOf<Long>()
                    for (event in currentGroup) {
                        val min = ends.filter { it < event.start }.min()
                        val index = ends.indexOf(min)

                        if (index < 0) {
                            eventsParallel[event] = 1 to ends.size
                            ends.add(endOf(event))
                        } else {
                            eventsParallel[event] = 1 to index
                            ends[index] = endOf(event)
                        }
                    }
                    for (e in currentGroup)
                        eventsParallel[e] = eventsParallel[e]!!.copy(first = ends.size)
                }
            }
        }
        for (event in events)
            if (event.start <= currentEnd) {
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
        if (events.any { event -> event.start < day.start || event.start >= day.end })
            throw IllegalArgumentException("event starts must all be inside the set day")
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

    private fun onUpdateDay(day: Day) {
        val a = context.obtainStyledAttributes(
                attrs, R.styleable.DayView, defStyleAttr, R.style.Calendar_DayViewStyle)

        background = if (day.isToday)
            a.getDrawable(R.styleable.DayView_dateCurrentBackground)
        else
            null

        if (day.isToday && timePaint == null) {
            timeLineSize = a.getDimensionPixelSize(R.styleable.DayView_timeLineSize, 16)
            val timeColor = a.getColor(R.styleable.DayView_timeColor, Color.BLACK)
            timePaint = Paint().apply {
                color = timeColor
            }
        }

        a.recycle()
    }
}
