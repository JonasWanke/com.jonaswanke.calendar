package com.jonaswanke.calendar

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.util.Log
import com.jonaswanke.calendar.InfinitePagerAdapter.Companion.PAGE_COUNT
import com.jonaswanke.calendar.InfinitePagerAdapter.Companion.POSITION_CENTER
import com.jonaswanke.calendar.InfinitePagerAdapter.Companion.POSITION_LEFT
import com.jonaswanke.calendar.InfinitePagerAdapter.Companion.POSITION_RIGHT

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
                val listener = listener ?: return
                val adapter = adapter as InfinitePagerAdapter<*>? ?: return

                listener.onPageScrolled(adapter.currentIndicator, positionOffset, positionOffsetPixels)
            }

            override fun onPageSelected(position: Int) {
                mCurrPosition = position
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "on page $position")

                val listener = listener ?: return
                val adapter = adapter as InfinitePagerAdapter<*>? ?: return
                listener.onPageSelected(adapter.currentIndicator)
            }

            override fun onPageScrollStateChanged(state: Int) {
                listener?.onPageScrollStateChanged(state)
                @Suppress("UNCHECKED_CAST")
                val adapter = adapter as InfinitePagerAdapter<Any>? ?: return

                if (state != ViewPager.SCROLL_STATE_IDLE)
                    return

                if (mCurrPosition == POSITION_LEFT) {
                    adapter.movePageContents(POSITION_CENTER, POSITION_RIGHT)
                    adapter.movePageContents(POSITION_LEFT, POSITION_CENTER)
                    adapter.currentIndicator = adapter.previousIndicator
                    adapter.fillPage(POSITION_LEFT)
                } else if (mCurrPosition == POSITION_RIGHT) {
                    adapter.movePageContents(POSITION_CENTER, POSITION_LEFT)
                    adapter.movePageContents(POSITION_RIGHT, POSITION_CENTER)
                    adapter.currentIndicator = adapter.nextIndicator
                    adapter.fillPage(POSITION_RIGHT)
                }
                setCurrentItem(POSITION_CENTER, false)
            }
        })
    }

    private var mCurrPosition = POSITION_CENTER
    var listener: OnInfinitePageChangeListener? = null


    override fun setCurrentItem(item: Int) {
        if (item != POSITION_CENTER) {
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
            currentItem = POSITION_CENTER
        } else
            throw IllegalArgumentException("Adapter should be an instance of InfinitePagerAdapter.")
    }

    fun <T : Any> setCurrentIndicator(indicator: T) {
        val adapter = adapter ?: return
        val infinitePagerAdapter = adapter as InfinitePagerAdapter<*>
        val currentIndicator = infinitePagerAdapter.currentIndicator
        if (currentIndicator!!.javaClass != indicator.javaClass) {
            return
        }
        infinitePagerAdapter.reset()
        @Suppress("UNCHECKED_CAST")
        (infinitePagerAdapter as InfinitePagerAdapter<T>).currentIndicator = indicator
        for (i in 0 until PAGE_COUNT)
            infinitePagerAdapter.fillPage(i)
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
