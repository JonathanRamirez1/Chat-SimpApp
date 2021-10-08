package com.jonathan.loginfuturo.ui.view.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.viewpager.widget.ViewPager
import com.facebook.login.LoginManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.auth.FirebaseAuth
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.core.ViewedMessageHelper
import com.jonathan.loginfuturo.data.model.providers.AuthProvider
import com.jonathan.loginfuturo.data.model.providers.TokenProvider
import com.jonathan.loginfuturo.databinding.ActivityHomeBinding
import com.jonathan.loginfuturo.ui.view.adapters.PagerAdapter
import com.jonathan.loginfuturo.ui.view.fragments.ContainerChatFragment
import com.jonathan.loginfuturo.ui.view.fragments.ProfileFragment
import com.jonathan.loginfuturo.ui.view.fragments.RatesFragment

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var tokenProvider: TokenProvider
    private lateinit var authProvider: AuthProvider

    private var previewBottomSelected: MenuItem? = null
    private var interstitial: InterstitialAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenProvider = TokenProvider()
        authProvider = AuthProvider()

        /** Toolbar **/
        setSupportActionBar(findViewById(R.id.toolbarView))

        setUpViewPager(getPagerAdapter())
        setUpBottomNavigationBar()
        createToken()
        initInterstitialAd()
        initListeners()
    }

    private fun getPagerAdapter() : PagerAdapter {
        val pagerAdapter = PagerAdapter(supportFragmentManager)
        pagerAdapter.addFragment(ProfileFragment())
        pagerAdapter.addFragment(RatesFragment())
        pagerAdapter.addFragment(ContainerChatFragment())
        return pagerAdapter
    }

    private fun setUpViewPager(pagerAdapter: PagerAdapter) {
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = pagerAdapter.count
        binding.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {

                if (previewBottomSelected == null) {
                    binding.bottomNavigation.menu.getItem(0).isChecked = false
                } else {
                    previewBottomSelected!!.isChecked = false
                }
                binding.bottomNavigation.menu.getItem(position).isChecked = true
                previewBottomSelected = binding.bottomNavigation.menu.getItem(position)
            }
        })
    }

    private fun setUpBottomNavigationBar() {

        binding.bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.bottom_nav_info -> {
                    binding.viewPager.currentItem = 0
                    true
                }
                R.id.bottom_nav_rates -> {
                    binding.viewPager.currentItem = 1
                    true
                }
                R.id.bottom_nav_chat -> {
                    binding.viewPager.currentItem = 2
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
                showAds()
                initInterstitialAd()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun createToken() {
        tokenProvider.create(authProvider.getUid())
    }

    private fun initListeners() {
        interstitial?.fullScreenContentCallback = object : FullScreenContentCallback() {

            override fun onAdDismissedFullScreenContent() {}
            override fun onAdFailedToShowFullScreenContent(adError: AdError?) {}
            override fun onAdShowedFullScreenContent() { interstitial = null }
        }
    }

    //TODO CAMBIAR LOS IDS DE ADS(ANUNCIOS) EN EL XML Y EL MANIFEST CUANDO SE TERMINE LA APP, LOS ACTUALES SON DE PRUEBA (Ver strings o video de ari)
    private fun initInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, getString(R.string.test_interstitial), adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) { interstitial = interstitialAd }
            override fun onAdFailedToLoad(p0: LoadAdError) { interstitial = null } })
    }

    private fun showAds() {
        interstitial?.show(this)
    }

    override fun onStart() {
        super.onStart()
        ViewedMessageHelper.updateState(true, this)
        binding.bottomNavigation.visibility = View.VISIBLE
    }

    override fun onPause() {
        super.onPause()
        ViewedMessageHelper.updateState(false, this)
    }
}
