package com.example.makeitso

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.android.gms.common.api.ApiException

data class GoogleAuthResult(
    val credential: SignInCredential?,
    val idToken: String?,
    val username: String?,
    val password: String?)

@SuppressLint("StaticFieldLeak")
object GoogleAuthData {
    val REQ_ONE_TAP = 2

    lateinit var applicationContext: Context
    lateinit var oneTapClient: SignInClient
    lateinit var signInRequest: BeginSignInRequest

    lateinit var beginSignIn: () -> Unit
    lateinit var auth: (String?) -> Unit

    fun init(context: Context) {
        applicationContext = context
        oneTapClient = Identity.getSignInClient(applicationContext)
        signInRequest = BeginSignInRequest.builder()
            .setPasswordRequestOptions(BeginSignInRequest.PasswordRequestOptions.builder()
                .setSupported(true)
                .build())
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(applicationContext.getString(R.string.your_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build())
            .setAutoSelectEnabled(true)
            .build()
    }

    fun processIntent(requestCode: Int, data: Intent?) {
        when (requestCode) {
            REQ_ONE_TAP -> {
                try {
                    val credential = oneTapClient.getSignInCredentialFromIntent(data)
                    val idToken = credential.googleIdToken
                    auth(idToken)
                } catch (e: ApiException) {
                    //...
                }
            }
        }
    }
}