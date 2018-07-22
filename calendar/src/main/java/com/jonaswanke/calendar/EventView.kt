package com.jonaswanke.calendar

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.support.annotation.AttrRes
import android.util.AttributeSet
import android.widget.TextView
import kotlin.properties.Delegates

/**
 * TODO: document your custom view class.
 */
class EventView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, @AttrRes defStyleAttr: Int = 0)
    : TextView(context, attrs, defStyleAttr) {

    var event by Delegates.observable<Event?>(null) { _, old, new ->
        if (old == new)
            return@observable

        onEventChanged(new)
    }

    private fun onEventChanged(event: Event?) {
        if (event != null) {
            // TODO: use spans
            text = "${event.title} (${event.description})"
            background = ColorDrawable(event.color)
        }
    }
}
