package com.jonaswanke.calendar

import android.content.Context
import android.gesture.GestureOverlayView
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.util.SparseArray
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.IntDef
import androidx.core.content.withStyledAttributes
import androidx.core.view.doOnLayout
import androidx.viewpager.widget.ViewPager
import com.jonaswanke.calendar.pager.InfinitePagerAdapter
import com.jonaswanke.calendar.pager.InfiniteViewPager
import com.jonaswanke.calendar.utils.*
import kotlinx.android.synthetic.main.view_calendar.view.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlin.math.ceil
import kotlin.properties.Delegates

/**
 * TODO: document your custom view class.
 */
class CalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = R.attr.calendarViewStyle
) : GestureOverlayView(context, attrs, defStyleAttr) {

    companion object {
        private const val GESTURE_STROKE_LENGTH = 10f

        const val RANGE_DAY = 1
        const val RANGE_3_DAYS = 3
        const val RANGE_WEEK = 7
        val RANGE_VALUES = intArrayOf(RANGE_DAY, RANGE_3_DAYS, RANGE_WEEK)
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(RANGE_DAY, RANGE_3_DAYS, RANGE_WEEK)
    annotation class Range


    var onEventClickListener: ((Event) -> Unit)?
            by Delegates.observable<((Event) -> Unit)?>(null) { _, _, new ->
                updateListeners(new, onEventLongClickListener, onAddEventListener)
            }
    var onEventLongClickListener: ((Event) -> Unit)?
            by Delegates.observable<((Event) -> Unit)?>(null) { _, _, new ->
                updateListeners(onEventClickListener, new, onAddEventListener)
            }
    var onAddEventListener: ((AddEvent) -> Boolean)?
            by Delegates.observable<((AddEvent) -> Boolean)?>(null) { _, _, new ->
                updateListeners(onEventClickListener, onEventLongClickListener, new)
            }

    var eventRequestCallback: (Week) -> Unit = {}

    val cachedWeeks: Set<Week>
        get() {
            return views
                    .map { it.value }
                    .flatMap {
                        val start = it.range.start.weekObj
                        val end = it.range.endExclusive.weekObj
                        return@flatMap generateSequence(start) { week ->
                            if (week < end) week.nextWeek
                            else null
                        }.toSet()
                    }
                    .toSet()
        }
    val cachedEvents: Set<Week> get() = events.keys

    var startIndicator: RangeViewStartIndicator? = null

    var hourHeight: Float by Delegates.vetoable(0f) { _, old, new ->
        @Suppress("ComplexCondition")
        if ((hourHeightMin > 0 && new < hourHeightMin)
                || (hourHeightMax > 0 && new > hourHeightMax))
            return@vetoable false
        if (old == new)
            return@vetoable true

        hours.hourHeight = new
        views[visibleStart]?.hourHeight = new
        launch(UI) {
            for (view in views.values)
                if (view.range.start != visibleStart)
                    view.hourHeight = new
        }
        return@vetoable true
    }
    var hourHeightMin: Float by Delegates.observable(0f) { _, _, new ->
        if (new > 0 && hourHeight < new)
            hourHeight = new

        hours.hourHeightMin = new
        for (week in views.values)
            week.hourHeightMin = new
    }
    var hourHeightMax: Float by Delegates.observable(0f) { _, _, new ->
        if (new > 0 && hourHeight > new)
            hourHeight = new

        hours.hourHeightMax = new
        for (week in views.values)
            week.hourHeightMax = new
    }
    var scrollPosition: Int by Delegates.observable(0) { _, _, new ->
        hoursScroll.scrollY = new
        for (week in views.values)
            week.scrollTo(new)
    }

    private val events: MutableMap<Week, List<Event>> = mutableMapOf()
    private val views: MutableMap<Day, RangeView> = mutableMapOf()
    private val scaleDetector: ScaleGestureDetector

    @Range
    private var _range: Int = RANGE_WEEK
    @Range
    var range: Int
        get() = _range
        set(value) {
            if (_range == value)
                return
            if (value !in RANGE_VALUES)
                throw UnsupportedOperationException()

            _range = value
            onRangeUpdated()
        }

    private var _visibleStart: Day = Day()
    var visibleStart: Day
        get() = _visibleStart
        set(value) {
            val start = when (range) {
                RANGE_DAY -> value
                RANGE_3_DAYS -> TODO()
                RANGE_WEEK -> value.weekObj.firstDay
                else -> throw UnsupportedOperationException()
            }
            if (_visibleStart == start)
                return

            _visibleStart = start
            startIndicator?.start = start
            pager.setCurrentIndicator<Day, RangeView>(start)
        }
    var visibleEnd: Day
        get() = _visibleStart + range
        set(value) {
            visibleStart = value - range
        }

    private val pagerAdapter: InfinitePagerAdapter<Day, RangeView>

    init {
        View.inflate(context, R.layout.view_calendar, this)

        context.withStyledAttributes(attrs, R.styleable.CalendarView, defStyleAttr, R.style.Calendar_CalendarViewStyle) {
            _range = getInteger(R.styleable.CalendarView_range, RANGE_WEEK)
            hourHeightMin = getDimension(R.styleable.CalendarView_hourHeightMin, 0f)
            hourHeightMax = getDimension(R.styleable.CalendarView_hourHeightMax, 0f)
            hourHeight = getDimension(R.styleable.CalendarView_hourHeight, 0f)
        }

        isGestureVisible = false
        gestureStrokeLengthThreshold = GESTURE_STROKE_LENGTH

        scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector?): Boolean {
                if (detector == null)
                    return false

                val foc = (detector.focusY + scrollPosition) / hourHeight
                hourHeight *= detector.currentSpanY / detector.previousSpanY
                scrollPosition = (foc * hourHeight - detector.focusY).toInt()

                return true
            }
        })

        pagerAdapter = object : InfinitePagerAdapter<Day, RangeView>(visibleStart, 2) {
            override fun nextIndicator(current: Day) = current + range
            override fun previousIndicator(current: Day) = current - range

            override var currentIndicatorString: String
                get() = currentIndicator.toString()
                set(value) {
                    currentIndicator = value.toDay()
                }

            override fun instantiateItem(indicator: Day, oldView: RangeView?): RangeView {
                val oldKey = oldView?.range?.start
                if (views[oldKey] == oldView)
                    views.remove(oldKey)

                val week = indicator.weekObj
                val view = when (range) {
                    RANGE_DAY -> (oldView as? DayView) ?: DayView(context, day = indicator)
                    RANGE_3_DAYS -> TODO()
                    RANGE_WEEK -> (oldView as? WeekView) ?: WeekView(context, week = week)
                    else -> throw UnsupportedOperationException()
                }

                // Configure view
                view.onEventClickListener = onEventClickListener
                view.onEventLongClickListener = onEventLongClickListener
                view.onAddEventViewListener = { _ ->
                    for (otherView in views.values)
                        if (otherView != view)
                            otherView.removeAddEvent()
                }
                view.onAddEventListener = onAddEventListener
                view.onHeaderHeightChangeListener = { onHeaderHeightUpdated() }
                view.onScrollChangeListener = { scrollPosition = it }
                view.hourHeightMin = hourHeightMin
                view.hourHeightMax = hourHeightMax
                view.hourHeight = hourHeight
                doOnLayout { _ -> view.scrollTo(scrollPosition) }

                // Set/update events
                if (oldView == null)
                    view.events = getEventsForRange(view.range)
                else
                    view.setStart(indicator, getEventsForRange(indicator.range(view.length)))
                views[indicator] = view

                // TODO: async
                var currentWeek = week
                var weeksLeft = ceil(range.toFloat() / WEEK_IN_DAYS).toInt()
                while (weeksLeft > 0) {
                    eventRequestCallback(week)
                    currentWeek = currentWeek.nextWeek
                    weeksLeft--
                }
                return view
            }
        }

        hoursScroll.onScrollChangeListener = { scrollPosition = it }
        startIndicator?.start = pagerAdapter.currentIndicator

        pager.adapter = pagerAdapter
        pager.listener = object : InfiniteViewPager.OnInfinitePageChangeListener {
            override fun onPageScrolled(indicator: Any?, positionOffset: Float, positionOffsetPixels: Int) {
                Log.d("Pager", "$indicator, $positionOffset")
                onHeaderHeightUpdated()
            }

            override fun onPageSelected(indicator: Any?) {
            }

            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager.SCROLL_STATE_IDLE) {
                    _visibleStart = pagerAdapter.currentIndicator
                    startIndicator?.start = _visibleStart
                }
            }
        }

        onRangeUpdated()
    }


    // View
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        scaleDetector.onTouchEvent(event)
        return super.dispatchTouchEvent(event)
    }


    // Custom
    private fun getEventsForRange(range: DayRange): List<Event> {
        var currentWeek = events.keys.minBy { it.start } ?: return emptyList()
        val viewEvents = mutableListOf<Event>()

        while (currentWeek.start < range.endExclusive.start) {
            val newEvents = events[currentWeek]
                    ?.filter { it.start < range.endExclusive.start && it.end > range.start.start }
            if (newEvents != null)
                viewEvents.addAll(newEvents)
            currentWeek = currentWeek.nextWeek
        }
        return viewEvents
    }

    fun setEventsForWeek(week: Week, events: List<Event>) {
        if (events.any { it.end < week.start || it.start >= week.end })
            throw IllegalArgumentException("event starts must all be inside the set day")

        this.events[week] = events

        // Update future weeks
        var start = views.keys.lastOrNull { it <= week.firstDay }
                ?: views.keys.firstOrNull()
                ?: return
        while (views[start]?.events?.any { it.start in week } == true // Deprecated event has to be removed
                || events.any { it.end > start.start }) { // New event has to be added
            val view = views[start]
            if (view != null)
                view.events = getEventsForRange(view.range)
            start += range
        }
    }


    private fun onHeaderHeightUpdated() {
        val firstPosition = when (pager.position) {
            -1 -> views[visibleStart - range]
            1 -> views[visibleStart + range]
            else -> views[visibleStart]
        }?.headerHeight ?: 0
        val secondPosition = when (pager.position) {
            0 -> views[visibleStart + range]
            else -> views[visibleStart]
        }?.headerHeight ?: 0
        startIndicator?.minimumHeight = (firstPosition * (1 - pager.positionOffset)
                + secondPosition * pager.positionOffset).toInt()
    }

    private fun onRangeUpdated() {
        // Forces aligning to new length
        visibleStart = visibleStart

        startIndicator = when (range) {
            RANGE_DAY -> RangeHeaderView(context, _range = visibleStart.range(1))
            RANGE_3_DAYS -> TODO()
            RANGE_WEEK -> WeekIndicatorView(context, _start = visibleStart)
            else -> throw UnsupportedOperationException()
        }
        hoursCol.removeViewAt(0)
        hoursCol.addView(startIndicator, 0, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        pagerAdapter.reset(visibleStart)
    }

    private fun updateListeners(
        onEventClickListener: ((Event) -> Unit)?,
        onEventLongClickListener: ((Event) -> Unit)?,
        onAddEventListener: ((AddEvent) -> Boolean)?
    ) {
        for (view in views.values) {
            view.onEventClickListener = onEventClickListener
            view.onEventLongClickListener = onEventLongClickListener
            view.onAddEventListener = onAddEventListener
        }
    }


    // State
    override fun dispatchSaveInstanceState(container: SparseArray<Parcelable>?) {
        dispatchFreezeSelfOnly(container)
    }

    override fun onSaveInstanceState(): Parcelable {
        return SavedState(super.onSaveInstanceState()).also {
            it.visibleStart = visibleStart
            it.range = range
            it.hourHeight = hourHeight
            it.hourHeightMin = hourHeightMin
            it.hourHeightMax = hourHeightMax
            it.scrollPosition = scrollPosition
        }
    }

    override fun dispatchRestoreInstanceState(container: SparseArray<Parcelable>?) {
        dispatchThawSelfOnly(container)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        // firstDayOfWeek not updated on restore automatically
        state.visibleStart?.also { visibleStart = Day(it.year, it.week, it.day) }
        state.range?.also { range = it }
        state.hourHeightMin?.also { hourHeightMin = it }
        state.hourHeightMax?.also { hourHeightMax = it }
        state.hourHeight?.also { hourHeight = it }
        state.scrollPosition?.also { scrollPosition = it }
    }

    internal class SavedState : View.BaseSavedState {
        companion object {
            @Suppress("unused")
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel?) = SavedState(source)
                override fun newArray(size: Int) = arrayOfNulls<SavedState>(size)
            }
        }

        var visibleStart: Day? = null
        @Range
        var range: Int? = null
        var hourHeight: Float? = null
        var hourHeightMin: Float? = null
        var hourHeightMax: Float? = null
        var scrollPosition: Int? = null

        constructor(source: Parcel?) : super(source) {
            if (source == null)
                return

            fun readInt(): Int? {
                val value = source.readInt()
                return if (value == Int.MIN_VALUE) null else value
            }

            fun readFloat(): Float? {
                val value = source.readFloat()
                return if (value == Float.NaN) null else value
            }

            visibleStart = source.readString()?.toDay()
            range = readInt()
            hourHeight = readFloat()
            hourHeightMin = readFloat()
            hourHeightMax = readFloat()
            scrollPosition = readInt()
        }

        constructor(superState: Parcelable?) : super(superState)

        override fun writeToParcel(out: Parcel?, flags: Int) {
            super.writeToParcel(out, flags)
            out?.writeString(visibleStart?.toString())
            out?.writeInt(range ?: Int.MIN_VALUE)
            out?.writeFloat(hourHeight ?: Float.NaN)
            out?.writeFloat(hourHeightMin ?: Float.NaN)
            out?.writeFloat(hourHeightMax ?: Float.NaN)
            out?.writeInt(scrollPosition ?: Int.MIN_VALUE)
        }
    }
}
