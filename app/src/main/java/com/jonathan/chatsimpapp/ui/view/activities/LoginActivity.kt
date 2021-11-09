package com.jonathan.chatsimpapp.ui.view.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.jonathan.chatsimpapp.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}