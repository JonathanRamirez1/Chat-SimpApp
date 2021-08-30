package com.jonathan.loginfuturo.view.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.databinding.FragmentCompleteInfoBinding
import com.jonathan.loginfuturo.isValidUsername
import com.jonathan.loginfuturo.model.UserModel
import com.jonathan.loginfuturo.providers.AuthProvider
import com.jonathan.loginfuturo.providers.UserProvider
import com.jonathan.loginfuturo.view.activities.HomeActivity

class CompleteInfoFragment : Fragment() {

    private lateinit var binding: FragmentCompleteInfoBinding
    private lateinit var navController: NavController
    private lateinit var userModel: UserModel
    private lateinit var userProvider: UserProvider
    private lateinit var authProvider: AuthProvider


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding =  DataBindingUtil.inflate(inflater, R.layout.fragment_complete_info, container, false)

        userModel = UserModel()
        userProvider = UserProvider()
        authProvider = AuthProvider()
        return binding.root
    }

    private fun validateFields() {
        val username: String = binding.materialEditTextCompleteUsername.text.toString()
        if (!username.isEmpty()) {
            if (isValidUsername(username)) {
                completeInfo()
            }
        } else {
            Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun completeInfo() {
        val id: String = authProvider.getUid()
        userModel.setId(id)
        userProvider.CompleteUserInfo(userModel).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                launchLoginByComplete()
            }
        }
    }

    private fun launchLoginByComplete() {
        val goLogin = binding.buttonCompleteOk
        goLogin.setOnClickListener {
            val intent = Intent(context, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

    }

}