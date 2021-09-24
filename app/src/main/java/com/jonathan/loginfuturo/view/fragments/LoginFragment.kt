package com.jonathan.loginfuturo.view.fragments

import android.app.AlertDialog
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.facebook.AccessToken
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
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.jonathan.loginfuturo.*
import com.jonathan.loginfuturo.Constants.REQUEST_CODE_GOOGLE_SIGN_IN
import com.jonathan.loginfuturo.databinding.FragmentLoginBinding
import com.jonathan.loginfuturo.model.UserModel
import com.jonathan.loginfuturo.providers.AuthProvider
import com.jonathan.loginfuturo.providers.UserProvider
import com.jonathan.loginfuturo.view.activities.HomeActivity
import com.jonathan.loginfuturo.viewmodels.LoginViewModel
import dmax.dialog.SpotsDialog

class LoginFragment : Fragment(), GoogleApiClient.OnConnectionFailedListener {

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var navController: NavController
    private lateinit var binding: FragmentLoginBinding
    private lateinit var authProvider: AuthProvider
    private lateinit var userProvider: UserProvider
    private lateinit var alertDialog: AlertDialog
    private lateinit var userModel: UserModel

    private val callbackManager = CallbackManager.Factory.create()
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val loginManager : LoginManager by lazy {  LoginManager.getInstance() }

    private val mGoogleApiClient: GoogleApiClient by lazy { getGoogleApiClient()!! }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loginViewModel = activity?.run {
            ViewModelProviders.of(this)[LoginViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        authProvider = AuthProvider()
        userProvider = UserProvider()
        userModel = UserModel()
        setUpAlertDialog()


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
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding  = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)
        binding.loginViewModel = LoginViewModel(application = Application())


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        launchLoginByEmailAndPassword()
        validEmailAndPassword()
        launchRegisterFragment(view)
        launchForgotPasswordFragment(view)
        launchLoginByGoogle()
        loginByFacebookAccountIntoFirebase()
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

    private fun launchLoginByEmailAndPassword() {
        val loginUser = binding.buttonLogin
        val userInformation = UserInformation()

        loginUser.setOnClickListener {
            userInformation.email = binding.editTextEmailLogin.text.toString()
            userInformation.password = binding.editTextPasswordLogin.text.toString()

            if (isValidEmail(userInformation.email) && isValidPassword(userInformation.password)) {
                    logInByEmailAndPassword(userInformation)
            } else {
                Toast.makeText(context, "Please make sure all data is correct", Toast.LENGTH_SHORT).show()
            }
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
                            val intent = Intent(context, HomeActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        } else {
                            Toast.makeText(context, "User must confirm email first", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Sign In Failure. Authentication failed", Toast.LENGTH_SHORT).show()
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
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    } else {
                        Toast.makeText(context, "The information could not be saved", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun handlerFacebookAccessToken(token : AccessToken) {
        binding.buttonLogInFacebook.setOnClickListener {
            val credential = FacebookAuthProvider.getCredential(token.token)
            firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val intent = Intent(context, HomeActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                }
        }
    }

    private fun loginByFacebookAccountIntoFirebase() {
        loginManager.logInWithReadPermissions(this, listOf("email", "public_profile"))
        loginManager.registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult?) {
                    result?.let {
                        handlerFacebookAccessToken(result.accessToken)
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

    private fun getGoogleApiClient() : GoogleApiClient? {
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