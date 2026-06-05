package com.minlish.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.minlish.app.data.model.VocabSet
import kotlinx.coroutines.tasks.await

class VocabSetRepository {
    private val db = FirebaseFirestore.getInstance()
    private val setsRef = db.collection("vocab_sets")

    suspend fun createSet(set: VocabSet): String {
        val doc = setsRef.document()
        val newSet = set.copy(id = doc.id)
        doc.set(newSet).await()
        return doc.id
    }

    suspend fun getUserSets(userId: String): List<VocabSet> {
        val snapshot = setsRef.whereEqualTo("userId", userId).get().await()
        return snapshot.toObjects(VocabSet::class.java)
            .sortedByDescending { it.createdAt }
    }

    suspend fun getSet(setId: String): VocabSet? {
        val snapshot = setsRef.document(setId).get().await()
        return snapshot.toObject(VocabSet::class.java)
    }

    suspend fun updateWordCount(setId: String, total: Long, learned: Long) {
        setsRef.document(setId).update(
            mapOf("totalWords" to total, "learnedWords" to learned,
                "updatedAt" to System.currentTimeMillis())
        ).await()
    }

    suspend fun deleteSetCascade(setId: String) {
        val wordsRef = db.collection("words")
        val recordsRef = db.collection("learning_records")

        val wordsSnapshot = wordsRef.whereEqualTo("vocabSetId", setId).get().await()
        val wordIds = wordsSnapshot.documents.map { it.id }

        for (wordId in wordIds) {
            val recordSnapshot = recordsRef.whereEqualTo("wordId", wordId).get().await()
            for (doc in recordSnapshot.documents) {
                doc.reference.delete().await()
            }
        }

        for (doc in wordsSnapshot.documents) {
            doc.reference.delete().await()
        }

        setsRef.document(setId).delete().await()
    }

    suspend fun deleteSet(setId: String) {
        deleteSetCascade(setId)
    }

    suspend fun updateFavoriteStatus(setId: String, isFavorite: Boolean) {
        setsRef.document(setId).update("isFavorite", isFavorite).await()
    }
}
