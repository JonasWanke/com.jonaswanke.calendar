package com.jonaswanke.calendar

import android.content.Context
import android.gesture.GestureOverlayView
import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.AttrRes
import android.support.annotation.IntDef
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.util.Log
import android.util.SparseArray
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlinx.android.synthetic.main.view_calendar.view.*
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
                updateListeners(new, onEventLongClickListener)
            }
    var onEventLongClickListener: ((Event) -> Unit)?
            by Delegates.observable<((Event) -> Unit)?>(null) { _, _, new ->
                updateListeners(onEventClickListener, new)
            }

    var eventRequestCallback: (Week) -> Unit = {}

    val cachedWeeks: Set<Week> get() = weekViews.keys


    @get: Range
    var range: Int by Delegates.observable(RANGE_WEEK) { _, old, new ->
        if (old == new)
            return@observable
        if (new !in RANGE_VALUES)
            throw UnsupportedOperationException()

        onRangeUpdated()
    }
    var hourHeight: Float by Delegates.vetoable(0f) { _, old, new ->
        if ((hourHeightMin > 0 && new < hourHeightMin)
                || (hourHeightMax > 0 && new > hourHeightMax))
            return@vetoable false
        if (old == new)
            return@vetoable true

        hours.hourHeight = new
        for (week in weekViews.values)
            week.hourHeight = new
        return@vetoable true
    }
    var hourHeightMin: Float by Delegates.observable(0f) { _, _, new ->
        if (new > 0 && hourHeight < new)
            hourHeight = new

        hours.hourHeightMin = new
        for (week in weekViews.values)
            week.hourHeightMin = new
    }
    var hourHeightMax: Float by Delegates.observable(0f) { _, _, new ->
        if (new > 0 && hourHeight > new)
            hourHeight = new

        hours.hourHeightMax = new
        for (week in weekViews.values)
            week.hourHeightMax = new
    }
    var scrollPosition: Int by Delegates.observable(0) { _, _, new ->
        hoursScroll.scrollY = new
        for (week in weekViews.values)
            week.scrollTo(new)
    }

    private val events: MutableMap<Week, List<Event>> = mutableMapOf()
    private val weekViews: MutableMap<Week, WeekView> = mutableMapOf()
    private val scaleDetector: ScaleGestureDetector

    private var _currentWeek: Week = Week()
    var currentWeek: Week
        get() = _currentWeek
        set(value) {
            if (_currentWeek == value)
                return

            _currentWeek = value
            hoursHeader.week = value
            pager.setCurrentIndicator<Week, WeekView>(value)
        }

    private val pagerAdapter: InfinitePagerAdapter<Week, WeekView>

    init {
        View.inflate(context, R.layout.view_calendar, this)

        val a = context.obtainStyledAttributes(
                attrs, R.styleable.CalendarView, defStyleAttr, R.style.Calendar_CalendarViewStyle)

        range = a.getInteger(R.styleable.CalendarView_range, RANGE_WEEK)
        hourHeightMin = a.getDimension(R.styleable.CalendarView_hourHeightMin, 0f)
        hourHeightMax = a.getDimension(R.styleable.CalendarView_hourHeightMax, 0f)
        hourHeight = a.getDimension(R.styleable.CalendarView_hourHeight, 100f)

        a.recycle()

        isGestureVisible = false
        gestureStrokeLengthThreshold = 10f

        scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector?): Boolean {
                if (detector == null)
                    return false

                val foc = (detector.focusY + scrollPosition) / hourHeight
                hourHeight *= detector.currentSpanY / detector.previousSpanY
                scrollPosition = (foc * hourHeight - detector.focusY).toInt() // (beginFocus * hourHeight - detector.focusY).toInt()

                return true
            }
        })

        pagerAdapter = object : InfinitePagerAdapter<Week, WeekView>(currentWeek, 2) {
            override fun nextIndicator(current: Week) = current.nextWeek
            override fun previousIndicator(current: Week) = current.prevWeek

            override var currentIndicatorString: String
                get() = currentIndicator.toString()
                set(value) {
                    currentIndicator = value.toWeek()!!
                }

            override fun instantiateItem(indicator: Week, oldView: WeekView?): WeekView {
                val view = if (oldView == null)
                    WeekView(context, _week = indicator).also {
                        it.events = events[indicator] ?: emptyList()
                        it.onEventClickListener = onEventClickListener
                        it.onEventLongClickListener = onEventLongClickListener
                        it.onHeaderHeightChangeListener = { onHeaderHeightUpdated() }
                        it.onScrollChangeListener = { scrollPosition = it }
                        it.hourHeightMin = hourHeightMin
                        it.hourHeightMax = hourHeightMax
                        it.hourHeight = hourHeight
                        doOnLayout { _ -> it.scrollTo(scrollPosition) }
                    }
                else {
                    if (weekViews[oldView.week] == oldView)
                        weekViews.remove(oldView.week)
                    oldView.also {
                        it.setWeek(indicator, events[indicator] ?: emptyList())
                    }
                }
                weekViews[indicator] = view
                eventRequestCallback(indicator)
                return view
            }
        }

        hoursScroll.onScrollChangeListener = { scrollPosition = it }
        hoursHeader.week = pagerAdapter.currentIndicator

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
                    _currentWeek = pagerAdapter.currentIndicator
                    hoursHeader.week = _currentWeek
                }
            }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        scaleDetector.onTouchEvent(event)
        return super.dispatchTouchEvent(event)
    }


    fun setEventsForWeek(week: Week, events: List<Event>) {
        this.events[week] = events
        weekViews[week]?.events = events
    }


    private fun onHeaderHeightUpdated() {
        val firstPosition = when (pager.position) {
            -1 -> weekViews[currentWeek.prevWeek]
            1 -> weekViews[currentWeek.nextWeek]
            else -> weekViews[currentWeek]
        }?.headerHeight ?: 0
        val secondPosition = when (pager.position) {
            0 -> weekViews[currentWeek.nextWeek]
            else -> weekViews[currentWeek]
        }?.headerHeight ?: 0
        hoursHeader.minimumHeight = (firstPosition * (1 - pager.positionOffset)
                + secondPosition * pager.positionOffset).toInt()
    }

    private fun onRangeUpdated() {
        when (range) {
            RANGE_DAY -> TODO()
            RANGE_3_DAYS -> TODO()
            RANGE_WEEK -> {

            }
            else -> throw UnsupportedOperationException()
        }
    }

    private fun updateListeners(
        onEventClickListener: ((Event) -> Unit)?,
        onEventLongClickListener: ((Event) -> Unit)?
    ) {
        for (view in weekViews.values) {
            view.onEventClickListener = onEventClickListener
            view.onEventLongClickListener = onEventLongClickListener
        }
    }


    override fun dispatchSaveInstanceState(container: SparseArray<Parcelable>?) {
        dispatchFreezeSelfOnly(container)
    }

    override fun onSaveInstanceState(): Parcelable {
        return SavedState(super.onSaveInstanceState()).also {
            it.week = currentWeek
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
        state.week?.also { currentWeek = Week(it.year, it.week) }
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

        var week: Week? = null
        @get: Range
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

            week = source.readString()?.toWeek()
            range = readInt()
            hourHeight = readFloat()
            hourHeightMin = readFloat()
            hourHeightMax = readFloat()
            scrollPosition = readInt()
        }

        constructor(superState: Parcelable) : super(superState)

        override fun writeToParcel(out: Parcel?, flags: Int) {
            super.writeToParcel(out, flags)
            out?.writeString(week?.toString())
            out?.writeInt(range ?: Int.MIN_VALUE)
            out?.writeFloat(hourHeight ?: Float.NaN)
            out?.writeFloat(hourHeightMin ?: Float.NaN)
            out?.writeFloat(hourHeightMax ?: Float.NaN)
            out?.writeInt(scrollPosition ?: Int.MIN_VALUE)
        }
    }
}
