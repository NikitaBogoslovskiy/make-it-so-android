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

package com.example.makeitso.model.service.impl

import com.example.makeitso.model.User
import com.example.makeitso.model.service.AccountService
import com.example.makeitso.model.service.trace
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AccountServiceImpl @Inject constructor(private val auth: FirebaseAuth) : AccountService {

  override val currentUserId: String
    get() = auth.currentUser?.uid.orEmpty()

  override val hasUser: Boolean
    get() = auth.currentUser != null

  override val currentUser: Flow<User>
    get() = callbackFlow {
      val listener =
        FirebaseAuth.AuthStateListener { auth ->
          this.trySend(auth.currentUser?.let { User(uid = it.uid, isAnonymous = it.isAnonymous) } ?: User())
        }
      auth.addAuthStateListener(listener)
      awaitClose { auth.removeAuthStateListener(listener) }
    }

  override suspend fun authenticate(email: String, password: String) {
    val account = getCurrentAccount()
    auth.signInWithEmailAndPassword(email, password).await()
    if (account.isAnonymous) {
      deleteAccount(account)
    }
  }

  override suspend fun authenticate(authCredential: AuthCredential) {
    val account = getCurrentAccount()
    auth.signInWithCredential(authCredential).await()
    if (account.isAnonymous) {
      deleteAccount(account)
    }
  }

  override suspend fun linkAccount(authCredential: AuthCredential) {
    auth.currentUser!!.linkWithCredential(authCredential).await()
  }

  override suspend fun sendRecoveryEmail(email: String) {
    auth.sendPasswordResetEmail(email).await()
  }

  override suspend fun createAnonymousAccount() {
    auth.signInAnonymously().await()
  }

  override suspend fun linkAccount(email: String, password: String) {
    val credential = EmailAuthProvider.getCredential(email, password)
    linkAccount(credential)
  }

  override suspend fun deleteAccount() {
    auth.currentUser!!.delete().await()
  }

  override suspend fun signOut() {
    val account = getCurrentAccount()
    if (account.isAnonymous) {
      deleteAccount(account)
    }
    auth.signOut()

    // Sign the user back in anonymously.
    createAnonymousAccount()
  }

  override fun getCurrentAccount() = auth.currentUser!!

  private fun deleteAccount(account: FirebaseUser) {
      account.delete()
  }

  companion object {
    private const val LINK_ACCOUNT_TRACE = "linkAccount"
  }
}
