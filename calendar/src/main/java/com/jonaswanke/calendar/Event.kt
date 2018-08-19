package com.jonaswanke.calendar

import androidx.annotation.ColorInt

interface Event : Comparable<Event> {
    val title: String
    val description: String?
    @get:ColorInt
    val color: Int?
    val start: Long
    val end: Long
    val allDay: Boolean
}

open class BaseEvent(
    override val title: String,
    override val description: String?,
    @get:ColorInt
    override val color: Int?,
    override val start: Long,
    override val end: Long,
    override val allDay: Boolean = false
) : Event {
    override fun compareTo(other: Event): Int {
        val result = start.compareTo(other.start)
        if (result != 0)
            return result
        return end.compareTo(other.end)
    }

    override fun toString(): String {
        return "$title ($description), $start-$end, allDay: $allDay"
    }
}

class AddEvent(start: Long, end: Long) :
        BaseEvent("", null, null, start, end, false)
