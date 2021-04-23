package com.jonathan.loginfuturo.view.fragments

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
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.jonathan.loginfuturo.*
import com.jonathan.loginfuturo.Constants.REQUEST_CODE_GOOGLE_SIGN_IN
import com.jonathan.loginfuturo.Utils.RxBus
import com.jonathan.loginfuturo.databinding.FragmentLoginBinding
import com.jonathan.loginfuturo.view.activities.HomeActivity
import com.jonathan.loginfuturo.viewmodels.LoginViewModel
import io.reactivex.disposables.Disposable
import java.util.*
import kotlin.collections.HashMap

class LoginFragment : Fragment(), GoogleApiClient.OnConnectionFailedListener {

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var navController: NavController

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val mGoogleApiClient : GoogleApiClient by lazy { getGoogleApiClient()!! }


    //DataBinding
    private lateinit var binding: FragmentLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loginViewModel = activity?.run {
            ViewModelProviders.of(this)[LoginViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

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

       /* loginViewModel.message.observe(viewLifecycleOwner, Observer { it ->
            it.getContentIfNotHandled()?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        })*/
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        validEmailAndPassword()
        launchRegisterFragment(view)
        launchForgotPasswordFragment(view)
        launchLoginByGoogle()
        launchLoginByEmail()
    }

    private fun validEmailAndPassword() {

        binding.editTextEmailLogin.validate {
            if (isValidEmail(it)) {
                binding.editTextEmailLogin.error = null
            } else {
                binding.editTextEmailLogin.error = getString(R.string.email_is_no_valid)
            }
        }

        binding.editTextPasswordLogin.validate {
            if (isValidPassword(it)) {
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

    private fun launchLoginByEmail() {
        val loginUser = binding.buttonLogin
        loginUser.setOnClickListener {
            val email = binding.editTextEmailLogin.text.toString()
            val password = binding.editTextPasswordLogin.text.toString()

            if (isValidEmail(email) && isValidPassword(password)) {
                    logInByEmail(email, password)
            } else {
                Toast.makeText(context, "Please make sure all data is correct", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun logInByEmail(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (task.isSuccessful) {
                        if (firebaseAuth.currentUser!!.isEmailVerified) {
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
    private fun loginByGoogleAccountIntoFirebase(googleAccount : GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(googleAccount.idToken, null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener {
            if (mGoogleApiClient.isConnected) {
                Auth.GoogleSignInApi.signOut(mGoogleApiClient)
            }
            val intent = Intent(context, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
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
