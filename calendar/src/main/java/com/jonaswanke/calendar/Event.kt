package com.jonaswanke.calendar

import android.support.annotation.ColorInt

interface Event {
    val title: String
    val description: String
    @get:ColorInt
    val color: Int?
    val start: Long
    val end: Long
}

open class BaseEvent(
    override val title: String,
    override val description: String,
    @get:ColorInt
    override val color: Int?,
    override val start: Long,
    override val end: Long
) : Event
