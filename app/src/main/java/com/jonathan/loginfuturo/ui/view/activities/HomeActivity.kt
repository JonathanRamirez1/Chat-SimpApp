package com.jonathan.loginfuturo.ui.view.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.viewpager.widget.ViewPager
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseAuth
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.core.ViewedMessageHelper
import com.jonathan.loginfuturo.data.model.providers.AuthProvider
import com.jonathan.loginfuturo.data.model.providers.TokenProvider
import com.jonathan.loginfuturo.ui.view.adapters.PagerAdapter
import com.jonathan.loginfuturo.ui.view.fragments.ContainerChatFragment
import com.jonathan.loginfuturo.ui.view.fragments.ProfileFragment
import com.jonathan.loginfuturo.ui.view.fragments.RatesFragment
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity : AppCompatActivity() {

    private var previewBottomSelected : MenuItem? = null

    private lateinit var tokenProvider: TokenProvider
    private lateinit var authProvider: AuthProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        tokenProvider = TokenProvider()
        authProvider = AuthProvider()

        /** Toolbar **/
        setSupportActionBar(findViewById(R.id.toolbarView))

        setUpViewPager(getPagerAdapter())
        setUpBottomNavigationBar()
        createToken()
    }

    private fun getPagerAdapter() : PagerAdapter {
        val pagerAdapter = PagerAdapter(supportFragmentManager)
        pagerAdapter.addFragment(ProfileFragment())
        pagerAdapter.addFragment(RatesFragment())
        pagerAdapter.addFragment(ContainerChatFragment())
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
                    viewPager.currentItem = 0
                    true
                }
                R.id.bottom_nav_rates -> {
                    viewPager.currentItem = 1
                    true
                }
                R.id.bottom_nav_chat -> {
                    viewPager.currentItem = 2
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
                LoginManager.getInstance().logOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun createToken() {
        tokenProvider.create(authProvider.getUid())
    }

    override fun onStart() {
        super.onStart()
        ViewedMessageHelper.updateState(true, this)
        bottomNavigation.visibility = View.VISIBLE
    }

    override fun onPause() {
        super.onPause()
        ViewedMessageHelper.updateState(false, this)
    }
}
