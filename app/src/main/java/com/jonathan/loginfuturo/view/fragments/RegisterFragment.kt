package com.jonathan.loginfuturo.view.fragments

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.firebase.auth.FirebaseAuth
import com.jonathan.loginfuturo.*
import com.jonathan.loginfuturo.databinding.FragmentRegisterBinding
import com.jonathan.loginfuturo.viewmodels.RegisterViewModel
import kotlinx.android.synthetic.main.fragment_register.*


class RegisterFragment : Fragment() {

    private lateinit var registerViewModel: RegisterViewModel
    private lateinit var navController: NavController

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    //DataBinding
    private lateinit var binding: FragmentRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerViewModel = activity?.run {
            ViewModelProviders.of(this)[RegisterViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        registerViewModel.isRegister.value = null
        registerViewModel.isRegister.observe(this, Observer {
            view?.let { it1 -> validateFieldsRegisterFragment(it1) }
        })

        registerViewModel.launchFragment.observe(this, Observer { it ->
            it.getContentIfNotHandled()?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        })
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_register, container, false)
        binding.registerViewModel = RegisterViewModel(application = Application())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        validEmailAndPasswordAndConfirmPassword()
        launchLoginFragment(view)
    }

    private fun validEmailAndPasswordAndConfirmPassword() {

        binding.editTextEmailSignUp.validate {
            if (isValidEmail(it)) {
                binding.editTextEmailSignUp.error = null
            } else {
                binding.editTextEmailSignUp.error = getString(R.string.email_is_no_valid)
            }
        }

        binding.editTextPasswordSignUp.validate {
            if (isValidPassword(it)) {
                binding.editTextPasswordSignUp.error = null
            } else {
                binding.editTextPasswordSignUp.error = getString(R.string.password_is_no_valid)
            }
        }

        binding.editTextConfirmPassword.validate {
            if (isValidConfirmPassword((editTextPasswordSignUp.text.toString()), it)) {
                binding.editTextConfirmPassword.error = null
            } else {
                binding.editTextConfirmPassword.error =
                    getString(R.string.confirm_password_is_no_valid)
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

    private fun validateFieldsRegisterFragment(view: View) {
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

/* registerViewModel.commandData.observe(this, Observer { it ->
           when (it) {
               is BaseCommand.Success -> {
                   val snackBar =
                       view?.let { it1 -> Snackbar.make(it1, it.toastMessage.toString(), Snackbar.LENGTH_INDEFINITE) }
                   snackBar?.setAction("Okay") {
                       snackBar.dismiss()
                   }
                   snackBar?.show()
               }
               is BaseCommand.Error -> {
                    val snackBar =
                        view?.let { it1 -> Snackbar.make(it1, it.errorString, Snackbar.LENGTH_INDEFINITE) }
                   snackBar?.setAction("Okay") {
                       snackBar.dismiss()
                   }
                   snackBar?.show()
               }
           }
       })*/

