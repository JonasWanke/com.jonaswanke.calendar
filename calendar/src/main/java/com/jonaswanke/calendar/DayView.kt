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
import android.view.View
import android.view.ViewGroup
import java.util.*
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

        removeAllViews()
        for (event in new)
            addView(EventView(context).apply {
                this.event = event
            })
        invalidate()
    }

    init {
        setWillNotDraw(false)
    }

    override fun addView(child: View?, index: Int, params: LayoutParams?) {
        if (child !is EventView)
            throw IllegalArgumentException("Only EventViews may be children of DayView")
        super.addView(child, index, params)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val left = paddingLeft
        val top = paddingTop
        val right = r - l - paddingRight
        val bottom =  b - t - paddingBottom
        val height = bottom - top

        val cal = Calendar.getInstance()

        fun getPosForTime(time: Long): Int {
            return (height * cal.apply { timeInMillis = time }.timeOfDay / DateUtils.DAY_IN_MILLIS).toInt()
        }

        for (viewIndex in 0 until childCount) {
            val view = getChildAt(viewIndex) as EventView
            val event = view.event ?: continue

            view.layout(left,
                    top + getPosForTime(event.start),
                    right,
                    bottom + getPosForTime(event.end))
        }
    }

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
