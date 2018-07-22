package com.jonaswanke.calendar

import android.support.v4.view.PagerAdapter
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout


abstract class InfinitePagerAdapter<T>(initValue: T) : PagerAdapter() {

    companion object {
        private val TAG: String = InfinitePagerAdapter::class.java.simpleName

        const val POSITION_LEFT = 0
        const val POSITION_CENTER = 1
        const val POSITION_RIGHT = 2

        const val PAGE_COUNT = 3
    }

    private val pageModels: Array<PageModel<T>?>

    var currentIndicator: T
        internal set

    open var currentIndicatorString: String
        get() = ""
        set(value) {}

    abstract val nextIndicator: T

    abstract val previousIndicator: T

    init {
        currentIndicator = initValue
        pageModels = arrayOfNulls(PAGE_COUNT)
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        if (BuildConfig.DEBUG) Log.i(TAG, String.format("instantiating position %s", position))
        val model = createPageModel(position)
        pageModels[position] = model
        container.addView(model.wrapper)
        return model
    }

    internal fun fillPage(position: Int) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "setup Page $position")
            printPageModels("before newPage")
        }
        val oldModel = pageModels[position]
        val newModel = createPageModel(position)
        if (oldModel == null) {
            Log.w(TAG, "fillPage no model found $oldModel $newModel")
            return
        }
        // moving the new created views to the page of the viewpager
        oldModel.wrapper.removeAllViews()
        newModel.wrapper.removeView(newModel.view)
        oldModel.wrapper.addView(newModel.view)

        pageModels[position]?.indicator = newModel.indicator
    }

    private fun createPageModel(pagePosition: Int): PageModel<T> {
        val indicator = getIndicatorFromPagePosition(pagePosition)
        val view = instantiateItem(indicator)
        val wrapper = FrameLayout(view.context).apply {
            addView(view)
        }

        return PageModel(wrapper, view, indicator)
    }

    private fun getIndicatorFromPagePosition(pagePosition: Int): T {
        return when (pagePosition) {
            POSITION_LEFT -> previousIndicator
            POSITION_CENTER -> currentIndicator
            POSITION_RIGHT -> nextIndicator
            else -> throw IllegalArgumentException()
        }
    }

    internal fun movePageContents(from: Int, to: Int) {
        val fromModel = pageModels[from]
        val toModel = pageModels[to]
        if (fromModel == null || toModel == null) {
            Log.w(TAG, "fillPage no model found $fromModel $toModel")
            return
        }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Moving page $from to $to, indicator from ${fromModel.indicator} to ${toModel.indicator}")
            printPageModels("before")
        }

        toModel.wrapper.removeAllViews()
        fromModel.wrapper.removeView(fromModel.view)
        toModel.wrapper.addView(fromModel.view)

        if (BuildConfig.DEBUG) printPageModels("transfer")
        toModel.indicator = fromModel.indicator
        if (BuildConfig.DEBUG) printPageModels("after")
    }

    internal fun reset() {
        for (pageModel in pageModels)
            pageModel?.wrapper?.removeAllViews()
    }

    abstract fun instantiateItem(indicator: T): View

    override fun getCount(): Int {
        return PAGE_COUNT
    }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        val model = obj as PageModel<*>
        container.removeView(model.wrapper)
    }

    override fun isViewFromObject(view: View, o: Any): Boolean {
        return view === (o as PageModel<*>).wrapper
    }


    private fun printPageModels(tag: String) {
        for (i in 0 until PAGE_COUNT)
            printPageModel(tag, pageModels[i], i)
    }

    private fun printPageModel(tag: String, model: PageModel<T>?, modelPos: Int) {
        Log.d(TAG, "$tag: ModelPos $modelPos, indicator ${model?.indicator}, " +
                "tag ${model?.wrapper?.tag}")
    }
}
