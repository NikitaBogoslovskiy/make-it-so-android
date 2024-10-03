/*
Copyright 2022 Google LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package com.example.makeitso.screens.login

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.makeitso.GoogleAuthData
import com.example.makeitso.LOGIN_SCREEN
import com.example.makeitso.SETTINGS_SCREEN
import com.example.makeitso.TASKS_SCREEN
import com.example.makeitso.common.ext.isValidEmail
import com.example.makeitso.common.snackbar.SnackbarManager
import com.example.makeitso.model.User
import com.example.makeitso.model.service.AccountService
import com.example.makeitso.model.service.LogService
import com.example.makeitso.model.service.StorageService
import com.example.makeitso.screens.MakeItSoViewModel
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import com.example.makeitso.R.string as AppText

@HiltViewModel
class LoginViewModel @Inject constructor(
  private val accountService: AccountService,
  private val storageService: StorageService,
  logService: LogService,
) : MakeItSoViewModel(logService) {
  private lateinit var popup: () -> Unit

  init {
      GoogleAuthData.auth = { token ->
        val firebaseCredential = GoogleAuthProvider.getCredential(token, null)
        launchCatching {
          accountService.authenticate(firebaseCredential)
          popup()
          saveUserIfDoNotExist()
        }
      }
  }

  var uiState = mutableStateOf(LoginUiState())
    private set

  private val email
    get() = uiState.value.email
  private val password
    get() = uiState.value.password

  fun onEmailChange(newValue: String) {
    uiState.value = uiState.value.copy(email = newValue)
  }

  fun onPasswordChange(newValue: String) {
    uiState.value = uiState.value.copy(password = newValue)
  }

  fun onSignInClick(openAndPopUp: (String, String) -> Unit) {
    if (!email.isValidEmail()) {
      SnackbarManager.showMessage(AppText.email_error)
      return
    }

    if (password.isBlank()) {
      SnackbarManager.showMessage(AppText.empty_password_error)
      return
    }

    launchCatching {
      accountService.authenticate(email, password)
      openAndPopUp(TASKS_SCREEN, LOGIN_SCREEN)
    }
  }

  fun onSignInWithGoogleClick(openAndPopUp: (String, String) -> Unit) {
    popup = { openAndPopUp(TASKS_SCREEN, LOGIN_SCREEN) }
    GoogleAuthData.beginSignIn()
  }

  fun onForgotPasswordClick() {
    if (!email.isValidEmail()) {
      SnackbarManager.showMessage(AppText.email_error)
      return
    }

    launchCatching {
      accountService.sendRecoveryEmail(email)
      SnackbarManager.showMessage(AppText.recovery_email_sent)
    }
  }

  private suspend fun saveUserIfDoNotExist() {
    storageService.users.collect {
      var user = it.firstOrNull()
      if (user == null) {
        user = User.fromFirebaseUser(accountService.getCurrentAccount())
        storageService.save(user)
      }
    }
  }

}
