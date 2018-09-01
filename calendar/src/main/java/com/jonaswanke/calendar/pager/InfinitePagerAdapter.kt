package com.jonaswanke.calendar.pager

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.viewpager.widget.PagerAdapter
import com.jonaswanke.calendar.BuildConfig


abstract class InfinitePagerAdapter<T, V : View>(initValue: T, private val offscreenPages: Int = 1) : PagerAdapter() {
    companion object {
        private val TAG: String = InfinitePagerAdapter::class.java.simpleName

        private const val CACHE_SIZE = 5
    }

    private val defaultCenter = offscreenPages * CACHE_SIZE
    internal val pageCount = defaultCenter * 2
    private val pageModels: MutableMap<Int, PageModel<T, V>?>

    var currentIndicator: T
        internal set
    open var currentIndicatorString: String
        get() = ""
        set(_) {}

    private var _currentPosition: Int = defaultCenter
    var currentPosition: Int
        get() = _currentPosition
        set(value) {
            currentIndicator = indicatorFromPosition(value)


        }

    private val pagesToLoad get() = currentPosition - offscreenPages..currentPosition + offscreenPages

    init {
        currentIndicator = initValue
        pageModels = mutableMapOf()
    }


    // PagerAdapter
    final override fun instantiateItem(container: ViewGroup, position: Int): Any {
        if (BuildConfig.DEBUG)
            Log.i(TAG, String.format("instantiating position %s", position))

        val indicator = indicatorFromPosition(position)
        val view = indicator.takeIf { position in pagesToLoad }
                ?.let { instantiateItem(it, null) }
        val wrapper = FrameLayout(container.context).apply {
            view?.also { addView(it) }
        }
        val model = PageModel(wrapper, view, indicator)
        pageModels[position] = model

        container.addView(model.wrapper)
        return model
    }

    override fun getCount() = pageCount
    override fun isViewFromObject(view: View, o: Any) = view === (o as PageModel<*, *>).wrapper

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        container.removeView((obj as PageModel<*, *>).wrapper)
    }


    // Custom
    abstract fun instantiateItem(indicator: T, oldView: V?): V

    internal fun setPosition(position: Int): Int {
        return when (position) {
            currentPosition -> position
            in offscreenPages..(pageCount - offscreenPages) -> {
                var delta = position - currentPosition
                while (delta > 0) {
                    move(currentPosition - offscreenPages, currentPosition + offscreenPages + 1)
                    fillPage(currentPosition + offscreenPages + 1)
                    _currentPosition++
                    currentIndicator = nextIndicator(currentIndicator)
                    delta--
                }
                while (delta < 0) {
                    move(currentPosition + offscreenPages, currentPosition - offscreenPages - 1)
                    fillPage(currentPosition - offscreenPages - 1)
                    _currentPosition--
                    currentIndicator = previousIndicator(currentIndicator)
                    delta++
                }
                position
            }
            else -> {
                cycleBack()
                defaultCenter
            }
        }
    }

    internal fun reset(newIndicator: T) {
        currentIndicator = newIndicator
        _currentPosition = defaultCenter

        for (i in 0..offscreenPages * 2)
        // Start at the currentPosition and move outwards
            fillPage(_currentPosition + if (i % 2 == 0) i / 2 else -(i / 2 + 1))
    }

    abstract fun nextIndicator(current: T): T
    abstract fun previousIndicator(current: T): T


    // Helpers
    private fun cycleBack() {
        val delta = defaultCenter - currentPosition
        for (position in pagesToLoad)
            move(position, position + delta)
        _currentPosition = defaultCenter
    }

    private fun move(from: Int, to: Int) {
        val fromModel = pageModels[from]
        val toModel = pageModels[to]
        if (fromModel == null || toModel == null) {
            Log.w(TAG, "cycleBack.move no model found $fromModel $toModel")
            return
        }
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Moving page $from to $to, indicator from ${fromModel.indicator} to ${toModel.indicator}")

        fromModel.view?.also {
            fromModel.wrapper.removeView(it)
            toModel.wrapper.addView(it)
        }
        toModel.indicator = fromModel.indicator
        toModel.view = fromModel.view
    }

    private fun indicatorFromPosition(position: Int): T {
        var diff = position - currentPosition
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

    private fun fillPage(position: Int) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Fill page $position")
        val model = pageModels[position]
        if (model == null) {
            Log.w(TAG, "fillPage: no model found")
            return
        }

        // moving the new created views to the page of the viewpager
        val oldView = model.view
        if (oldView != null) {
            (oldView.parent as? ViewGroup)?.removeView(oldView)
            model.wrapper.removeView(oldView)
        }

        val indicator = indicatorFromPosition(position)
        val view = instantiateItem(indicator, oldView)
        model.indicator = indicator
        model.view = view
        model.wrapper.addView(view)
    }
}
