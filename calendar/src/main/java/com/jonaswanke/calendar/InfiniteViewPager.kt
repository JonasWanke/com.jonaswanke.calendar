package com.jonaswanke.calendar

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.util.Log
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class InfiniteViewPager @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null)
    : ViewPager(context, attrs) {

    companion object {
        private val TAG: String = InfiniteViewPager::class.java.simpleName

        const val STATE_SUPER = "STATE_SUPER"
        const val STATE_ADAPTER = "STATE_ADAPTER"
    }

    init {
        addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(i: Int, positionOffset: Float, positionOffsetPixels: Int) {
                val adapter = adapter as InfinitePagerAdapter<*>? ?: return
                listener?.onPageScrolled(adapter.currentIndicator, positionOffset, positionOffsetPixels)
            }

            override fun onPageSelected(position: Int) {
                positionCurrent = position
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "on page $position")

                val adapter = adapter as InfinitePagerAdapter<*>? ?: return
                listener?.onPageSelected(adapter.currentIndicator)
            }

            override fun onPageScrollStateChanged(state: Int) {
                @Suppress("UNCHECKED_CAST")
                val adapter = adapter as InfinitePagerAdapter<Any>? ?: return

                if (state != ViewPager.SCROLL_STATE_IDLE)
                    return

                adapter.cycleBack(positionCurrent)
                setCurrentItem(adapter.center, false)
                listener?.onPageScrollStateChanged(state)
            }
        })
    }

    private var positionCurrent = -1
    var listener: OnInfinitePageChangeListener? = null


    override fun setCurrentItem(item: Int) {
        if (item != (adapter as? InfinitePagerAdapter<*>)?.center ?: -1) {
            throw RuntimeException("Cannot change page index unless its 1.")
        }
        super.setCurrentItem(item)
    }

    override fun setOffscreenPageLimit(limit: Int) {
        if (limit != offscreenPageLimit)
            throw RuntimeException("OffscreenPageLimit cannot be changed.")
        super.setOffscreenPageLimit(limit)
    }

    override fun setAdapter(adapter: PagerAdapter?) {
        if (adapter is InfinitePagerAdapter<*>) {
            super.setAdapter(adapter)
            super.setOffscreenPageLimit(adapter.pageCount)
            positionCurrent = adapter.center
            currentItem = adapter.center
        } else
            throw IllegalArgumentException("Adapter should be an instance of InfinitePagerAdapter.")
    }

    fun <T : Any> setCurrentIndicator(indicator: T) {
        val adapter = adapter as? InfinitePagerAdapter<*> ?: return
        val currentIndicator = adapter.currentIndicator
        if (currentIndicator!!.javaClass != indicator.javaClass)
            return

        launch(UI) {
            @Suppress("UNCHECKED_CAST")
            (adapter as InfinitePagerAdapter<T>).reset(indicator)
            listener?.onPageScrollStateChanged(SCROLL_STATE_IDLE)
        }
    }

    override fun onSaveInstanceState(): Parcelable? {
        val adapter = adapter as InfinitePagerAdapter<*>?
        if (adapter == null) {
            Log.d(TAG, "onSaveInstanceState adapter == null")
            return super.onSaveInstanceState()
        }

        val bundle = Bundle()
        bundle.putParcelable(STATE_SUPER, super.onSaveInstanceState())
        bundle.putString(STATE_ADAPTER, adapter.currentIndicatorString)

        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val adapter = adapter as InfinitePagerAdapter<*>?
        if (adapter == null) {
            if (BuildConfig.DEBUG)
                Log.w(TAG, "onRestoreInstanceState adapter == null")
            super.onRestoreInstanceState(state)
            return
        }

        if (state is Bundle) {
            val representation = state.getString(STATE_ADAPTER)
            adapter.currentIndicatorString = representation!!
            super.onRestoreInstanceState(state.getParcelable(STATE_SUPER))
            return
        }

        super.onRestoreInstanceState(state)
    }


    interface OnInfinitePageChangeListener {

        /**
         * This method will be invoked when the current page is scrolled, either as part
         * of a programmatically initiated smooth scroll or a user initiated touch scroll.
         *
         * @param indicator Indicator of the first page currently being displayed.
         * @param positionOffset Value from [0, 1) indicating the offset from the page at position.
         * @param positionOffsetPixels Value in pixels indicating the offset from position.
         */
        fun onPageScrolled(indicator: Any?, positionOffset: Float, positionOffsetPixels: Int)

        /**
         * This method will be invoked when a new page has been selected.
         * @param indicator the indicator of this page.
         */
        fun onPageSelected(indicator: Any?)

        /**
         * Called when the scroll state changes. Useful for discovering when the user
         * begins dragging, when the pager is automatically settling to the current page,
         * or when it is fully stopped/idle.
         *
         * @param state The new scroll state.
         * @see ViewPager.SCROLL_STATE_IDLE
         * @see ViewPager.SCROLL_STATE_DRAGGING
         * @see ViewPager.SCROLL_STATE_SETTLING
         */
        fun onPageScrollStateChanged(state: Int)

    }
}
