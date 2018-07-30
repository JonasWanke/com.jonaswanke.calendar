package com.jonaswanke.calendar

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.support.annotation.AttrRes
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.view.ContextThemeWrapper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.AttributeSet
import android.view.Gravity
import android.widget.TextView
import kotlin.properties.Delegates

/**
 * TODO: document your custom view class.
 */
class EventView @JvmOverloads constructor(context: Context,
                                          attrs: AttributeSet? = null,
                                          @AttrRes defStyleAttr: Int = R.attr.eventViewStyle,
                                          _event: Event? = null)
    : TextView(ContextThemeWrapper(context, R.style.Calendar_EventViewStyle), attrs, defStyleAttr) {

    var event by Delegates.observable<Event?>(_event) { _, old, new ->
        if (old == new)
            return@observable

        onEventChanged(new)
    }

    private val backgroundDrawable: Drawable?
    private val backgroundColorDefault: Int

    init {
        gravity = Gravity.START or Gravity.TOP

        backgroundDrawable = ResourcesCompat.getDrawable(context.resources,
                R.drawable.event_background, ContextThemeWrapper(context, R.style.Calendar_EventViewStyle).theme)
        backgroundColorDefault = 0xFF039BE5.toInt()

        val a = context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
        foreground = a.getDrawable(0)
        a.recycle()
    }

    private fun onEventChanged(event: Event?) {
        if (event == null) {
            text = null
            background = null
            return
        }

        val builder = SpannableStringBuilder(event.title)
        val titleEnd = builder.length
        builder.setSpan(StyleSpan(Typeface.BOLD), 0, titleEnd, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        builder.append("\n").append(event.description)
        text = builder

        backgroundDrawable?.also {
            DrawableCompat.setTint(it, event.color ?: backgroundColorDefault)
        }
        background = backgroundDrawable
    }
}
