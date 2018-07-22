package com.jonaswanke.calendar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.support.annotation.AttrRes
import android.support.v4.content.ContextCompat
import android.text.TextPaint
import android.text.format.DateUtils
import android.util.AttributeSet
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

    private var divider by Delegates.observable<Drawable?>(null) { _, _, new ->
        dividerHeight = new?.intrinsicWidth ?: 0
    }
    private var dividerHeight: Int = 0

    init {
        setWillNotDraw(false)
        divider = ContextCompat.getDrawable(context, android.R.drawable.divider_horizontal_bright)
    }

    override fun addView(child: View?, index: Int, params: LayoutParams?) {
        if (child !is EventView)
            throw IllegalArgumentException("Only EventViews may be children of DayView")
        super.addView(child, index, params)
    }

    private val cal = Calendar.getInstance()
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val left = paddingLeft
        val top = paddingTop
        val right = r - l - paddingRight
        val bottom = b - t - paddingBottom
        val height = bottom - top

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
        if (canvas == null)
            return

        val left = paddingLeft
        val top = paddingTop
        val right = canvas.width - paddingRight
        val bottom = canvas.height - paddingBottom
        val height = bottom - top

        val hourHeight = height / 23
        for (hour in 0..24) {
            divider?.setBounds(left, top + hourHeight * hour,
                    right, top + hourHeight * hour + dividerHeight)
            divider?.draw(canvas)
        }
        canvas.drawText(day.day.toString(), 100.0f, 200.0f, TextPaint().apply {
            color = Color.BLUE
            textSize = 100f
        })
    }
}
