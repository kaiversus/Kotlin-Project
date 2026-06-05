package com.minlish.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.minlish.app.data.model.LearningRecord
import com.minlish.app.data.model.Word
import kotlinx.coroutines.tasks.await

class VaultRepository {
    private val db = FirebaseFirestore.getInstance()
    private val setsRef = db.collection("vocab_sets")
    private val wordsRef = db.collection("words")
    private val recordsRef = db.collection("learning_records")

    suspend fun getLearnedRecords(userId: String): List<LearningRecord> {
        val snapshot = recordsRef
            .whereEqualTo("userId", userId)
            .get().await()
        return snapshot.toObjects(LearningRecord::class.java)
            .filter { it.status != "NEW" }
    }

    suspend fun getUserSets(userId: String): List<String> {
        val snapshot = setsRef.whereEqualTo("userId", userId).get().await()
        return snapshot.documents.map { it.id }
    }

    suspend fun getWordsForSets(setIds: List<String>): List<Word> {
        val allWords = mutableListOf<Word>()
        for (setId in setIds) {
            val snapshot = wordsRef.whereEqualTo("vocabSetId", setId).get().await()
            allWords.addAll(snapshot.toObjects(Word::class.java))
        }
        return allWords
    }
}
