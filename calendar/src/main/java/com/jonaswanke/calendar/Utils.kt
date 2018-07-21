package com.jonaswanke.calendar

import java.util.*

typealias Week = Pair<Int, Int>

fun Calendar.toWeek(): Week = run { get(Calendar.YEAR) to get(Calendar.WEEK_OF_YEAR) }
fun Week.toCalendar(): Calendar = Calendar.getInstance().apply {
    set(Calendar.YEAR, first)
    set(Calendar.WEEK_OF_YEAR, second)
    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}
