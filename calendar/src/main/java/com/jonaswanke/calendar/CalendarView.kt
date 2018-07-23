package com.jonaswanke.calendar

import android.content.Context
import android.support.annotation.AttrRes
import android.support.annotation.IntDef
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_calendar.view.*
import java.util.*
import kotlin.properties.Delegates

/**
 * TODO: document your custom view class.
 */
class CalendarView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, @AttrRes defStyleAttr: Int = 0)
    : LinearLayout(context, attrs, defStyleAttr) {

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

    private val events: MutableMap<Week, List<Event>> = mutableMapOf()
    private val weekViews: MutableMap<Week, WeekView> = mutableMapOf()

    private var currentWeek: Week = Week()

    private val pagerAdapter: InfinitePagerAdapter<Week>

    init {
        orientation = HORIZONTAL
        dividerDrawable = ContextCompat.getDrawable(context, android.R.drawable.divider_horizontal_bright)
        showDividers = SHOW_DIVIDER_MIDDLE

        View.inflate(context, R.layout.view_calendar, this)

        // Load attributes
        val a = context.obtainStyledAttributes(
                attrs, R.styleable.CalendarView, defStyleAttr, 0)

        range = a.getInteger(R.styleable.CalendarView_range, RANGE_WEEK)

        a.recycle()

        pagerAdapter = object : InfinitePagerAdapter<Week>(currentWeek, 2) {
            override fun nextIndicator(current: Week): Week {
                return currentIndicator.toCalendar().apply { add(Calendar.WEEK_OF_YEAR, 1) }.toWeek()
            }

            override fun previousIndicator(current: Week): Week {
                return currentIndicator.toCalendar().apply { add(Calendar.WEEK_OF_YEAR, -1) }.toWeek()
            }

            override var currentIndicatorString: String
                get() = "${currentIndicator.year}-${currentIndicator.week}"
                set(value) {
                    val parts = value.split("-")
                    currentIndicator = Week(parts[0].toInt(), parts[1].toInt())
                }

            override fun instantiateItem(indicator: Week): ViewGroup {
                val view = WeekView(context, _week = indicator).also {
                    it.events = events[indicator] ?: emptyList()
                    it.onEventClickListener = onEventClickListener
                    it.onEventLongClickListener = onEventLongClickListener
                }
                weekViews[indicator] = view
                eventRequestCallback(indicator)
                return view
            }
        }

        hours.week = pagerAdapter.currentIndicator

        pager.adapter = pagerAdapter
        pager.listener = object : InfiniteViewPager.OnInfinitePageChangeListener {
            override fun onPageScrolled(indicator: Any?, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(indicator: Any?) {
            }

            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager.SCROLL_STATE_IDLE)
                    hours.week = pagerAdapter.currentIndicator
            }
        }
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


    fun setEventsForWeek(week: Week, events: List<Event>) {
        this.events[week] = events
        weekViews[week]?.events = events
    }

    fun jumpToToday() {
        pager.setCurrentIndicator(Week())
    }
}
