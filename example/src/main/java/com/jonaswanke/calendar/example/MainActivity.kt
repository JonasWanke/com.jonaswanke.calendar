package com.jonaswanke.calendar.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import com.jonaswanke.calendar.BaseEvent
import com.jonaswanke.calendar.utils.Day
import com.jonaswanke.calendar.Event
import com.jonaswanke.calendar.utils.Week
import com.jonaswanke.calendar.example.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    private var nextId: Long = 0
    private val random = Random()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        binding.activity = this
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.main_drawer_open, R.string.main_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        calendar.eventRequestCallback = {
            populate(it)
        }
        calendar.onEventClickListener = {
            Toast.makeText(this, it.title + " clicked", Toast.LENGTH_SHORT).show()
        }
        calendar.onEventLongClickListener = {
            Toast.makeText(this, it.title + " long clicked", Toast.LENGTH_SHORT).show()
        }
        calendar.onAddEventListener = {
            val start = DateUtils.formatDateTime(this, it.start, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME)
            Toast.makeText(this, "Add event at $start", Toast.LENGTH_SHORT).show()
            true
        }
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START))
            drawer_layout.closeDrawer(GravityCompat.START)
        else
            super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.main_action_today -> calendar.visibleStart = Day()
            R.id.main_action_regenerate -> calendar.cachedWeeks.forEach { populate(it, true) }
            else ->
                return super.onOptionsItemSelected(item)
        }
        return true
    }

    @Suppress("MagicNumber")
    private fun populate(week: Week, force: Boolean = false) {
        if (calendar.cachedEvents.contains(week))
            return

        val events = mutableListOf<Event>()
        for (i in 0..15) {
            val id = nextId++.toString()
            val start = week.start + abs(random.nextLong()) % DateUtils.WEEK_IN_MILLIS
            events.add(BaseEvent(
                    id,
                    id,
                    (random.nextInt() or 0xFF000000.toInt()) and 0x00202020.inv(),
                    start,
                    start + abs(random.nextLong()) % (DateUtils.DAY_IN_MILLIS / 8)))
        }
        for (i in 0..3) {
            val id = nextId++.toString()
            val start = week.start + abs(random.nextLong()) % DateUtils.WEEK_IN_MILLIS
            events.add(BaseEvent(
                    id,
                    id,
                    (random.nextInt() or 0xFF000000.toInt()) and 0x00202020.inv(),
                    start,
                    start + abs(random.nextLong()) % (DateUtils.DAY_IN_MILLIS * 7),
                    true))
        }
        calendar.setEventsForWeek(week, events)
    }

    fun openHomepage() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://jonas-wanke.com")))
    }
}
