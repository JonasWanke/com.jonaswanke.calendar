package com.jonaswanke.calendar

import android.support.annotation.ColorInt

interface Event {
    val id: String

    val title: String

    val description: String

    @get:ColorInt
    val color: Int

    val start: Long

    val end: Long
}
