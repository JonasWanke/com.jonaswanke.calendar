package com.jonaswanke.calendar

import java.util.*

private val TODAY: Calendar = Calendar.getInstance()

data class Week(
        var year: Int = TODAY.get(Calendar.YEAR),
        var week: Int = TODAY.get(Calendar.WEEK_OF_YEAR)
)

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
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

data class Day(
        var year: Int = TODAY.get(Calendar.YEAR),
        var week: Int = TODAY.get(Calendar.WEEK_OF_YEAR),
        var day: Int = TODAY.get(Calendar.DAY_OF_WEEK)
) {
    constructor(week: Week, day: Int) : this(week.year, week.week, day)
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
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}
