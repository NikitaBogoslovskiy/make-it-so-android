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
    lateinit var applicationContext: Context
    lateinit var oneTapClient: SignInClient
    lateinit var signInRequest: BeginSignInRequest
    lateinit var signUpRequest: BeginSignInRequest

    val REQ_ONE_TAP_AUTH = 2
    val REQ_ONE_TAP_SIGNUP = 3
    var showOneTapUI = true

    lateinit var beginSignIn: () -> Unit
    lateinit var beginSignUp: () -> Unit
    lateinit var auth: (String?) -> Unit
    lateinit var signup: (String?) -> Unit

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
                    .setFilterByAuthorizedAccounts(true)
                    .build())
            .setAutoSelectEnabled(true)
            .build()
        signUpRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(applicationContext.getString(R.string.your_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build())
            .build()
    }

    fun processIntent(requestCode: Int, data: Intent?) {
        when (requestCode) {
            REQ_ONE_TAP_AUTH -> {
                try {
                    val credential = oneTapClient.getSignInCredentialFromIntent(data)
                    val idToken = credential.googleIdToken
                    val username = credential.id
                    val password = credential.password
                    auth(idToken)
                } catch (e: ApiException) {
                    auth(null)
                }
            }
            REQ_ONE_TAP_SIGNUP -> {
                try {
                    val credential = oneTapClient.getSignInCredentialFromIntent(data)
                    val idToken = credential.googleIdToken
                    val username = credential.id
                    val password = credential.password
                    signup(idToken)
                } catch (e: ApiException) {
                    signup(null)
                }
            }
        }
    }
}