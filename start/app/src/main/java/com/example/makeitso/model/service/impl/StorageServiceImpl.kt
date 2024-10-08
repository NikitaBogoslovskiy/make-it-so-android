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

import com.example.makeitso.model.Task
import com.example.makeitso.model.User
import com.example.makeitso.model.service.AccountService
import com.example.makeitso.model.service.StorageService
import com.example.makeitso.model.service.trace
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.dataObjects
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.tasks.await

class StorageServiceImpl
@Inject
constructor(private val firestore: FirebaseFirestore, private val auth: AccountService) :
  StorageService {

  @OptIn(ExperimentalCoroutinesApi::class)
  override val tasks: Flow<List<Task>>
    get() = auth.currentUser.flatMapLatest { user ->
      firestore.collection(TASK_COLLECTION).whereEqualTo(USER_ID_FIELD, user.uid).dataObjects()
    }

  @OptIn(ExperimentalCoroutinesApi::class)
  override val users: Flow<List<User>>
    get() = auth.currentUser.flatMapLatest { user ->
      firestore.collection(USERS_COLLECTION).whereEqualTo(UID_FIELD, user.uid).dataObjects()
    }

  override suspend fun getTask(taskId: String): Task? =
    firestore.collection(TASK_COLLECTION).document(taskId).get().await().toObject()

  override suspend fun save(task: Task): String =
    trace(SAVE_TASK_TRACE) {
      val taskWithUserId = task.copy(userId = auth.currentUserId)
      firestore.collection(TASK_COLLECTION).add(taskWithUserId).await().id
    }

  override suspend fun update(task: Task): Unit =
    trace(UPDATE_TASK_TRACE) {
      firestore.collection(TASK_COLLECTION).document(task.id).set(task).await()
    }

  override suspend fun delete(taskId: String) {
    firestore.collection(TASK_COLLECTION).document(taskId).delete().await()
  }

  override suspend fun getUser(userId: String): User? =
    firestore.collection(USERS_COLLECTION).document(userId).get().await().toObject()

  override suspend fun save(user: User) {
    firestore.collection(USERS_COLLECTION).add(user).await()
  }

  override suspend fun update(user: User) {
    firestore.collection(USERS_COLLECTION).document(user.id).set(user).await()
  }

  override suspend fun delete(user: User) {
    firestore.collection(USERS_COLLECTION).document(user.id).delete().await()
  }

  companion object {
    private const val UID_FIELD = "uid"
    private const val USER_ID_FIELD = "userId"
    private const val TASK_COLLECTION = "tasks"
    private const val USERS_COLLECTION = "users"
    private const val SAVE_TASK_TRACE = "saveTask"
    private const val UPDATE_TASK_TRACE = "updateTask"
  }
}
