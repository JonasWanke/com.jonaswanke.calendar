package com.jonaswanke.calendar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.support.annotation.AttrRes
import android.util.AttributeSet
import android.util.Log
import android.view.View

/**
 * TODO: document your custom view class.
 */
open class CalendarView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, @AttrRes defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {

    init {
        // Load attributes
        val a = context.obtainStyledAttributes(
                attrs, R.styleable.CalendarView, defStyleAttr, 0)

        a.recycle()
    }

    val _paddingLeft = paddingLeft
    val _paddingTop = paddingTop
    val _paddingRight = paddingRight
    val _paddingBottom = paddingBottom

    val _contentWidth = width - paddingLeft - paddingRight
    val _contentHeight = height - paddingTop - paddingBottom

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        Log.d("CalendarView", "onDraw")
        canvas?.drawRect(0.0f, 0.0f, 200.0f, 200.0f, Paint().apply { color = Color.RED })
    }
}
