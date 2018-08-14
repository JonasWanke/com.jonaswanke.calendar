package com.jonaswanke.calendar

import androidx.annotation.ColorInt

interface Event {
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
    override fun toString(): String {
        return "$title ($description), $start-$end, allDay: $allDay"
    }
}

class AddEvent(start: Long, end: Long) :
        BaseEvent("", null, null, start, end, false)
