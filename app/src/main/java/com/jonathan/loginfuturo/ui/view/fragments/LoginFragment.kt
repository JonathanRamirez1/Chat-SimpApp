package com.jonathan.loginfuturo.ui.view.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.core.Constants.REQUEST_CODE_GOOGLE_SIGN_IN
import com.jonathan.loginfuturo.core.UserInformation
import com.jonathan.loginfuturo.core.ext.createFactory
import com.jonathan.loginfuturo.core.isValidEmail
import com.jonathan.loginfuturo.core.isValidPassword
import com.jonathan.loginfuturo.core.validate
import com.jonathan.loginfuturo.data.model.UserModel
import com.jonathan.loginfuturo.data.model.providers.AuthProvider
import com.jonathan.loginfuturo.data.model.providers.UserProvider
import com.jonathan.loginfuturo.databinding.FragmentLoginBinding
import com.jonathan.loginfuturo.ui.view.activities.HomeActivity
import com.jonathan.loginfuturo.ui.viewmodels.LoginViewModel
import com.jonathan.loginfuturo.ui.viewmodels.RegisterViewModel
import dmax.dialog.SpotsDialog

class LoginFragment : Fragment(), GoogleApiClient.OnConnectionFailedListener {

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var registerViewModel: RegisterViewModel
    private lateinit var navController: NavController
    private lateinit var binding: FragmentLoginBinding
    private lateinit var authProvider: AuthProvider
    private lateinit var userProvider: UserProvider
    private lateinit var alertDialog: AlertDialog
    private lateinit var userModel: UserModel

    private val callbackManager = CallbackManager.Factory.create()

    private val mGoogleApiClient: GoogleApiClient by lazy { getGoogleApiClient()!! }

    private val logInViewModel by viewModels<LoginViewModel> {
        LoginViewModel().createFactory()
    }

    /*  loginViewModel.isLogin.value = null
    loginViewModel.isLogin.observe(this, Observer {
        launchLoginByEmail()
    })

    loginViewModel.isLoginByGoogle.value = null
    loginViewModel.isLoginByGoogle.observe(this, Observer {
        val intent = Intent(context, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    })*/

