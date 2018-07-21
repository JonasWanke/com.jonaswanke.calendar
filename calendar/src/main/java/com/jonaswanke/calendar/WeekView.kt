package com.jonaswanke.calendar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.support.annotation.AttrRes
import android.support.v4.content.ContextCompat
import android.text.TextPaint
import android.text.format.DateUtils
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import java.util.*
import kotlin.properties.Delegates

/**
 * TODO: document your custom view class.
 */
class WeekView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, @AttrRes defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {

    var onEventClickListener: ((String) -> Unit)? = null
    var onEventLongClickListener: ((String) -> Unit)? = null

    var week: Week by Delegates.observable(Week()) { _, old, new ->
        if (old == new)
            return@observable

        events = emptyList()
    }
    val start: Long
        get() = week.toCalendar().timeInMillis
    val end: Long
        get() = week.toCalendar().timeInMillis + DateUtils.WEEK_IN_MILLIS
    var events: List<Event> by Delegates.observable(emptyList()) { _, old, new ->
        if (old == new)
            return@observable
        if (new.any { event -> event.start < start || event.start >= end })
            throw IllegalArgumentException("event starts must all be inside the set week")

        onEventsChanged(new)
    }

    private fun onEventsChanged(events: List<Event>?) {

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

        canvas.drawRect(100f, 100f, 200f, 200f, Paint().apply { color = Color.GREEN })
        canvas.drawText(week.week.toString(), 100.0f, 100.0f, TextPaint().apply {
            color = Color.BLUE
            textSize = 100f
        })
    }
}
