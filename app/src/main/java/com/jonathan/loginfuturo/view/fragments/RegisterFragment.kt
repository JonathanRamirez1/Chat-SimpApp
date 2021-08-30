package com.jonathan.loginfuturo.view.fragments

import android.app.AlertDialog
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.jonathan.loginfuturo.*
import com.jonathan.loginfuturo.databinding.FragmentRegisterBinding
import com.jonathan.loginfuturo.model.UserModel
import com.jonathan.loginfuturo.providers.AuthProvider
import com.jonathan.loginfuturo.providers.UserProvider
import com.jonathan.loginfuturo.viewmodels.RegisterViewModel
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.fragment_register.*


class RegisterFragment : Fragment() {

    private lateinit var binding: FragmentRegisterBinding
    private lateinit var registerViewModel: RegisterViewModel
    private lateinit var navController: NavController
    private lateinit var authProvider: AuthProvider
    private lateinit var userProvider: UserProvider
    private lateinit var userModel: UserModel
    private lateinit var userInformation: UserInformation
    private lateinit var alertDialog: AlertDialog

    /* private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firebaseFirestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerViewModel = activity?.run {
            ViewModelProviders.of(this)[RegisterViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        authProvider = AuthProvider()
        userProvider = UserProvider()
        userModel = UserModel()
        userInformation = UserInformation()
        setUpAlertDialog()

        /*  registerViewModel.isRegister.value = null
        registerViewModel.isRegister.observe(this, Observer {
            view?.let { it1 -> validateFieldsRegisterFragment(it1) }
        })

        registerViewModel.launchFragment.observe(this, Observer { it ->
            it.getContentIfNotHandled()?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        })*/
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_register, container, false)
        binding.registerViewModel = RegisterViewModel(application = Application())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        validEmailAndPasswordAndConfirmPassword()
        validateFields(view)
        launchLoginFragment(view)
    }

    private fun setUpAlertDialog() {
        alertDialog = SpotsDialog.Builder()
            .setContext(context)
            .setTheme(R.style.Custom)
            .setCancelable(false)
            .build()
    }

    private fun validEmailAndPasswordAndConfirmPassword() {

        binding.editTextUsernameSignUp.validate { username ->
            if (isValidUsername(username)) {
                binding.editTextUsernameSignUp.error = null
            } else {
                binding.editTextUsernameSignUp.error = getString(R.string.username_is_no_valid)
            }
        }

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
            if (isValidConfirmPassword((editTextPasswordSignUp.text.toString()), confirmPassword)) {
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

    /**Valida si los campos son correctos de acuerdo a los expresiones regulares (Ver Extensions.kt)**/
    private fun validateFields(view: View) {
        val signUpUser = binding.buttonSignUp

        signUpUser.setOnClickListener {
            userInformation.username = binding.editTextUsernameSignUp.text.toString()
            userInformation.email = binding.editTextEmailSignUp.text.toString()
            userInformation.password = binding.editTextPasswordSignUp.text.toString()
            userInformation.confirmPassword = binding.editTextConfirmPassword.text.toString()

            if (isValidUsername(userInformation.username) && isValidEmail(userInformation.email) && isValidPassword(
                    userInformation.password
                )
                && isValidConfirmPassword(userInformation.password, userInformation.confirmPassword)
            ) {
                signUpByEmail()
                navController = Navigation.findNavController(view)
                navController.navigate(R.id.loginFragment)
            } else {
                Toast.makeText(context, "Please make sure all data is correct", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun signUpByEmail() {
        alertDialog.show()
        authProvider.register(userInformation.email, userInformation.password)
            .addOnCompleteListener { task ->
                alertDialog.dismiss()
                if (task.isSuccessful) {
                    saveUserInformation()
                    authProvider.sendEmailVerification().addOnCompleteListener {
                        Toast.makeText(context, "An email has been sent to you. Confirm before logging in.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    alertDialog.dismiss()
                    Toast.makeText(context, "Unexpected error occurred. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    /**Guarda los datos de usuarios registrados, en la Base de Datos de Firestore**/
    private fun saveUserInformation() {
        val id: String = authProvider.getUid()
        userModel.setEmail(userInformation.email)
        userModel.setId(id)
        userModel.setTimeStamp(userInformation.timeStamp)
        userModel.setUsername(userInformation.username)

        userProvider.createCollection(userModel).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "User successfully saved", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Unexpected Error", Toast.LENGTH_SHORT).show()
            }
        }
    }
}


