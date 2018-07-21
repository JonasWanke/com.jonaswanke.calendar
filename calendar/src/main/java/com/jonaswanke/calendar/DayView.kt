package com.jonaswanke.calendar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.support.annotation.AttrRes
import android.text.TextPaint
import android.text.format.DateUtils
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup
import kotlin.properties.Delegates

/**
 * TODO: document your custom view class.
 */
class DayView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, @AttrRes defStyleAttr: Int = 0)
    : ViewGroup(context, attrs, defStyleAttr) {

    var onEventClickListener: ((String) -> Unit)? = null
    var onEventLongClickListener: ((String) -> Unit)? = null

    var day: Day by Delegates.observable(Day()) { _, old, new ->
        if (old == new)
            return@observable

        events = emptyList()
    }
    val start: Long
        get() = day.toCalendar().timeInMillis
    val end: Long
        get() = day.toCalendar().timeInMillis + DateUtils.DAY_IN_MILLIS
    var events: List<Event> by Delegates.observable(emptyList()) { _, old, new ->
        if (old == new)
            return@observable
        if (new.any { event -> event.start < start || event.start >= end })
            throw IllegalArgumentException("event starts must all be inside the set day")

        onEventsChanged(new)
    }

    init {
        setWillNotDraw(false)
    }

    private fun onEventsChanged(events: List<Event>?) {

    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {

    }

    private val _paddingLeft = paddingLeft
    private val _paddingTop = paddingTop
    private val _paddingRight = paddingRight
    private val _paddingBottom = paddingBottom

    private val _contentWidth = width - paddingLeft - paddingRight
    private val _contentHeight = height - paddingTop - paddingBottom

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        Log.d("CalendarView", "onDraw")
        if (canvas == null)
            return

        canvas.drawRect(100f, 200f, 200f, 300f, Paint().apply { color = Color.GREEN })
        canvas.drawText(day.day.toString(), 100.0f, 200.0f, TextPaint().apply {
            color = Color.BLUE
            textSize = 100f
        })
    }
}
