package com.jonaswanke.calendar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.support.annotation.AttrRes
import android.support.v4.content.ContextCompat
import android.text.TextPaint
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.jonaswanke.calendar.R.attr.timeColor
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.util.*
import kotlin.math.max
import kotlin.properties.Delegates

/**
 * TODO: document your custom view class.
 */
class DayView @JvmOverloads constructor(context: Context,
                                        private val attrs: AttributeSet? = null,
                                        @AttrRes private val defStyleAttr: Int = R.attr.dayViewStyle,
                                        _day: Day? = null)
    : ViewGroup(context, attrs, defStyleAttr) {

    var onEventClickListener: ((Event) -> Unit)?
            by Delegates.observable<((Event) -> Unit)?>(null) { _, _, new ->
                updateListeners(new, onEventLongClickListener)
            }
    var onEventLongClickListener: ((Event) -> Unit)?
            by Delegates.observable<((Event) -> Unit)?>(null) { _, _, new ->
                updateListeners(onEventClickListener, new)
            }

    var day: Day by Delegates.observable(_day ?: Day()) { _, old, new ->
        onUpdateDay(new)
        if (old == new)
            return@observable

        events = emptyList()
    }
    private var events: List<Event> = emptyList()

    private var dateSize: Int = 0
    private var datePaint: TextPaint? = null
    private var weekDaySize: Int = 0
    private var weekDayPaint: TextPaint? = null
    private lateinit var weekDayString: String
    private var timeCircleRadius: Int = 0
    private var timeLineSize: Int = 0
    private var timePaint: Paint? = null
    private val headerHeight: Int

    internal var divider by Delegates.observable<Drawable?>(null) { _, _, new ->
        dividerHeight = new?.intrinsicWidth ?: 0
    }
        private set
    private var dividerHeight: Int = 0

    private val cal: Calendar

    init {
        setWillNotDraw(false)

        onUpdateDay(day)
        headerHeight = context.resources.getDimensionPixelOffset(R.dimen.calendar_headerHeight)
        cal = day.start.asCalendar()
        launch(UI) {
            divider = ContextCompat.getDrawable(context, android.R.drawable.divider_horizontal_bright)
            weekDayString = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())
            invalidate()
        }
    }

    override fun addView(child: View?, index: Int, params: LayoutParams?) {
        if (child !is EventView)
            throw IllegalArgumentException("Only EventViews may be children of DayView")
        super.addView(child, index, params)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val left = paddingLeft + timeCircleRadius
        val top = paddingTop + headerHeight
        val right = r - l - paddingRight
        val bottom = b - t - paddingBottom
        val height = bottom - top

        fun getPosForTime(time: Long): Int {
            return (height * cal.apply { timeInMillis = time }.timeOfDay / DateUtils.DAY_IN_MILLIS).toInt()
        }

        for (viewIndex in 0 until childCount) {
            val view = getChildAt(viewIndex) as EventView
            val event = view.event ?: continue
            val startHeight = top + getPosForTime(event.start)
            val endHeight = max(top + getPosForTime(event.end), startHeight + view.minHeight)

            view.layout(left, startHeight, right, endHeight)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null)
            return

        val left = paddingLeft
        var top = paddingTop
        val right = canvas.width - paddingRight
        val bottom = canvas.height - paddingBottom

        cal.timeInMillis = day.start

        top += (dateSize * 1.4).toInt()
        canvas.drawText(cal.get(Calendar.DAY_OF_MONTH).toString(),
                .3f * dateSize, top.toFloat(), datePaint)
        top += (dateSize * .2).toInt()

        if (this::weekDayString.isInitialized) {
            top += weekDaySize
            canvas.drawText(weekDayString, .3f * dateSize, top.toFloat(), weekDayPaint)
        }

        divider?.setBounds(left, paddingTop + headerHeight - dividerHeight,
                right, paddingTop + headerHeight)
        divider?.draw(canvas)
        top = paddingTop + headerHeight

        if (day.isToday) {
            val time = Calendar.getInstance().timeOfDay
            val posY = top + (bottom.toFloat() - top) * time / DateUtils.DAY_IN_MILLIS
            canvas.drawCircle(left.toFloat(), posY, timeCircleRadius.toFloat(), timePaint)
            canvas.drawRect(left.toFloat(), posY - timeLineSize / 2,
                    right.toFloat(), posY + timeLineSize / 2, timePaint)
        }

        val hourHeight = (bottom.toFloat() - top) / 24
        for (hour in 1..23) {
            divider?.setBounds(left, (top + hourHeight * hour).toInt(),
                    right, (top + hourHeight * hour + dividerHeight).toInt())
            divider?.draw(canvas)
        }
    }

    private fun updateListeners(onEventClickListener: ((Event) -> Unit)?,
                                onEventLongClickListener: ((Event) -> Unit)?) {
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

        dateSize = a.getDimensionPixelSize(R.styleable.DayView_dateSize, 16)
        val dateColorAttr = when {
            day.isToday -> R.styleable.DayView_dateCurrentColor
            day.isFuture -> R.styleable.DayView_dateFutureColor
            else -> R.styleable.DayView_dateColor
        }
        datePaint = TextPaint().apply {
            color = a.getColor(dateColorAttr, Color.BLACK)
            isAntiAlias = true
            textSize = dateSize.toFloat()
        }
        background = if (day.isToday)
            a.getDrawable(R.styleable.DayView_dateCurrentBackground)
        else
            null

        weekDaySize = a.getDimensionPixelSize(R.styleable.DayView_weekDaySize, 16)
        val weekDayColorAttr = when {
            day.isToday -> R.styleable.DayView_weekDayCurrentColor
            day.isFuture -> R.styleable.DayView_weekDayFutureColor
            else -> R.styleable.DayView_weekDayColor
        }
        weekDayPaint = TextPaint().apply {
            color = a.getColor(weekDayColorAttr, Color.BLACK)
            isAntiAlias = true
            textSize = weekDaySize.toFloat()
        }

        timeCircleRadius = a.getDimensionPixelSize(R.styleable.DayView_timeCircleRadius, 16)
        timeLineSize = a.getDimensionPixelSize(R.styleable.DayView_timeLineSize, 16)
        val timeColor = a.getColor(R.styleable.DayView_timeColor, Color.BLACK)
        timePaint = Paint().apply {
            color = timeColor
        }

        a.recycle()

    }


    fun setEvents(events: List<Event>) {
        if (events.any { event -> event.start < day.start || event.start >= day.end })
            throw IllegalArgumentException("event starts must all be inside the set day")
        this.events = events

        async(UI) {
            removeAllViews()
            events.map { event ->
                async(UI) {
                    addView(EventView(this@DayView.context).also {
                        it.event = event
                    })
                }
            }.forEach { it.await() }
            updateListeners(onEventClickListener, onEventLongClickListener)
        }
    }
}
