package com.jonathan.chatsimpapp.ui.view.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.activity.viewModels
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.core.ext.createFactory
import com.jonathan.chatsimpapp.databinding.ActivityMainBinding
import com.jonathan.chatsimpapp.ui.viewmodels.MainViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val mainViewModel by viewModels<MainViewModel> {
        MainViewModel().createFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        animation()

        setObservers()

        Handler().postDelayed(Runnable {
            mainViewModel.setDestinyView()
        }, 3500)
    }

    private fun animation() {
        binding.lottieAnimationViewSplash.animate().translationY(-1500F).setDuration(1000).startDelay = 2000
        binding.textViewChatSimpApp.animate().translationYBy(1000F).setDuration(1000).startDelay = 2500

        val animationSlideUp: Animation = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        binding.textViewChatSimpApp.animation = animationSlideUp

        val animationSlideDown: Animation = AnimationUtils.loadAnimation(this, R.anim.slide_down)
        binding.linearLayoutSplash.animation = animationSlideDown
        binding.linearLayoutSplash.animate().translationY(1000F).setDuration(1000).startDelay = 2500
    }

    private fun setObservers() {
        mainViewModel.isLoginView.observe(this) { loginView ->
            if (loginView == true) {
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } else if (loginView == false) {
                val intent = Intent(this, HomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }
}