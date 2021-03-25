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
import com.jonathan.loginfuturo.view.fragments.ChatFragment
import com.jonathan.loginfuturo.view.fragments.InformationFragment
import com.jonathan.loginfuturo.view.fragments.RatesFragment
import com.jonathan.loginfuturo.view.fragments.RoomsFragment
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


    }
}