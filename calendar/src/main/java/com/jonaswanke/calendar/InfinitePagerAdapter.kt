package com.jonaswanke.calendar

import androidx.viewpager.widget.PagerAdapter
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout


abstract class InfinitePagerAdapter<T, V : View>(initValue: T, offscreenPages: Int = 1) : PagerAdapter() {

    companion object {
        private val TAG: String = InfinitePagerAdapter::class.java.simpleName
    }

    internal val pageCount: Int = 2 * offscreenPages + 1
    private val pageModels: Array<PageModel<T, V>?>

    var currentIndicator: T
        internal set

    open var currentIndicatorString: String
        get() = ""
        set(_) {}

    abstract fun nextIndicator(current: T): T

    abstract fun previousIndicator(current: T): T

    init {
        currentIndicator = initValue
        pageModels = arrayOfNulls(pageCount)
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        if (BuildConfig.DEBUG)
            Log.i(TAG, String.format("instantiating position %s", position))

        val indicator = getIndicatorFromPagePosition(position)
        val view = instantiateItem(indicator, null)
        val wrapper = FrameLayout(view.context).apply {
            addView(view)
        }
        val model = PageModel(wrapper, view, indicator)
        pageModels[position] = model
        container.addView(wrapper)
        return model
    }

    private fun getIndicatorFromPagePosition(pagePosition: Int): T {
        var diff = pagePosition - center
        var indicator = currentIndicator
        while (diff > 0) {
            indicator = nextIndicator(indicator)
            diff--
        }
        while (diff < 0) {
            indicator = previousIndicator(indicator)
            diff++
        }
        return indicator
    }

    internal val center
        get() = pageCount / 2

    internal fun cycleBack(from: Int) {
        fun move(from: Int, to: Int) {
            val fromModel = pageModels[from]
            val toModel = pageModels[to]
            if (fromModel == null || toModel == null) {
                Log.w(TAG, "cycleBack.move no model found $fromModel $toModel")
                return
            }
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Moving page $from to $to, indicator from ${fromModel.indicator} to ${toModel.indicator}")

            fromModel.wrapper.removeView(fromModel.view)
            toModel.wrapper.addView(fromModel.view)

            toModel.indicator = fromModel.indicator
            toModel.view = fromModel.view
        }

        var current = from
        while (current < center) {
            val oldView = pageModels[pageCount - 1]?.run {
                wrapper.removeViewAt(0)
                view
            }
            for (i in (pageCount - 2) downTo 0)
                move(i, i + 1)
            // Recycle old view
            if (oldView != null)
                pageModels[0]?.view = oldView

            currentIndicator = previousIndicator(currentIndicator)
            fillPage(0)
            current++
        }
        while (current > center) {
            val oldView = pageModels[0]?.run {
                wrapper.removeViewAt(0)
                view
            }
            for (i in 1 until pageCount)
                move(i, i - 1)
            // Recycle old view
            if (oldView != null)
                pageModels[pageCount - 1]?.view = oldView

            currentIndicator = nextIndicator(currentIndicator)
            fillPage(pageCount - 1)
            current--
        }
    }

    internal fun reset(newIndicator: T) {
        currentIndicator = newIndicator

        val center = pageCount / 2
        for (i in 0 until pageCount)
            // Start at the center and move outwards
            fillPage(center + if (i % 2 == 0) i / 2 else -(i / 2 + 1))
    }

    private fun fillPage(position: Int) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "setup Page $position")
        val oldModel = pageModels[position]
        if (oldModel == null) {
            Log.w(TAG, "fillPage no model found $oldModel")
            return
        }

        // moving the new created views to the page of the viewpager
        val oldView = oldModel.view
        val parent = oldView.parent
        if (parent != null && parent is ViewGroup)
            parent.removeView(oldView)
        oldModel.wrapper.removeView(oldView)

        val indicator = getIndicatorFromPagePosition(position)
        val view = instantiateItem(indicator, oldView)
        oldModel.indicator = indicator
        oldModel.view = view
        oldModel.wrapper.addView(view)
    }

    abstract fun instantiateItem(indicator: T, oldView: V?): V

    override fun getCount(): Int {
        return pageCount
    }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        val model = obj as PageModel<*, *>
        container.removeView(model.wrapper)
    }

    override fun isViewFromObject(view: View, o: Any): Boolean {
        return view === (o as PageModel<*, *>).wrapper
    }
}
