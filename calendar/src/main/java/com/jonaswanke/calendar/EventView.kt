package com.jonaswanke.calendar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.appcompat.view.ContextThemeWrapper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.AttributeSet
import android.widget.TextView
import androidx.core.content.withStyledAttributes
import kotlin.properties.Delegates

/**
 * TODO: document your custom view class.
 */
class EventView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = R.attr.eventViewStyle,
    @StyleRes defStyleRes: Int = R.style.Calendar_EventViewStyle,
    _event: Event? = null
) : TextView(ContextThemeWrapper(context, defStyleRes), attrs, defStyleAttr) {

    var event by Delegates.observable<Event?>(_event) { _, old, new ->
        if (old == new)
            return@observable

        onEventChanged(new)
    }
    private val titleDefault by lazy {
        var default: String? = null
        context.withStyledAttributes(attrs, R.styleable.EventView, defStyleAttr, defStyleRes) {
            default = getString(R.styleable.EventView_titleDefault)
        }
        return@lazy default
    }
    private val title: String?
        get() {
            val title = event?.title
            return if (title == null || title.isBlank())
                titleDefault
            else
                title
        }

    private val backgroundDrawable: Drawable? = ResourcesCompat.getDrawable(context.resources,
            R.drawable.event_background, ContextThemeWrapper(context, defStyleRes).theme)
    private val backgroundColorDefault: Int = 0xFF039BE5.toInt()

    init {
        context.withStyledAttributes(attrs = intArrayOf(android.R.attr.selectableItemBackground)) {
            @SuppressLint("NewApi")
            foreground = getDrawable(0)
        }

        onEventChanged(event)
    }

    private fun onEventChanged(event: Event?) {
        if (event == null) {
            text = null
            background = null
            return
        }

        if (text.isNullOrBlank()) {
            val builder = SpannableStringBuilder(title)
            val titleEnd = builder.length
            builder.setSpan(StyleSpan(Typeface.BOLD), 0, titleEnd, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            if (event.description != null)
                builder.append("\n").append(event.description)
            text = builder
        }

        backgroundDrawable?.also {
            DrawableCompat.setTint(it, event.color ?: backgroundColorDefault)
        }
        background = backgroundDrawable
    }
}
