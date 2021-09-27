package com.jonathan.loginfuturo.ui.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.jonathan.loginfuturo.*
import com.jonathan.loginfuturo.core.*
import com.jonathan.loginfuturo.databinding.FragmentRegisterBinding
import com.jonathan.loginfuturo.ui.viewmodels.RegisterViewModel
import kotlinx.android.synthetic.main.fragment_register.*


class RegisterFragment : Fragment() {

    private lateinit var binding: FragmentRegisterBinding
    private lateinit var registerViewModel: RegisterViewModel
    private lateinit var navController: NavController

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_register, container, false)
        registerViewModel = ViewModelProvider(this).get()

        registerViewModel.isLoading.observe(viewLifecycleOwner)  { loading ->
            binding.linearLayoutRegister.visibility = if (loading == true) View.VISIBLE else View.GONE
           /* if (progress) {
                setUpProgress().show()
            } else {
                setUpProgress().dismiss()
            }*/
        }

        registerViewModel.showToast.observe(viewLifecycleOwner, Observer { event ->  //TODO HACER QUE FUNCIONE LOS TOAST
            event.getContentIfNotHandled()?.let { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        })
            return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        validFields()
        validateFields(view)
        launchLoginFragment(view)
    }

    private fun validFields() {

        binding.editTextEmailSignUp.validate { email ->
            if (isValidEmail(email)) {
                binding.editTextEmailSignUp.error = null
            } else {
                binding.editTextEmailSignUp.error = getString(R.string.email_is_no_valid)
            }
        }

        binding.editTextPasswordSignUp.validate { password ->
            if (isValidPassword(password)) {
                binding.editTextPasswordSignUp.error = null
            } else {
                binding.editTextPasswordSignUp.error = getString(R.string.password_is_no_valid)
            }
        }

        binding.editTextConfirmPassword.validate { confirmPassword ->
            if (isValidConfirmPassword((binding.editTextPasswordSignUp.text.toString()), confirmPassword)) {
                binding.editTextConfirmPassword.error = null
            } else {
                binding.editTextConfirmPassword.error = getString(R.string.confirm_password_is_no_valid)
            }
        }
    }

    private fun launchLoginFragment(view: View) {
        val goLogin = binding.buttonRegisterGoLogIn

        navController = Navigation.findNavController(view)
        goLogin.setOnClickListener {
            navController.navigate(R.id.loginFragment)
        }
    }

    /**Valida si los campos son correctos de acuerdo a los expresiones regulares (Ver Extensions.kt)**/
    private fun validateFields(view: View) {
        val signUpUser = binding.buttonSignUp

        signUpUser.setOnClickListener {
            val email = binding.editTextEmailSignUp.text.toString()
            val password = binding.editTextPasswordSignUp.text.toString()
            val confirmPassword = binding.editTextConfirmPassword.text.toString()

            if (isValidEmail(email) && isValidPassword(password) && isValidConfirmPassword(password, confirmPassword)) {
                registerViewModel.signUpByEmail(email, password)
                navController = Navigation.findNavController(view)
                navController.navigate(R.id.loginFragment)
            } else {
                Toast.makeText(context, "Please make sure all data is correct", Toast.LENGTH_SHORT).show()
            }
        }
    }
}


