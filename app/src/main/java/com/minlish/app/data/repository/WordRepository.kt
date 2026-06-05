package com.minlish.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.minlish.app.data.model.Word
import kotlinx.coroutines.tasks.await

class WordRepository {
    private val db = FirebaseFirestore.getInstance()
    private val wordsRef = db.collection("words")

    suspend fun addWord(word: Word): String {
        val doc = wordsRef.document()
        val newWord = word.copy(id = doc.id)
        doc.set(newWord).await()
        return doc.id
    }

    suspend fun getSetWords(setId: String): List<Word> {
        val snapshot = wordsRef.whereEqualTo("vocabSetId", setId).get().await()
        return snapshot.toObjects(Word::class.java)
            .sortedBy { it.createdAt }
    }

    suspend fun deleteWordCascade(wordId: String) {
        val recordsRef = db.collection("learning_records")
        val recordSnapshot = recordsRef.whereEqualTo("wordId", wordId).get().await()
        for (doc in recordSnapshot.documents) {
            doc.reference.delete().await()
        }
        wordsRef.document(wordId).delete().await()
    }

    suspend fun deleteWord(wordId: String) {
        deleteWordCascade(wordId)
    }

    suspend fun updateWord(wordId: String, updates: Map<String, Any>) {
        wordsRef.document(wordId).update(updates).await()
    }

    suspend fun getWordsByIds(wordIds: List<String>): List<Word> {
        if (wordIds.isEmpty()) return emptyList()
        val snapshot = wordsRef.whereIn("id", wordIds.take(30)).get().await()
        return snapshot.toObjects(Word::class.java)
    }
}
