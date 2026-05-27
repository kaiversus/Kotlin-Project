package com.minlish.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.minlish.app.data.model.User
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val db = FirebaseFirestore.getInstance()
    private val usersRef = db.collection("users")

    suspend fun createUser(user: User) {
        usersRef.document(user.id).set(user).await()
    }

    suspend fun getUser(userId: String): User? {
        val snapshot = usersRef.document(userId).get().await()
        return snapshot.toObject(User::class.java)
    }

    suspend fun updateUser(userId: String, updates: Map<String, Any>) {
        usersRef.document(userId).update(updates).await()
    }

    suspend fun deleteUser(userId: String) {
        usersRef.document(userId).delete().await()
    }
}
