package com.jonathan.loginfuturo.ui.view.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.core.*
import com.jonathan.loginfuturo.core.Constants.REQUEST_CODE_GOOGLE_SIGN_IN
import com.jonathan.loginfuturo.core.ext.createFactory
import com.jonathan.loginfuturo.databinding.FragmentLoginBinding
import com.jonathan.loginfuturo.ui.view.activities.CompleteInfoActivity
import com.jonathan.loginfuturo.ui.view.activities.HomeActivity
import com.jonathan.loginfuturo.ui.viewmodels.LoginViewModel

class LoginFragment : Fragment(), GoogleApiClient.OnConnectionFailedListener {

    private lateinit var binding: FragmentLoginBinding
    private lateinit var navController: NavController

    private val mGoogleApiClient: GoogleApiClient by lazy { getGoogleApiClient()!! }

    private val loginViewModel by viewModels<LoginViewModel> {
        LoginViewModel().createFactory()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObservers()

        launchLoginByEmailAndPassword()
        validEmailAndPassword()
        launchRegisterFragment(view)
        launchForgotPasswordFragment(view)
        launchLoginByGoogle()
        launchLoginFacebook()
    }

    private fun setObservers() {

        loginViewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            val visibility = if (loading == true) setUpProgress().show() else setUpProgress().dismiss()
            Log.d("CONSOLE", "isViewLoading $visibility ")
        }

        loginViewModel.emailValid.observe(viewLifecycleOwner) { email ->
            if (email != null) {
                binding.editTextEmailLogin.error = getString(R.string.email_is_no_valid)
            }
        }

        loginViewModel.isPasswordValid.observe(viewLifecycleOwner) { password ->
            if (password != null) {
                binding.editTextPasswordLogin.error = getString(R.string.password_is_no_valid)
            }
        }

        loginViewModel.isDestinyView.observe(viewLifecycleOwner) { home ->
            if (home == true) {
                goToHomeView()
            } else if (home == false) {
                goToCompleteInfoView()
            }
        }

        loginViewModel.showToast.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            Log.d("CONSOLE", "onError $it ")
        }

        loginViewModel.isLoginFacebook.observe(viewLifecycleOwner) { loginFacebook ->
            if (loginFacebook == true) {
                goToHomeView()
            }
        }
    }

    private fun goToHomeView() {
        val intent = Intent(context, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun goToCompleteInfoView() {
        val intent = Intent(context, CompleteInfoActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun validEmailAndPassword() {

        binding.editTextEmailLogin.validate { email ->
            loginViewModel.validEmail(email)
        }

        binding.editTextPasswordLogin.validate { password ->
            loginViewModel.validPassword(password)
        }
    }

    private fun launchRegisterFragment(view: View) {
        val registerUser = binding.buttonSignUpLogin

        navController = Navigation.findNavController(view)
        registerUser.setOnClickListener {
            navController.navigate(R.id.registerFragment)
        }
    }

    private fun launchForgotPasswordFragment(view: View) {
        val forgotPassword = binding.textViewForgotPassword

        navController = Navigation.findNavController(view)
        forgotPassword.setOnClickListener {
            navController.navigate(R.id.forgotPasswordFragment)
        }
    }

    private fun launchLoginByEmailAndPassword() {
        val loginUser = binding.buttonLogin

        loginUser.setOnClickListener {
            val email = binding.editTextEmailLogin.text.toString().trim()
            val password = binding.editTextPasswordLogin.text.toString().trim()
            loginViewModel.onLoginEmailAndPassword(email, password)
        }
    }

    private fun launchLoginFacebook() {
        binding.buttonLogInFacebook.setOnClickListener {
            loginByFacebookAccountIntoFirebase()
        }
    }

    private fun loginByFacebookAccountIntoFirebase() {
        LoginManager.getInstance().logInWithReadPermissions(this, listOf("email", "public_profile"))
        loginViewModel.onLoginFacebook()
    }

    private fun launchLoginByGoogle() {
        val loginGoogle = binding.buttonLogInGoogle

        loginGoogle.setOnClickListener {
            val intent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient)
            startActivityForResult(intent, REQUEST_CODE_GOOGLE_SIGN_IN)
        }
    }

    private fun getGoogleApiClient(): GoogleApiClient? {
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        return context?.let {
            GoogleApiClient.Builder(it)
                .enableAutoManage(FragmentActivity(), this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, googleSignInOptions)
                .build()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        loginViewModel.onActivityResultForFacebook(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
        loginViewModel.onActivityResultForGoogle(data, requestCode)
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Toast.makeText(context,"An error occurred: Check your internet connection", Toast.LENGTH_SHORT).show()
    }
}