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

package com.example.makeitso.model

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId val id: String = "",
    val uid: String = "",
    val isAnonymous: Boolean = true,
    val name: String = "",
    val birthDate: String = "",
    val login: String = "",
    val authTypes: List<String> = emptyList()
) {
    companion object {
        @JvmStatic
        fun fromFirebaseUser(firebaseUser: FirebaseUser) = User(
            uid = firebaseUser.uid,
            name = (firebaseUser.providerData.firstOrNull { !it.displayName.isNullOrEmpty() }?.displayName) ?: "",
            login = (firebaseUser.providerData.firstOrNull { !it.email.isNullOrEmpty() }?.email) ?: "",
            authTypes = firebaseUser.providerData.map {
                when (it.providerId) {
                    "password" -> "Login and Password"
                    "google.com" -> "Google Account"
                    else -> ""
                }
            }.filter { it != "" }
        )
    }
}
