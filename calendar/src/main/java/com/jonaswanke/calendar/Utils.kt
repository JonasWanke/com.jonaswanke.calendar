package com.jonaswanke.calendar

import android.support.v4.view.ViewCompat
import android.text.format.DateUtils
import android.view.View
import java.util.*

private val TODAY: Calendar = Calendar.getInstance().apply {
    timeOfDay = 0
}
private val TOMORROW: Calendar = (TODAY.clone() as Calendar).apply {
    add(Calendar.DAY_OF_WEEK, 1)
}

fun Long.asCalendar(): Calendar {
    return Calendar.getInstance().apply { timeInMillis = this@asCalendar }
}

val Calendar.isToday: Boolean
    get() = TODAY.timeInMillis <= timeInMillis && timeInMillis < TOMORROW.timeInMillis
val Calendar.isFuture: Boolean
    get() = TODAY.timeInMillis < timeInMillis


data class Week(
        val year: Int = TODAY.get(Calendar.YEAR),
        val week: Int = TODAY.get(Calendar.WEEK_OF_YEAR)
) {
    private val cal: Calendar = toCalendar()
    val start = cal.timeInMillis
    val end: Long by lazy {
        val end = cal.apply { add(Calendar.WEEK_OF_YEAR, 1) }.timeInMillis
        cal.add(Calendar.WEEK_OF_YEAR, -1)
        end
    }

    val isToday = TODAY.timeInMillis <= start && start < TOMORROW.timeInMillis
    val isFuture = TODAY.timeInMillis < start

    val nextWeek: Week by lazy {
        val week = cal.apply { add(Calendar.WEEK_OF_YEAR, 1) }.toWeek()
        cal.apply { add(Calendar.WEEK_OF_YEAR, -1) }
        if (week.year > year || week.week > this.week)
            week
        else
            Week(year + 1, week.week)
    }
    val prevWeek: Week by lazy {
        val week = cal.apply { add(Calendar.WEEK_OF_YEAR, -1) }.toWeek()
        cal.apply { add(Calendar.WEEK_OF_YEAR, 1) }
        if (week.year >= year || week.week > this.week)
            week
        else
            Week(year, week.week)
    }

    override fun toString(): String {
        return "$year-$week"
    }
}

fun Calendar.toWeek(): Week {
    return Week(
            get(Calendar.YEAR),
            get(Calendar.WEEK_OF_YEAR))
}

fun String.toWeek(): Week? {
    val parts = split("-")
    return Week(parts[0].toInt(), parts[1].toInt())
}

fun Week.toCalendar(): Calendar =
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.WEEK_OF_YEAR, week)
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            timeOfDay = 0
        }


data class Day(
        val year: Int = TODAY.get(Calendar.YEAR),
        val week: Int = TODAY.get(Calendar.WEEK_OF_YEAR),
        val day: Int = TODAY.get(Calendar.DAY_OF_WEEK)
) {
    constructor(week: Week, day: Int) : this(week.year, week.week, day)

    private val cal: Calendar = toCalendar()

    val start = cal.timeInMillis
    val end: Long by lazy {
        val end = cal.apply { add(Calendar.DAY_OF_WEEK, 1) }.timeInMillis
        cal.add(Calendar.DAY_OF_WEEK, -1)
        end
    }

    val isToday = TODAY.timeInMillis <= start && start < TOMORROW.timeInMillis
    val isFuture = TODAY.timeInMillis < start
}

fun Calendar.toDay(): Day {
    return Day(
            get(Calendar.YEAR),
            get(Calendar.WEEK_OF_YEAR),
            get(Calendar.DAY_OF_WEEK))
}

fun Day.toCalendar(): Calendar = Calendar.getInstance().apply {
    set(Calendar.YEAR, year)
    set(Calendar.WEEK_OF_YEAR, week)
    set(Calendar.DAY_OF_WEEK, day)
    timeOfDay = 0
}

var Calendar.timeOfDay: Long
    get() = (get(Calendar.HOUR_OF_DAY).toLong() * DateUtils.HOUR_IN_MILLIS
            + get(Calendar.MINUTE) * DateUtils.MINUTE_IN_MILLIS
            + get(Calendar.SECOND) * DateUtils.SECOND_IN_MILLIS
            + get(Calendar.MILLISECOND))
    set(value) {
        var time = value
        set(Calendar.MILLISECOND, (value % DateUtils.SECOND_IN_MILLIS).toInt())
        time /= DateUtils.SECOND_IN_MILLIS
        set(Calendar.SECOND, (time % 60).toInt())
        time /= 60
        set(Calendar.MINUTE, (time % 60).toInt())
        time /= 60
        set(Calendar.HOUR_OF_DAY, (time % 24).toInt())
    }

var Calendar.dayOfWeek: Int
    get() = get(Calendar.DAY_OF_WEEK)
    set(value) = set(Calendar.DAY_OF_WEEK, value)


inline fun View.doOnNextLayout(crossinline action: (view: View) -> Unit) {
    addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
        override fun onLayoutChange(view: View,
                                    left: Int, top: Int, right: Int, bottom: Int,
                                    oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
            view.removeOnLayoutChangeListener(this)
            action(view)
        }
    })
}

inline fun View.doOnLayout(crossinline action: (view: View) -> Unit) {
    if (ViewCompat.isLaidOut(this) && !isLayoutRequested)
        action(this)
    else
        doOnNextLayout {
            action(it)
        }
}