    //TODO refactor
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)

        authProvider = AuthProvider()
        userProvider = UserProvider()
        userModel = UserModel()

        //loginViewModel = ViewModelProvider(this).get()
        //registerViewModel = ViewModelProvider(this).get()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObservers()
        setUpAlertDialog()
        launchLoginByEmailAndPassword()
        validEmailAndPassword()
        launchRegisterFragment(view)
        launchForgotPasswordFragment(view)
        launchLoginByGoogle()
        launchLoginFacebook()

        //mockData
        binding.editTextEmailLogin.setText("emedinaa@gmail.com")
        binding.editTextPasswordLogin.setText("Emedinaa#123456")
    }


    private fun setObservers() {
        logInViewModel.isAuthenticated.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), "Login successful !! $it", Toast.LENGTH_SHORT).show()
            it?.let { data->
                goToHomeView(data)
            }
        }

        logInViewModel.isViewLoading.observe(viewLifecycleOwner) {
            val visibility = if (it == true) View.VISIBLE else View.GONE
            Log.d("CONSOLE", "isViewLoading $visibility ")
        }

        loginViewModel.onError.observe(viewLifecycleOwner) {
            Log.d("CONSOLE", "onError $it ")
        }
    }

    //TODO to implement
    private fun goToHomeView(data:Any) {
        Log.d("CONSOLE", "goToHomeView() $data ")
    }

    private fun setUpAlertDialog() {
        alertDialog = SpotsDialog.Builder()
            .setContext(context)
            .setTheme(R.style.Custom)
            .setCancelable(false)
            .build()
    }

    private fun validEmailAndPassword() {

        binding.editTextEmailLogin.validate { email ->
            if (isValidEmail(email)) {
                binding.editTextEmailLogin.error = null
            } else {
                binding.editTextEmailLogin.error = getString(R.string.email_is_no_valid)
            }
        }

        binding.editTextPasswordLogin.validate { password ->
            if (isValidPassword(password)) {
                binding.editTextPasswordLogin.error = null
            } else {
                binding.editTextPasswordLogin.error = getString(R.string.password_is_no_valid)
            }
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

    //TODO refactor
    private fun launchLoginByEmailAndPassword() {
        val loginUser = binding.buttonLogin
        val userInformation = UserInformation()

        loginUser.setOnClickListener {
            userInformation.email = binding.editTextEmailLogin.text.toString().trim()
            userInformation.password = binding.editTextPasswordLogin.text.toString().trim()
            val email = binding.editTextEmailLogin.text.toString().trim()
            val password = binding.editTextPasswordLogin.text.toString().trim()
            loginViewModel.logIn(email,password)

            /*if (isValidEmail(userInformation.email) && isValidPassword(userInformation.password)) {
                logInByEmailAndPassword(userInformation)
            } else {
                Toast.makeText(context, "Please make sure all data is correct", Toast.LENGTH_SHORT)
                    .show()
            }*/
        }
    }

    private fun logInByEmailAndPassword(userInformation: UserInformation) {
        alertDialog.show()
        authProvider.login(userInformation.email, userInformation.password)
            .addOnCompleteListener { task ->
                alertDialog.dismiss()
                if (task.isSuccessful) {
                    if (task.isSuccessful) {
                        if (authProvider.isEmailVerified()) {
                            registerViewModel.saveUserInformation(userInformation.email)
                            val intent = Intent(context, HomeActivity::class.java)
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        } else {
                            Toast.makeText(
                                context,
                                "User must confirm email first",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Sign In Failure. Authentication failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
    }

    /** Verifica si un usuario esta registrado en Firestore, sino lo agrega a la Base de Datos **/
    private fun checkUserExist() {
        val id: String = authProvider.getUid()
        userProvider.getUser(id).addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                alertDialog.dismiss()
                val intent = Intent(context, HomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } else {
                val userModel = UserModel()
                val userInformation = UserInformation()
                userInformation.email = authProvider.getEmail()
                userModel.setId(id)
                userModel.setEmail(userInformation.email)
                userModel.setTimeStamp(userInformation.timeStamp)
                userProvider.createCollection(userModel).addOnCompleteListener { task ->
                    alertDialog.dismiss()
                    if (task.isSuccessful) {
                        val intent = Intent(context, HomeActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    } else {
                        Toast.makeText(
                            context,
                            "The information could not be saved",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun launchLoginFacebook() {
        binding.buttonLogInFacebook.setOnClickListener {
            loginByFacebookAccountIntoFirebase()
        }
    }

    private fun loginByFacebookAccountIntoFirebase() {
        LoginManager.getInstance().logInWithReadPermissions(this, listOf("email", "public_profile"))
        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult?) {
                    result?.let {
                        val token = it.accessToken
                        authProvider.facebookLogin(token).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(
                                    context,
                                    "Iniciaste Sesion con Facebook",
                                    Toast.LENGTH_SHORT
                                ).show()
                                val intent = Intent(context, HomeActivity::class.java)
                                intent.flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                            }
                        }
                    }
                }

                override fun onCancel() {
                    Toast.makeText(context, "facebook:onCancel", Toast.LENGTH_SHORT).show()
                }

                override fun onError(error: FacebookException?) {
                    Toast.makeText(context, "facebook:onError", Toast.LENGTH_SHORT).show()
                }
            })
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

    /** Inicio de sesion con google **/
    private fun loginByGoogleAccountIntoFirebase(googleAccount: GoogleSignInAccount) {
        alertDialog.show()
        authProvider.googleLogin(googleAccount).addOnCompleteListener { task ->
            if (mGoogleApiClient.isConnected) {
                Auth.GoogleSignInApi.signOut(mGoogleApiClient)
            }
            if (task.isSuccessful) {
                checkUserExist()
            } else {
                alertDialog.dismiss()
                Toast.makeText(context, "Could not login with google", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_GOOGLE_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result!!.isSuccess) {
                val account = result.signInAccount
                loginByGoogleAccountIntoFirebase(account!!)
            }
        }
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Toast.makeText(context, "Connection Failed!!", Toast.LENGTH_SHORT).show()
    }
}