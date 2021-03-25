package com.jonathan.loginfuturo.view.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.firebase.auth.FirebaseAuth
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.databinding.FragmentForgotPasswordBinding
import com.jonathan.loginfuturo.isValidEmail
import com.jonathan.loginfuturo.validate
import kotlinx.android.synthetic.main.fragment_forgot_password.*


class ForgotPasswordFragment : Fragment() {

    private lateinit var binding: FragmentForgotPasswordBinding
    private lateinit var navController: NavController

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater,
            R.layout.fragment_forgot_password, container, false)

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        validResetPassword()
        launchLoginFragmentFromForgotPassword(view)
        resetPassword(view)
    }

    private fun validResetPassword() {
        binding.editTextEmail.validate {
            if (isValidEmail(it)) {
                binding.editTextEmail.error = null
            } else {
                binding.editTextEmail.error = getString(R.string.login_forgot_password)
            }
        }
    }

    private fun launchLoginFragmentFromForgotPassword(view: View) {
        val goLogin = binding.buttonGoLogInForgot

        navController = Navigation.findNavController(view)
        goLogin.setOnClickListener {

            navController.navigate(R.id.loginFragment)
            //TODO IMPLEMENTAR TRANSACCION Y FLAG intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun resetPassword(view: View) {
        val resetPassword = binding.buttonForgotPassword

        resetPassword.setOnClickListener {
            val email = editTextEmail.text.toString()
            if (isValidEmail(email)) {
                firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener {
                    Toast.makeText(context, "Email has been sent to reset your password", Toast.LENGTH_SHORT).show()
                    navController = Navigation.findNavController(view)
                    navController.navigate(R.id.loginFragment)
                    //TODO IMPLEMENTAR TRANSACCION Y FLAG intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    //overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
            } else {
                Toast.makeText(context, "Please make sure the email is correct", Toast.LENGTH_SHORT).show()
            }
        }
    }
}