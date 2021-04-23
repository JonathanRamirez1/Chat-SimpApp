package com.jonathan.loginfuturo.model

import android.app.Application
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.api.Context
import com.google.api.ContextOrBuilder
import com.jonathan.loginfuturo.R
import kotlinx.coroutines.withContext

class LoginModel  {

   /*  fun getGoogleApiClient() : GoogleApiClient? {
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(Application().getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        return this.let {
            GoogleApiClient.Builder(Application())
                .enableAutoManage(FragmentActivity(), this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, googleSignInOptions)
                .build()
        }
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        TODO("Not yet implemented")
    }*/
}


