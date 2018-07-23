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
                                        attrs: AttributeSet? = null,
                                        @AttrRes defStyleAttr: Int = R.attr.dayViewStyle,
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
        background = if (new.isToday) dateCurrentBackground else null
        if (old == new)
            return@observable

        events = emptyList()
    }
    private var events: List<Event> = emptyList()

    private val dateSize: Int
    private val dateColor: Int
    private val datePaint: TextPaint
    private val dateCurrentColor: Int
    private val dateCurrentPaint: TextPaint
    private val dateCurrentBackground: Drawable?
    private val dateFutureColor: Int
    private val dateFuturePaint: TextPaint
    private val weekDaySize: Int
    private val weekDayColor: Int
    private val weekDayPaint: TextPaint
    private val weekDayCurrentColor: Int
    private val weekDayCurrentPaint: TextPaint
    private val weekDayFutureColor: Int
    private val weekDayFuturePaint: TextPaint
    private lateinit var weekDayString: String
    private val timeCircleRadius: Int
    private val timeLineSize: Int
    private val timeColor: Int
    private val timePaint: Paint
    private val headerHeight: Int

    internal var divider by Delegates.observable<Drawable?>(null) { _, _, new ->
        dividerHeight = new?.intrinsicWidth ?: 0
    }
        private set
    private var dividerHeight: Int = 0

    private val cal: Calendar

    init {
        divider = ContextCompat.getDrawable(context, android.R.drawable.divider_horizontal_bright)

        val a = context.obtainStyledAttributes(
                attrs, R.styleable.DayView, defStyleAttr, R.style.Calendar_DayViewStyle)

        dateSize = a.getDimensionPixelSize(R.styleable.DayView_dateSize, 16)
        dateColor = a.getColor(R.styleable.DayView_dateColor, Color.BLACK)
        datePaint = TextPaint().apply {
            color = dateColor
            isAntiAlias = true
            textSize = dateSize.toFloat()
        }
        dateCurrentColor = a.getColor(R.styleable.DayView_dateCurrentColor, dateColor)
        dateCurrentPaint = TextPaint().apply {
            color = dateCurrentColor
            isAntiAlias = true
            textSize = dateSize.toFloat()
        }
        dateCurrentBackground = a.getDrawable(R.styleable.DayView_dateCurrentBackground)
        dateFutureColor = a.getColor(R.styleable.DayView_dateFutureColor, dateColor)
        dateFuturePaint = TextPaint().apply {
            color = dateFutureColor
            isAntiAlias = true
            textSize = dateSize.toFloat()
        }

        weekDaySize = a.getDimensionPixelSize(R.styleable.DayView_weekDaySize, 16)
        weekDayColor = a.getColor(R.styleable.DayView_weekDayColor, Color.BLACK)
        weekDayPaint = TextPaint().apply {
            color = weekDayColor
            isAntiAlias = true
            textSize = weekDaySize.toFloat()
        }
        weekDayCurrentColor = a.getColor(R.styleable.DayView_weekDayCurrentColor, weekDayColor)
        weekDayCurrentPaint = TextPaint().apply {
            color = weekDayCurrentColor
            isAntiAlias = true
            textSize = weekDaySize.toFloat()
        }
        weekDayFutureColor = a.getColor(R.styleable.DayView_weekDayFutureColor, weekDayColor)
        weekDayFuturePaint = TextPaint().apply {
            color = weekDayFutureColor
            isAntiAlias = true
            textSize = weekDaySize.toFloat()
        }

        timeCircleRadius = a.getDimensionPixelSize(R.styleable.DayView_timeCircleRadius, 16)
        timeLineSize = a.getDimensionPixelSize(R.styleable.DayView_timeLineSize, 16)
        timeColor = a.getColor(R.styleable.DayView_timeColor, Color.BLACK)
        timePaint = Paint().apply {
            color = timeColor
        }

        a.recycle()

        headerHeight = context.resources.getDimensionPixelOffset(R.dimen.calendar_headerHeight)
        cal = day.start.asCalendar()
        launch(UI) {
            weekDayString = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())
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
        val datePaintCurrent = when {
            day.isToday -> dateCurrentPaint
            day.isFuture -> dateFuturePaint
            else -> datePaint
        }
        canvas.drawText(cal.get(Calendar.DAY_OF_MONTH).toString(),
                .3f * dateSize, top.toFloat(), datePaintCurrent)
        top += (dateSize * .2).toInt()

        top += weekDaySize
        val weekDayPaintCurrent = when {
            day.isToday -> weekDayCurrentPaint
            day.isFuture -> weekDayFuturePaint
            else -> weekDayPaint
        }
        canvas.drawText(weekDayString, .3f * dateSize, top.toFloat(), weekDayPaintCurrent)

        divider?.setBounds(left, paddingTop + headerHeight - dividerHeight,
                right, paddingTop + headerHeight)
        divider?.draw(canvas)
        top = paddingTop + headerHeight

        if (day.isToday) {
            val time = Calendar.getInstance().timeOfDay
            val posY = (bottom.toFloat() - top) * time / DateUtils.DAY_IN_MILLIS
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


    fun setEvents(events: List<Event>) {
        if (events.any { event -> event.start < day.start || event.start >= day.end })
            throw IllegalArgumentException("event starts must all be inside the set day")
        this.events = events

        async(UI) {
            removeAllViews()
            for (event in events)
                addView(EventView(this@DayView.context).also {
                    it.event = event
                })
            updateListeners(onEventClickListener, onEventLongClickListener)
        }
    }
}
