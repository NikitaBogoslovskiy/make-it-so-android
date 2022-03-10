package com.example.makeitso.screens.login

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.makeitso.R.string as AppText
import com.example.makeitso.common.snackbar.SnackbarMessage.Companion.toSnackbarMessage
import com.example.makeitso.common.ext.isValidEmail
import com.example.makeitso.common.snackbar.SnackbarManager
import com.example.makeitso.model.database.repository.TaskRepository
import com.example.makeitso.model.service.AccountService
import com.example.makeitso.model.service.CrashlyticsService
import com.example.makeitso.model.service.FirestoreService
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val accountService: AccountService,
    private val firestoreService: FirestoreService,
    private val crashlyticsService: CrashlyticsService,
    private val taskRepository: TaskRepository
) : ViewModel() {
    var uiState = mutableStateOf(LoginUiState())
        private set

    private val email get() = uiState.value.email
    private val password get() = uiState.value.password

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        SnackbarManager.showMessage(AppText.generic_error)
        viewModelScope.launch { crashlyticsService.logNonFatalCrash(throwable) }
    }

    fun onEmailChange(newValue: String) {
        uiState.value = uiState.value.copy(email = newValue)
    }

    fun onPasswordChange(newValue: String) {
        uiState.value = uiState.value.copy(password = newValue)
    }

    fun onSignInClick(popUpScreen: () -> Unit) {
        if (!email.isValidEmail()) {
            SnackbarManager.showMessage(AppText.email_error)
            return
        }

        if (password.isBlank()) {
            SnackbarManager.showMessage(AppText.empty_password_error)
            return
        }

        viewModelScope.launch(exceptionHandler) {
            accountService.authenticate(email, password) { task ->
                task.onResult { linkWithEmail(popUpScreen) }
            }
        }
    }

    private fun linkWithEmail(popUpScreen: () -> Unit) {
        viewModelScope.launch(exceptionHandler) {
            accountService.linkAccount(email, password) { task ->
                task.onResult { updateUserId(popUpScreen) }
            }
        }
    }

    private fun updateUserId(popUpScreen: () -> Unit) {
        viewModelScope.launch(exceptionHandler) {
            val oldUserId = accountService.getAnonymousUserId()
            val newUserId = accountService.getUserId()

            firestoreService.updateUserId(oldUserId, newUserId) { error ->
                if (error == null) {
                    viewModelScope.launch { taskRepository.updateUserId(oldUserId, newUserId) }
                    popUpScreen()
                } else {
                    viewModelScope.launch { crashlyticsService.logNonFatalCrash(error) }
                }
            }
        }
    }

    private fun Task<AuthResult>.onResult(successCallback: () -> Unit) {
        if (this.isSuccessful) {
            successCallback()
        } else {
            SnackbarManager.showMessage(this.exception.toSnackbarMessage())
            crashlyticsService.logNonFatalCrash(this.exception)
        }
    }

    fun onForgotPasswordClick() {
        if (!email.isValidEmail()) {
            SnackbarManager.showMessage(AppText.email_error)
            return
        }

        viewModelScope.launch(exceptionHandler) {
            accountService.sendRecoveryEmail(email) { error ->
                if (error == null) {
                    SnackbarManager.showMessage(AppText.recovery_email_sent)
                } else {
                    SnackbarManager.showMessage(error.toSnackbarMessage())
                    crashlyticsService.logNonFatalCrash(error)
                }
            }
        }
    }
}