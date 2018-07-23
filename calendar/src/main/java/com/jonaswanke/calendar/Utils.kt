package com.jonaswanke.calendar

import android.text.format.DateUtils
import java.util.*

private val TODAY: Calendar = Calendar.getInstance().apply {
    timeOfDay = 0
}

fun Long.asCalendar(): Calendar {
    return Calendar.getInstance().apply { timeInMillis = this@asCalendar }
}

data class Week(
        val year: Int = TODAY.get(Calendar.YEAR),
        val week: Int = TODAY.get(Calendar.WEEK_OF_YEAR)
) {
    private val cal: Calendar = toCalendar()
    val start = cal.timeInMillis
    val end = cal.timeInMillis + DateUtils.WEEK_IN_MILLIS
}

fun Calendar.toWeek(): Week {
    return Week(
            get(Calendar.YEAR),
            get(Calendar.WEEK_OF_YEAR))
}

fun Week.toCalendar(): Calendar =
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.WEEK_OF_YEAR, week)
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
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
    val end = cal.timeInMillis + DateUtils.DAY_IN_MILLIS

    val isToday = TODAY.timeInMillis <= start && start < TODAY.timeInMillis + DateUtils.DAY_IN_MILLIS
    val isFuture = start > TODAY.timeInMillis
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
