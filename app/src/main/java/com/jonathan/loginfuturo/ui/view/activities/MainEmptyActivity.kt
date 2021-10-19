package com.jonathan.loginfuturo.ui.view.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.jonathan.loginfuturo.data.model.providers.AuthProvider
import com.jonathan.loginfuturo.data.model.providers.UserProvider

class MainEmptyActivity : AppCompatActivity() {

    private val authProvider = AuthProvider()
    private val userProvider = UserProvider()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setDestinyView()
    }

    private fun setDestinyView() {
        val id: String = authProvider.getUid()
        if (authProvider.isEmailVerified()) {
            userProvider.getUser(id).addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val cover: String = documentSnapshot.getString("cover").toString()
                    val phone: String = documentSnapshot.getString("phone").toString()
                    val photo: String = documentSnapshot.getString("photo").toString()
                    val username: String = documentSnapshot.getString("username").toString()
                    if (cover.isNotEmpty() && phone.isNotEmpty() && photo.isNotEmpty() && username.isNotEmpty()) {
                        val intent = Intent(this, HomeActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    } else {
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                }
            }
        } else {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        finish()
    }
}