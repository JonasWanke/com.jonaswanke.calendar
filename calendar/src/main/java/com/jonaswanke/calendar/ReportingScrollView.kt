package com.jonaswanke.calendar

import android.content.Context
import androidx.annotation.AttrRes
import android.util.AttributeSet
import android.widget.ScrollView

/**
 * TODO: document your custom view class.
 */
class ReportingScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr) {

    var onScrollChangeListener: ((Int) -> Unit)? = null

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)

        onScrollChangeListener?.invoke(t)
    }
}
