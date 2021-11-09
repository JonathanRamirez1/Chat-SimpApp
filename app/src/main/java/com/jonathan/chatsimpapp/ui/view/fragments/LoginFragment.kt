package com.jonathan.chatsimpapp.ui.view.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
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
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.core.*
import com.jonathan.chatsimpapp.core.objects.Constants.REQUEST_CODE_GOOGLE_SIGN_IN
import com.jonathan.chatsimpapp.core.ext.createFactory
import com.jonathan.chatsimpapp.databinding.FragmentLoginBinding
import com.jonathan.chatsimpapp.ui.view.activities.CompleteInfoActivity
import com.jonathan.chatsimpapp.ui.view.activities.HomeActivity
import com.jonathan.chatsimpapp.ui.viewmodels.LoginViewModel

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

        animation()
        launchLoginByEmailAndPassword()
        validEmailAndPassword()
        launchRegisterFragment(view)
        launchForgotPasswordFragment(view)
        launchLoginByGoogle()
        launchLoginFacebook()
    }

    private fun animation() {
        val animationScale: Animation = AnimationUtils.loadAnimation(context, R.anim.scale)
        binding.linearLayoutLogin.startAnimation(animationScale)
    }

    private fun setObservers() {

        loginViewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            val visibility = if (loading == true) setUpProgress().show() else setUpProgress().dismiss()
            Log.d("CONSOLE", "isViewLoading $visibility ")
        }

        loginViewModel.emailValid.observe(viewLifecycleOwner) { email ->
            if (email != null) {
                binding.materialTextInputEditTextEmail.error = getString(R.string.email_is_no_valid)
            }
        }

        loginViewModel.isPasswordValid.observe(viewLifecycleOwner) { password ->
            isEnabledTogglePassword(password)
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

        binding.materialTextInputEditTextEmail.validate { email ->
            loginViewModel.validEmail(email)
        }

        binding.materialTextInputEditTextPassword.validate { password ->
            loginViewModel.validPassword(password)
        }
    }

    private fun launchRegisterFragment(view: View) {
        val registerUser = binding.textViewRegister

        navController = Navigation.findNavController(view)
        registerUser.setOnClickListener {
            navController.navigate(R.id.registerFragment)
        }
    }

    private fun isEnabledTogglePassword(password: String?) {
        binding.materialTextInputEditTextPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {
                if (editable.trim().isNotEmpty()) {
                    binding.materialTextInputLayoutPassword.isPasswordVisibilityToggleEnabled = true
                    binding.materialTextInputEditTextPassword.error = null
                } else if (password != null) {
                    binding.materialTextInputLayoutPassword.isPasswordVisibilityToggleEnabled = false
                    binding.materialTextInputEditTextPassword.error = getString(R.string.password_is_no_valid)
                }
            }
        })
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
            val email = binding.materialTextInputEditTextEmail.text.toString().trim()
            val password = binding.materialTextInputEditTextPassword.text.toString().trim()
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
        val loginGoogle = binding.buttonLoginGoogle

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