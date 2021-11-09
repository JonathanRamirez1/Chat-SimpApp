package com.jonathan.chatsimpapp.ui.view.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.jonathan.chatsimpapp.databinding.ActivityFindUserBinding

class FindUserActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityFindUserBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}