package com.jonaswanke.calendar.utils

import android.text.format.DateUtils.*
import java.util.*


class Day(
    _year: Int = TODAY.get(Calendar.YEAR),
    _week: Int = TODAY.get(Calendar.WEEK_OF_YEAR),
    _day: Int = TODAY.get(Calendar.DAY_OF_WEEK)
) : Comparable<Day> {
    constructor(week: Week, day: Int) : this(week.year, week.week, day)
    constructor(cal: Calendar)
            : this(cal.get(Calendar.YEAR), cal.get(Calendar.WEEK_OF_YEAR), cal.get(Calendar.DAY_OF_WEEK))

    private val cal: Calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, _year)
        set(Calendar.WEEK_OF_YEAR, _week)
        set(Calendar.DAY_OF_WEEK, _day)
        timeOfDay = 0
    }
    val year = cal.get(Calendar.YEAR)
    val week = cal.get(Calendar.WEEK_OF_YEAR)
    val day = cal.get(Calendar.DAY_OF_WEEK)

    val start = cal.timeInMillis
    val end: Long by lazy {
        val end = cal.apply { add(Calendar.DAY_OF_WEEK, 1) }.timeInMillis
        cal.add(Calendar.DAY_OF_WEEK, -1)
        end
    }

    val isToday = TODAY.timeInMillis <= start && start < TOMORROW.timeInMillis
    val isFuture = TODAY.timeInMillis < start

    val weekObj: Week by lazy { Week(year, week) }
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

    fun toCalendar(): Calendar = Calendar.getInstance().apply { timeInMillis = cal.timeInMillis }

    // Operators
    operator fun plus(days: Int): Day {
        // Only intended for small differences
        var currentDay = this
        var delta = days
        while (delta > 0) {
            currentDay = currentDay.nextDay
            delta--
        }
        while (delta < 0) {
            currentDay = currentDay.prevDay
            delta++
        }
        return currentDay
    }

    operator fun minus(days: Int) = plus(-days)
    operator fun minus(other: Day): Int {
        if (other == this)
            return 0
        if (other > this)
            return other - this

        var current = other
        var diff = 0
        while (current < this) {
            current = current.nextDay
            diff++
        }
        return diff
    }

    operator fun inc() = nextDay
    operator fun dec() = prevDay

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Day) return false

        return year == other.year && week == other.week && day == other.day
    }

    override fun hashCode() = (HASHCODE_FACTOR * year + week) * HASHCODE_FACTOR + day
    override operator fun compareTo(other: Day): Int {
        var result = year.compareTo(other.year)
        if (result != 0)
            return result
        result = week.compareTo(other.week)
        if (result != 0)
            return result
        return day.compareTo(other.day)
    }

    fun range(length: Int) = this..this + (length - 1)
    operator fun rangeTo(that: Day) = DayRange(this, that)

    override fun toString() = "$year-$week-$day"
}

fun Calendar.toDay() = Day(this)
fun String.toDay(): Day {
    val parts = split("-")
    return Day(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
}


class DayRange(override val start: Day, override val endInclusive: Day) : ClosedRange<Day>, Iterable<Day> {
    val endExclusive = endInclusive.nextDay
    val length = endInclusive - start + 1

    override fun iterator() = DayIterator(start, endInclusive)
}

class DayIterator(start: Day, private val endInclusive: Day) : Iterator<Day> {
    private var current = start

    override fun hasNext() = current <= endInclusive
    override fun next() = current++
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
