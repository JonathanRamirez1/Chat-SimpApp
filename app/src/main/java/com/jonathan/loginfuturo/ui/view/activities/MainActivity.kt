package com.jonathan.loginfuturo.ui.view.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.jonathan.loginfuturo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}