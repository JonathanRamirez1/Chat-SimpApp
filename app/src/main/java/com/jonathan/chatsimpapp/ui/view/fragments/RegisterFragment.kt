package com.jonathan.chatsimpapp.ui.view.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.jonathan.chatsimpapp.*
import com.jonathan.chatsimpapp.core.*
import com.jonathan.chatsimpapp.core.ext.createFactory
import com.jonathan.chatsimpapp.data.network.GlideClient
import com.jonathan.chatsimpapp.databinding.FragmentRegisterBinding
import com.jonathan.chatsimpapp.ui.viewmodels.RegisterViewModel

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

        animation()
        validFields()
        validateFields()
        launchLoginFragment(view)
        loadAds()
    }

    private fun animation() {
        val imageLogo = binding.imageViewLogoRegister
        Glide.with(this)
            .load(GlideClient.uri)
            .into(imageLogo)
    }

    private fun setObservers() {

        registerViewModel.isEmailValid.observe(viewLifecycleOwner) { email ->
            if (email  != null) {
                binding.materialTextInputEditTextEmail.error = getString(R.string.email_is_no_valid)
            }
        }

        registerViewModel.isPasswordValid.observe(viewLifecycleOwner) { password ->
            isEnabledTogglePassword(password)
        }

        registerViewModel.isConfirmPasswordValid.observe(viewLifecycleOwner) { confirmPassword ->
            isEnabledToggleConfirmPassword(confirmPassword)
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

        binding.materialTextInputEditTextEmail.validate { email ->
            registerViewModel.validEmail(email)
        }

        binding.materialTextInputEditTextPassword.validate { password ->
            registerViewModel.validPassword(password)
        }

        binding.materialTextInputEditTextConfirmPassword.validate { confirmPassword ->
            val password = binding.materialTextInputEditTextPassword.text.toString()
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

    private fun isEnabledToggleConfirmPassword(confirmPassword: String?) {
        binding.materialTextInputEditTextConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {
                if (editable.trim().isNotEmpty()) {
                    binding.materialTextInputLayoutConfirmPassword.isPasswordVisibilityToggleEnabled = true
                    binding.materialTextInputEditTextPassword.error = null
                } else if (confirmPassword != null) {
                    binding.materialTextInputLayoutConfirmPassword.isPasswordVisibilityToggleEnabled = false
                    binding.materialTextInputEditTextConfirmPassword.error = getString(R.string.confirm_password_is_no_valid)
                }
            }
        })
    }

    /**Valida si los campos son correctos de acuerdo a los expresiones regulares (Ver Extensions.kt)**/
    private fun validateFields() {
        val signUpUser = binding.buttonRegister

        signUpUser.setOnClickListener {
            val email = binding.materialTextInputEditTextEmail.text.toString()
            val password = binding.materialTextInputEditTextPassword.text.toString()
            val confirmPassword = binding.materialTextInputEditTextConfirmPassword.text.toString()
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
                Log.e("admob", "An error occurred$adError")
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