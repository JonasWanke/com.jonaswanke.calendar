package com.jonaswanke.calendar.utils

import java.util.*

private val TODAY: Calendar = Calendar.getInstance().apply {
    timeOfDay = 0
}
private val TOMORROW: Calendar = (TODAY.clone() as Calendar).apply {
    add(Calendar.DAY_OF_WEEK, 1)
}

const val WEEK_IN_DAYS = 7
val WEEK_DAYS = 0 until WEEK_IN_DAYS
const val DAY_IN_HOURS = 24
val DAY_HOURS = 0 until DAY_IN_HOURS
const val HOUR_IN_MINUTES = 60
const val MINUTE_IN_SECONDS = 60

internal const val HASHCODE_FACTOR = 31

fun Long.toCalendar(): Calendar {
    return Calendar.getInstance().apply { timeInMillis = this@toCalendar }
}

val Calendar.isToday: Boolean
    get() = TODAY.timeInMillis <= timeInMillis && timeInMillis < TOMORROW.timeInMillis
val Calendar.isFuture: Boolean
    get() = TODAY.timeInMillis < timeInMillis


data class Week(
    val _year: Int = TODAY.get(Calendar.YEAR),
    val _week: Int = TODAY.get(Calendar.WEEK_OF_YEAR)
) {
    private val cal: Calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, _year)
        set(Calendar.WEEK_OF_YEAR, _week)
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        timeOfDay = 0
    }
    val year = cal.get(Calendar.YEAR)
    val week = cal.get(Calendar.WEEK_OF_YEAR)

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
    val firstDay: Day by lazy { Day(this, cal.firstDayOfWeek) }

    operator fun contains(time: Long) = time in start until end
    override operator fun equals(other: Any?): Boolean {
        if (other !is Week)
            return false

        return year == other.year && week == other.week
    }

    override fun hashCode() = HASHCODE_FACTOR * year + week
    operator fun compareTo(other: Week) = start.compareTo(other.start)

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
