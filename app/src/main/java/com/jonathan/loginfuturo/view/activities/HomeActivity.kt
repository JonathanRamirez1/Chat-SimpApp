package com.jonathan.loginfuturo.view.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.viewpager.widget.ViewPager
import com.google.firebase.auth.FirebaseAuth
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.view.adapters.PagerAdapter
import com.jonathan.loginfuturo.view.fragments.InformationFragment
import com.jonathan.loginfuturo.view.fragments.RatesFragment
import com.jonathan.loginfuturo.view.fragments.RoomsFragment
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_main.*

class HomeActivity : AppCompatActivity() {

    private var previewBottomSelected : MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        /** Toolbar **/
        setSupportActionBar(findViewById(R.id.toolbarView))

        setUpViewPager(getPagerAdapter())
        setUpBottomNavigationBar()
    }

    private fun getPagerAdapter() : PagerAdapter {
        val pagerAdapter = PagerAdapter(supportFragmentManager)
        pagerAdapter.addFragment(InformationFragment())
        pagerAdapter.addFragment(RatesFragment())
        pagerAdapter.addFragment(RoomsFragment())
        return pagerAdapter
    }

    private fun setUpViewPager(pagerAdapter: PagerAdapter) {
        viewPager.adapter = pagerAdapter
        viewPager.offscreenPageLimit = pagerAdapter.count
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {

                if (previewBottomSelected == null) {
                    bottomNavigation.menu.getItem(0).isChecked = false
                } else {
                    previewBottomSelected!!.isChecked = false
                }
                bottomNavigation.menu.getItem(position).isChecked = true
                previewBottomSelected = bottomNavigation.menu.getItem(position)
            }
        })
    }

    private fun setUpBottomNavigationBar() {

        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.bottom_nav_info -> {
                    viewPager.currentItem = 0;
                    true
                }
                R.id.bottom_nav_rates -> {
                    viewPager.currentItem = 1;
                    true
                }
                R.id.bottom_nav_chat -> {
                    viewPager.currentItem = 2;
                    true
                }
                else ->
                    false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.general_options_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menuLogOut -> {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }
}