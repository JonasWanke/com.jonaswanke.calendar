package com.jonaswanke.calendar


import android.view.View
import android.view.ViewGroup
import java.util.*

class PageModel<T>(val parentView: ViewGroup, var indicator: T?) {
    private val _children: MutableList<View>

    val children: List<View>
        get() = _children

    init {
        val size = parentView.childCount
        _children = ArrayList(size)

        for (i in 0 until size)
            _children.add(parentView.getChildAt(i))
    }

    fun hasChildren(): Boolean {
        return children.isNotEmpty()
    }


    private fun emptyChildren() {
        _children.clear()
    }

    fun removeAllChildren() {
        parentView.removeAllViews()
        emptyChildren()
    }

    fun addChild(child: View) {
        addViewToParent(child)
        _children.add(child)
    }

    fun removeViewFromParent(view: View) {
        parentView.removeView(view)
    }

    fun addViewToParent(view: View) {
        parentView.addView(view)
    }
}
