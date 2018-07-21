package com.jonaswanke.calendar

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.support.annotation.AttrRes
import android.support.annotation.IntDef
import android.util.AttributeSet
import android.util.Log
import android.view.View

/**
 * TODO: document your custom view class.
 */
open class CalendarView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, @AttrRes defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {

    companion object {
        const val RANGE_DAY = 1
        const val RANGE_3_DAYS = 3
        const val RANGE_WEEK = 7
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(RANGE_DAY, RANGE_3_DAYS, RANGE_WEEK)
    annotation class Range


    var onEventClickListener: ((String) -> Unit)? = null
    var onEventLongClickListener: ((String) -> Unit)? = null

    var eventProvider: EventProvider = object : EventProvider {
        override fun provideEvents(year: Int, week: Int): LiveData<List<Event>> {
            return MutableLiveData<List<Event>>().apply { value = emptyList() }
        }
    }

    @get: Range
    var range: Int = RANGE_WEEK

    init {
        // Load attributes
        val a = context.obtainStyledAttributes(
                attrs, R.styleable.CalendarView, defStyleAttr, 0)

        a.recycle()
    }


    private val _paddingLeft = paddingLeft
    private val _paddingTop = paddingTop
    private val _paddingRight = paddingRight
    private val _paddingBottom = paddingBottom

    private val _contentWidth = width - paddingLeft - paddingRight
    private val _contentHeight = height - paddingTop - paddingBottom

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        Log.d("CalendarView", "onDraw")
        canvas?.drawRect(0.0f, 0.0f, 200.0f, 200.0f, Paint().apply { color = Color.RED })
    }


    interface EventProvider {
        fun provideEvents(year: Int, week: Int): LiveData<List<Event>>
    }
}
