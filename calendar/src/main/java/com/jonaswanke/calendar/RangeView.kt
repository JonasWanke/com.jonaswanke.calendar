package com.jonaswanke.calendar

import android.content.Context
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import com.jonaswanke.calendar.utils.Day
import com.jonaswanke.calendar.utils.DayRange
import java.util.*
import kotlin.properties.Delegates

abstract class RangeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    val length: Int,
    _start: Day? = null
) : LinearLayout(context, attrs, defStyleAttr) {
    companion object {
        internal fun showAsAllDay(event: Event) = event.allDay || event.end - event.start >= DateUtils.DAY_IN_MILLIS
    }

    // Listeners
    var onEventClickListener: ((Event) -> Unit)?
            by Delegates.observable<((Event) -> Unit)?>(null) { _, _, new ->
                updateListeners(new, onEventLongClickListener, onAddEventListener)
            }
    var onEventLongClickListener: ((Event) -> Unit)?
            by Delegates.observable<((Event) -> Unit)?>(null) { _, _, new ->
                updateListeners(onEventClickListener, new, onAddEventListener)
            }
    var onAddEventViewListener: ((AddEvent) -> Unit)? = null
    var onAddEventListener: ((AddEvent) -> Boolean)?
            by Delegates.observable<((AddEvent) -> Boolean)?>(null) { _, _, new ->
                updateListeners(onEventClickListener, onEventLongClickListener, new)
            }

    protected abstract fun updateListeners(
        onEventClickListener: ((Event) -> Unit)?,
        onEventLongClickListener: ((Event) -> Unit)?,
        onAddEventListener: ((AddEvent) -> Boolean)?
    )

    var onHeaderHeightChangeListener: ((Int) -> Unit)? = null
    abstract var onScrollChangeListener: ((Int) -> Unit)?


    // Range
    var range: DayRange = (_start ?: Day()).range(length)

    fun setStart(start: Day, events: List<Event> = emptyList()) {
        range = start.range(length)
        cal = start.toCalendar()

        removeAddEvent()
        checkEvents(events)

        _events = events
        onRangeUpdated(range, events)
    }

    protected open fun onRangeUpdated(range: DayRange, events: List<Event>) {
        onEventsChanged(events)
    }


    // Events
    private var _events: List<Event> = emptyList()
    var events: List<Event>
        get() = _events
        set(value) {
            checkEvents(value)
            _events = sortEvents(value)
            onEventsChanged(_events)
        }

    protected open fun sortEvents(events: List<Event>): List<Event> = events.sorted()
    protected abstract fun checkEvents(events: List<Event>)
    protected abstract fun onEventsChanged(events: List<Event>)


    // Header height
    var headerHeight: Int by Delegates.observable(0) { _, old, new ->
        if (old == new)
            return@observable

        onHeaderHeightChangeListener?.invoke(new)
    }
        protected set


    // Hour height
    private var _hourHeight: Float = 0f
    var hourHeight: Float
        get() = _hourHeight
        set(value) {
            val v = value.coerceIn(if (hourHeightMin > 0) hourHeightMin else null,
                    if (hourHeightMax > 0) hourHeightMax else null)
            if (_hourHeight == v)
                return

            _hourHeight = v
            onHourHeightChanged(height = _hourHeight)
        }
    var hourHeightMin: Float by Delegates.observable(0f) { _, _, new ->
        if (new > 0 && hourHeight < new)
            hourHeight = new
        onHourHeightChanged(heightMin = new)
    }
    var hourHeightMax: Float by Delegates.observable(Float.MAX_VALUE) { _, _, new ->
        if (new > 0 && hourHeight > new)
            hourHeight = new
        onHourHeightChanged(heightMax = new)
    }

    protected abstract fun onHourHeightChanged(
        height: Float? = null,
        heightMin: Float? = null,
        heightMax: Float? = null
    )


    // Other
    protected var cal: Calendar = range.start.toCalendar()
        private set

    internal abstract fun scrollTo(pos: Int)

    internal abstract fun removeAddEvent()


    fun onInitialized() {
        updateListeners(onEventClickListener, onEventLongClickListener, onAddEventListener)
    }
}

abstract class RangeViewStartIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    abstract var start: Day
}
