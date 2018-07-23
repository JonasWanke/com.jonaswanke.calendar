package com.jonaswanke.calendar

import android.support.v4.view.PagerAdapter
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout


abstract class InfinitePagerAdapter<T>(initValue: T, offscreenPages: Int = 1) : PagerAdapter() {

    companion object {
        private val TAG: String = InfinitePagerAdapter::class.java.simpleName
    }

    internal val pageCount: Int = 2 * offscreenPages + 1
    private val pageModels: Array<PageModel<T>?>

    var currentIndicator: T
        internal set

    open var currentIndicatorString: String
        get() = ""
        set(value) {}

    abstract fun nextIndicator(current: T): T

    abstract fun previousIndicator(current: T): T

    init {
        currentIndicator = initValue
        pageModels = arrayOfNulls(pageCount)
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        if (BuildConfig.DEBUG) Log.i(TAG, String.format("instantiating position %s", position))
        val model = createPageModel(position, true)
        pageModels[position] = model
        container.addView(model.wrapper)
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
            for (i in (pageCount - 2) downTo 0)
                move(i, i + 1)
            currentIndicator = previousIndicator(currentIndicator)
            fillPage(0)
            current++
        }
        while (current > center) {
            for (i in 1 until pageCount)
                move(i, i - 1)
            currentIndicator = nextIndicator(currentIndicator)
            fillPage(pageCount - 1)
            current--
        }
    }

    internal fun reset(newIndicator: T) {
        for (pageModel in pageModels)
            pageModel?.wrapper?.removeAllViews()
        currentIndicator = newIndicator
        for (i in 0 until pageCount)
            fillPage(i)
    }

    private fun createPageModel(pagePosition: Int, addToWrapper: Boolean): PageModel<T> {
        val indicator = getIndicatorFromPagePosition(pagePosition)
        val view = instantiateItem(indicator)
        val wrapper = FrameLayout(view.context).apply {
            if (addToWrapper)
                addView(view)
        }

        return PageModel(wrapper, view, indicator)
    }

    private fun fillPage(position: Int) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "setup Page $position")
        val oldModel = pageModels[position]
        val newModel = createPageModel(position, false)
        if (oldModel == null) {
            Log.w(TAG, "fillPage no model found $oldModel $newModel")
            return
        }
        // moving the new created views to the page of the viewpager
        newModel.wrapper.removeView(newModel.view)
        oldModel.wrapper.addView(newModel.view)

        oldModel.indicator = newModel.indicator
        oldModel.view = newModel.view
    }

    abstract fun instantiateItem(indicator: T): View

    override fun getCount(): Int {
        return pageCount
    }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        val model = obj as PageModel<*>
        container.removeView(model.wrapper)
    }

    override fun isViewFromObject(view: View, o: Any): Boolean {
        return view === (o as PageModel<*>).wrapper
    }
}
