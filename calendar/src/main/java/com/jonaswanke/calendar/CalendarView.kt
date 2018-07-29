package com.jonaswanke.calendar

import android.content.Context
import android.gesture.GestureOverlayView
import android.support.annotation.AttrRes
import android.support.annotation.IntDef
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlinx.android.synthetic.main.view_calendar.view.*
import kotlin.properties.Delegates

/**
 * TODO: document your custom view class.
 */
class CalendarView @JvmOverloads constructor(context: Context,
                                             attrs: AttributeSet? = null,
                                             @AttrRes defStyleAttr: Int = R.attr.calendarViewStyle)
    : GestureOverlayView(context, attrs, defStyleAttr) {

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


    @get: Range
    var range: Int by Delegates.observable(RANGE_WEEK) { _, old, new ->
        if (old == new)
            return@observable
        if (new !in RANGE_VALUES)
            throw UnsupportedOperationException()

        onRangeUpdated()
    }
    var hourHeight: Float by Delegates.observable(0f) { _, old, new ->
        if (old == new)
            return@observable

        hours.hourHeight = new
        for (week in weekViews.values)
            week.hourHeight = new
    }

    private val events: MutableMap<Week, List<Event>> = mutableMapOf()
    private val weekViews: MutableMap<Week, WeekView> = mutableMapOf()
    private val scaleDetector: ScaleGestureDetector

    private var currentWeek: Week = Week()

    private val pagerAdapter: InfinitePagerAdapter<Week, WeekView>

    init {
        View.inflate(context, R.layout.view_calendar, this)

        val a = context.obtainStyledAttributes(
                attrs, R.styleable.CalendarView, defStyleAttr, R.style.Calendar_CalendarViewStyle)

        range = a.getInteger(R.styleable.CalendarView_range, RANGE_WEEK)
        hourHeight = a.getDimension(R.styleable.CalendarView_hourHeight, 100f)

        a.recycle()

        scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector?): Boolean {
                hourHeight *= detector?.scaleFactor ?: 1f
                return true
            }
        })

        pagerAdapter = object : InfinitePagerAdapter<Week, WeekView>(currentWeek, 2) {
            override fun nextIndicator(current: Week) = current.nextWeek
            override fun previousIndicator(current: Week) = current.prevWeek

            override var currentIndicatorString: String
                get() = "${currentIndicator.year}-${currentIndicator.week}"
                set(value) {
                    val parts = value.split("-")
                    currentIndicator = Week(parts[0].toInt(), parts[1].toInt())
                }

            override fun instantiateItem(indicator: Week, oldView: WeekView?): WeekView {
                val view = if (oldView == null)
                    WeekView(context, _week = indicator).also {
                        it.events = events[indicator] ?: emptyList()
                        it.onEventClickListener = onEventClickListener
                        it.onEventLongClickListener = onEventLongClickListener
                        it.onScrollChangeListener = this@CalendarView::updateScrollPosition
                    }
                else {
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

        hoursScroll.onScrollChangeListener = this::updateScrollPosition
        hoursHeader.week = pagerAdapter.currentIndicator

        pager.adapter = pagerAdapter
        pager.listener = object : InfiniteViewPager.OnInfinitePageChangeListener {
            override fun onPageScrolled(indicator: Any?, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(indicator: Any?) {
            }

            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager.SCROLL_STATE_IDLE)
                    hoursHeader.week = pagerAdapter.currentIndicator
            }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        scaleDetector.onTouchEvent(event)
        return super.dispatchTouchEvent(event)
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

    private fun updateListeners(onEventClickListener: ((Event) -> Unit)?,
                                onEventLongClickListener: ((Event) -> Unit)?) {
        for (view in weekViews.values) {
            view.onEventClickListener = onEventClickListener
            view.onEventLongClickListener = onEventLongClickListener
        }
    }

    private fun updateScrollPosition(pos: Int) {
        hoursScroll.scrollY = pos
        for (week in weekViews.values)
            week.scrollTo(pos)
    }


    fun setEventsForWeek(week: Week, events: List<Event>) {
        this.events[week] = events
        weekViews[week]?.events = events
    }

    fun jumpToToday() {
        pager.setCurrentIndicator<Week, WeekView>(Week())
    }
}
