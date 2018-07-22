package com.jonaswanke.calendar.example

import android.content.Intent
import android.databinding.DataBindingUtil
import android.net.Uri
import android.os.Bundle
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.text.format.DateUtils
import com.jonaswanke.calendar.Event
import com.jonaswanke.calendar.example.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {
    private var nextId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        binding.activity = this
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        val random = Random()
        calendar.eventRequestCallback = { week ->
            val events = mutableListOf<Event>()
            for (i in 0..15) {
                val id = nextId++
                val start = week.start + Math.abs(random.nextLong()) % DateUtils.WEEK_IN_MILLIS
                events.add(Event(
                        id.toString(),
                        id.toString(),
                        id.toString(),
                        random.nextInt(),
                        start,
                        start + Math.abs(random.nextLong()) % (DateUtils.DAY_IN_MILLIS / 8)))
            }
            calendar.setEventsForWeek(week, events)
        }
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START))
            drawer_layout.closeDrawer(GravityCompat.START)
        else
            super.onBackPressed()
    }


    fun openHomepage() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://jonas-wanke.com")))
    }
}
