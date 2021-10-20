package com.jonathan.loginfuturo.ui.view.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.jonathan.loginfuturo.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}