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

package com.example.makeitso.screens.settings

import androidx.compose.runtime.mutableStateOf
import com.example.makeitso.LOGIN_SCREEN
import com.example.makeitso.SIGN_UP_SCREEN
import com.example.makeitso.SPLASH_SCREEN
import com.example.makeitso.common.ext.idFromParameter
import com.example.makeitso.model.Task
import com.example.makeitso.model.User
import com.example.makeitso.model.service.AccountService
import com.example.makeitso.model.service.LogService
import com.example.makeitso.model.service.StorageService
import com.example.makeitso.screens.MakeItSoViewModel
import com.example.makeitso.screens.edit_task.EditTaskViewModel.Companion.DATE_FORMAT
import com.example.makeitso.screens.edit_task.EditTaskViewModel.Companion.UTC
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@HiltViewModel
class SettingsViewModel @Inject constructor(
  logService: LogService,
  private val accountService: AccountService,
  private val storageService: StorageService
) : MakeItSoViewModel(logService) {
  var uiState = accountService.currentUser.map {
    SettingsUiState(it.isAnonymous)
  }
  val user = mutableStateOf(User())
  val isEdited = mutableStateOf(false)

  init {
    launchCatching {
      storageService.users.collect {
        val firstUser = it.firstOrNull()
        if (firstUser != null) {
          user.value = storageService.getUser(firstUser.id) ?: User()
        }
      }
    }
  }

  fun onNameChange(newValue: String) {
    user.value = user.value.copy(name = newValue)
    isEdited.value = true
  }

  fun onBirthDateChange(newValue: Long) {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone(UTC))
    calendar.timeInMillis = newValue
    val newDueDate = SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH).format(calendar.time)
    user.value = user.value.copy(birthDate = newDueDate)
    isEdited.value = true
  }

  fun onDoneClick() {
    launchCatching {
      storageService.update(user.value)
      isEdited.value = false
    }
  }

  fun onLoginClick(openScreen: (String) -> Unit) = openScreen(LOGIN_SCREEN)

  fun onSignUpClick(openScreen: (String) -> Unit) = openScreen(SIGN_UP_SCREEN)

  fun onSignOutClick(restartApp: (String) -> Unit) {
    launchCatching {
      accountService.signOut()
      restartApp(SPLASH_SCREEN)
    }
  }

  fun onDeleteMyAccountClick(restartApp: (String) -> Unit) {
    launchCatching {
      storageService.users.collect {
        val user = it.firstOrNull()
        if (user != null) {
          storageService.delete(user)
        }
        accountService.deleteAccount()
        restartApp(SPLASH_SCREEN)
      }
    }
  }

}
