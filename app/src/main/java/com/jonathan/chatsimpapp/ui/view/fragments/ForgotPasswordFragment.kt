package com.jonathan.chatsimpapp.ui.view.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.core.ext.createFactory
import com.jonathan.chatsimpapp.core.validate
import com.jonathan.chatsimpapp.databinding.FragmentForgotPasswordBinding
import com.jonathan.chatsimpapp.ui.viewmodels.ForgotPasswordViewModel

class ForgotPasswordFragment : Fragment() {

    private lateinit var binding: FragmentForgotPasswordBinding
    private lateinit var navController: NavController

    private val forgotPasswordViewModel by viewModels<ForgotPasswordViewModel> {
        ForgotPasswordViewModel().createFactory()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObservers()
        validResetPassword()
        launchLoginFragmentFromForgotPassword(view)
        resetPassword()
        setTemplateNativeAdvanced()
    }

    private fun setObservers() {

        forgotPasswordViewModel.emailValid.observe(viewLifecycleOwner) { email ->
            if (email != null) {
                binding.materialTextInputEditTextEmail.error = getString(R.string.email_is_no_valid)
            }
        }

        forgotPasswordViewModel.showToast.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            Log.d("CONSOLE", "onError $it ")
        }

        forgotPasswordViewModel.isResetPassword.observe(viewLifecycleOwner) { resetPassword ->
            if (resetPassword == true) {
                view?.let { goToLoginView(it) }
            }
        }
    }

    private fun goToLoginView(view: View) {
        navController = Navigation.findNavController(view)
        navController.navigate(R.id.loginFragment)
    }

    private fun validResetPassword() {
        binding.materialTextInputEditTextEmail.validate { email ->
            forgotPasswordViewModel.validEmail(email)
        }
    }

    private fun launchLoginFragmentFromForgotPassword(view: View) {
        val goLogin = binding.buttonGoLogInForgot

        navController = Navigation.findNavController(view)
        goLogin.setOnClickListener {
            navController.navigate(R.id.loginFragment)
        }
    }

    private fun resetPassword() {
        val resetPassword = binding.buttonForgotPassword

        resetPassword.setOnClickListener {
            val email = binding.materialTextInputEditTextEmail.text.toString()
            forgotPasswordViewModel.onResetPassword(email)
        }
    }

    private fun setTemplateNativeAdvanced() {
        val adRequest = AdRequest.Builder().build()
        MobileAds.initialize(requireContext())
        val adLoader: AdLoader = AdLoader.Builder(requireContext(), getString(R.string.test_native_advanced))
            .forNativeAd { nativeAd ->
                val template = binding.templateForgotPassword
                template.setNativeAd(nativeAd)
            }
            .build()

        adLoader.loadAd(adRequest)
    }
}