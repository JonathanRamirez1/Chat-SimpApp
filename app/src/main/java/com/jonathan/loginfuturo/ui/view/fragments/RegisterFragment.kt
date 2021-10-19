package com.jonathan.loginfuturo.ui.view.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.jonathan.loginfuturo.*
import com.jonathan.loginfuturo.core.*
import com.jonathan.loginfuturo.core.ext.createFactory
import com.jonathan.loginfuturo.databinding.FragmentRegisterBinding
import com.jonathan.loginfuturo.ui.viewmodels.RegisterViewModel


class RegisterFragment : Fragment() {

    private lateinit var binding: FragmentRegisterBinding
    private lateinit var navController: NavController

    private val registerViewModel by viewModels<RegisterViewModel> {
        RegisterViewModel().createFactory()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObservers()

        validFields()
        validateFields()
        launchLoginFragment(view)
        loadAds()
    }

    private fun setObservers() {

        registerViewModel.isEmailValid.observe(viewLifecycleOwner) { email ->
            if (email  != null) {
                binding.editTextEmailSignUp.error = getString(R.string.email_is_no_valid)
            }
        }

        registerViewModel.isPasswordValid.observe(viewLifecycleOwner) { password ->
            if (password != null) {
                binding.editTextPasswordSignUp.error = getString(R.string.password_is_no_valid)
            }
        }

        registerViewModel.isConfirmPasswordValid.observe(viewLifecycleOwner) { confirmPassword ->
            if (confirmPassword != null) {
                binding.editTextConfirmPassword.error = getString(R.string.confirm_password_is_no_valid)
            }
        }

        registerViewModel.showToast.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            Log.d("CONSOLE", "onError $it ")
        }

        registerViewModel.isRegister.observe(viewLifecycleOwner) { register ->
            if (register == true) {
                view?.let { goToLoginView(it) }
            }
        }
    }

    private fun goToLoginView(view: View) {
        navController = Navigation.findNavController(view)
        navController.navigate(R.id.loginFragment)
    }

    private fun validFields() {

        binding.editTextEmailSignUp.validate { email ->
            registerViewModel.validEmail(email)
        }

        binding.editTextPasswordSignUp.validate { password ->
            registerViewModel.validPassword(password)
        }

        binding.editTextConfirmPassword.validate { confirmPassword ->
            val password = binding.editTextPasswordSignUp.text.toString()
            registerViewModel.validConfirmPassword(password, confirmPassword)
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
    private fun validateFields() {
        val signUpUser = binding.buttonSignUp

        signUpUser.setOnClickListener {
            val email = binding.editTextEmailSignUp.text.toString()
            val password = binding.editTextPasswordSignUp.text.toString()
            val confirmPassword = binding.editTextConfirmPassword.text.toString()
            registerViewModel.onRegister(email, password, confirmPassword)
        }
    }

    //TODO CAMBIAR LOS IDS DE ADS(ANUNCIOS) EN EL XML Y EL MANIFEST CUANDO SE TERMINE LA APP, LOS ACTUALES SON DE PRUEBA (Ver strings o video de ari)
    private fun loadAds() {
        val adRequest = AdRequest.Builder().build()
        binding.bannerRegister.loadAd(adRequest)

        binding.bannerRegister.adListener = object: AdListener() {
            override fun onAdLoaded() {
                binding.bannerRegister.visibility = View.VISIBLE
            }
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Toast.makeText(context, "An error occurred with the ad", Toast.LENGTH_SHORT).show()
                Log.e("admob", "Ocurrio un Error$adError")
            }
            override fun onAdOpened() {
                Toast.makeText(context, "The ad was successfully opened", Toast.LENGTH_SHORT).show()
            }
            override fun onAdClicked() {
                Toast.makeText(context, "The ad was clicked", Toast.LENGTH_SHORT).show()
            }
            override fun onAdClosed() {
                Toast.makeText(context, "Ad closed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}


