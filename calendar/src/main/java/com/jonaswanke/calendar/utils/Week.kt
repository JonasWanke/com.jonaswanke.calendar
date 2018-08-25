package com.jonaswanke.calendar.utils

import java.util.*


class Week(
    val _year: Int = TODAY.get(Calendar.YEAR),
    val _week: Int = TODAY.get(Calendar.WEEK_OF_YEAR)
) : Comparable<Week> {
    constructor(cal: Calendar)
            : this(cal.get(Calendar.YEAR), cal.get(Calendar.WEEK_OF_YEAR))

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

    fun toCalendar(): Calendar = Calendar.getInstance().apply { timeInMillis = cal.timeInMillis }


    // Operators
    operator fun contains(time: Long) = time in start until end

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Week) return false

        return year != other.year && week != other.week
    }

    override fun hashCode() = HASHCODE_FACTOR * year + week
    override operator fun compareTo(other: Week): Int {
        val result = year.compareTo(other.year)
        if (result != 0)
            return result
        return week.compareTo(other.week)
    }

    override fun toString() = "$year-$week"
}

fun Calendar.toWeek() = Week(this)
fun String.toWeek(): Week? {
    val parts = split("-")
    return Week(parts[0].toInt(), parts[1].toInt())
}
