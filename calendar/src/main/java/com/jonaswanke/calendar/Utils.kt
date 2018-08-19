package com.jonaswanke.calendar

import android.text.format.DateUtils.*
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

    operator fun contains(time: Long) = time in start until end

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

    val nextDay: Day by lazy {
        val day = cal.apply { add(Calendar.DAY_OF_WEEK, 1) }.toDay()
        cal.apply { add(Calendar.DAY_OF_WEEK, -1) }
        day
    }
    val prevDay: Day by lazy {
        val day = cal.apply { add(Calendar.DAY_OF_WEEK, -1) }.toDay()
        cal.apply { add(Calendar.DAY_OF_WEEK, 1) }
        day
    }
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
    get() = (get(Calendar.HOUR_OF_DAY).toLong() * HOUR_IN_MILLIS
            + get(Calendar.MINUTE) * MINUTE_IN_MILLIS
            + get(Calendar.SECOND) * SECOND_IN_MILLIS
            + get(Calendar.MILLISECOND))
    set(value) {
        var time = value
        set(Calendar.MILLISECOND, (value % SECOND_IN_MILLIS).toInt())
        time /= SECOND_IN_MILLIS
        set(Calendar.SECOND, (time % MINUTE_IN_SECONDS).toInt())
        time /= MINUTE_IN_SECONDS
        set(Calendar.MINUTE, (time % HOUR_IN_MINUTES).toInt())
        time /= HOUR_IN_MINUTES
        set(Calendar.HOUR_OF_DAY, (time % DAY_IN_HOURS).toInt())
    }

var Calendar.dayOfWeek: Int
    get() = get(Calendar.DAY_OF_WEEK)
    set(value) = set(Calendar.DAY_OF_WEEK, value)


internal fun Calendar.daysUntil(other: Long): Int {
    val time = timeInMillis
    var days = 0
    while (timeInMillis <= other) {
        days++
        add(Calendar.DAY_OF_MONTH, 1)
    }
    while (timeInMillis > other) {
        days--
        add(Calendar.DAY_OF_MONTH, -1)
    }
    timeInMillis = time
    return days
}
