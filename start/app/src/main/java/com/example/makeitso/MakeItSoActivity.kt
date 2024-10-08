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

package com.example.makeitso

import android.content.ContentValues.TAG
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import dagger.hilt.android.AndroidEntryPoint

// MakeItSoActivity starts the first composable, which uses material cards that are still experimental.
// TODO: Update material dependency and experimental annotations once the API stabilizes.
@AndroidEntryPoint
@ExperimentalMaterialApi
class MakeItSoActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { MakeItSoApp() }
    GoogleAuthData.init(this)

    GoogleAuthData.beginSignIn = {
      GoogleAuthData.oneTapClient.beginSignIn(GoogleAuthData.signInRequest)
        .addOnSuccessListener(this) { result ->
          try {
            startIntentSenderForResult(
              result.pendingIntent.intentSender, GoogleAuthData.REQ_ONE_TAP,
              null, 0, 0, 0, null)
          } catch (e: IntentSender.SendIntentException) {
            Log.e(TAG, "Couldn't start One Tap UI: ${e.localizedMessage}")
          }
        }
        .addOnFailureListener(this) { e ->
          Log.d(TAG, e.localizedMessage)
        }
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    GoogleAuthData.processIntent(requestCode, data)
  }
}
