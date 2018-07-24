package com.jonaswanke.calendar.example

import android.content.Intent
import android.databinding.DataBindingUtil
import android.net.Uri
import android.os.Bundle
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
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
                this, drawer_layout, toolbar, R.string.main_drawer_open, R.string.main_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        val random = Random()
        calendar.eventRequestCallback = { week ->
            val events = mutableListOf<Event>()
            for (i in 0..63) {
                val id = nextId++.toString()
                val start = week.start + Math.abs(random.nextLong()) % DateUtils.WEEK_IN_MILLIS
                events.add(Event(
                        id,
                        id,
                        random.nextInt(),
                        start,
                        start + Math.abs(random.nextLong()) % (DateUtils.DAY_IN_MILLIS / 8)))
            }
            calendar.setEventsForWeek(week, events)
        }
        calendar.onEventClickListener = {
            Toast.makeText(this, it.title + " clicked", Toast.LENGTH_LONG).show()
        }
        calendar.onEventLongClickListener = {
            Toast.makeText(this, it.title + " long clicked", Toast.LENGTH_LONG).show()
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
            R.id.main_action_today -> calendar.jumpToToday()
            else ->
                return super.onOptionsItemSelected(item)
        }
        return true
    }


    fun openHomepage() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://jonas-wanke.com")))
    }
}
