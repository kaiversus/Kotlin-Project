package com.minlish.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.minlish.app.data.model.LearningRecord
import kotlinx.coroutines.tasks.await
import kotlin.math.max

class LearningRepository {
    private val db = FirebaseFirestore.getInstance()
    private val recordsRef = db.collection("learning_records")

    suspend fun getOrCreateRecord(userId: String, wordId: String): LearningRecord {
        val snapshot = recordsRef
            .whereEqualTo("userId", userId)
            .whereEqualTo("wordId", wordId)
            .get().await()

        if (!snapshot.isEmpty) {
            return snapshot.documents[0].toObject(LearningRecord::class.java)!!
        }

        val doc = recordsRef.document()
        val record = LearningRecord(id = doc.id, userId = userId, wordId = wordId)
        doc.set(record).await()
        return record
    }

    suspend fun updateWithGrade(record: LearningRecord, grade: Int): LearningRecord {
        val updated = applySM2(record, grade)
        recordsRef.document(record.id).set(updated).await()
        return updated
    }

    // SM-2 algorithm: grade 0=Again 1=Hard 2=Good 3=Easy
    private fun applySM2(record: LearningRecord, grade: Int): LearningRecord {
        val now = System.currentTimeMillis()
        var ef = record.easeFactor
        var interval = record.interval
        var repetitions = record.repetitions

        if (grade < 1) {
            repetitions = 0
            interval = 0
        } else {
            ef = max(1.3f, ef + (0.1f - (3 - grade) * (0.08f + (3 - grade) * 0.02f)))
            interval = when (repetitions) {
                0 -> 1
                1 -> 4
                else -> (interval * ef).toInt().coerceAtLeast(interval + 1)
            }
            repetitions++
        }

        val nextReview = now + interval * 24L * 60 * 60 * 1000
        val status = when {
            repetitions == 0 -> "LEARNING"
            interval >= 21  -> "MASTERED"
            interval >= 4   -> "REVIEW"
            else            -> "LEARNING"
        }

        return record.copy(
            status = status,
            easeFactor = ef,
            interval = interval,
            repetitions = repetitions,
            nextReviewDate = nextReview,
            lastGrade = grade,
            totalReviews = record.totalReviews + 1,
            correctReviews = if (grade >= 2) record.correctReviews + 1 else record.correctReviews,
            firstLearnedAt = record.firstLearnedAt ?: now,
            lastReviewedAt = now
        )
    }

    suspend fun getRecordsForSet(userId: String, wordIds: List<String>): List<LearningRecord> {
        if (wordIds.isEmpty()) return emptyList()
        val snapshot = recordsRef
            .whereEqualTo("userId", userId)
            .whereIn("wordId", wordIds.take(30))
            .get().await()
        return snapshot.toObjects(LearningRecord::class.java)
    }

    suspend fun getTotalStats(userId: String): Triple<Int, Int, Int> {
        val snapshot = recordsRef.whereEqualTo("userId", userId).get().await()
        val records = snapshot.toObjects(LearningRecord::class.java)
        val total = records.size
        val mastered = records.count { it.status == "MASTERED" }
        val totalAnswers = records.sumOf { it.totalReviews }
        val correct = records.sumOf { it.correctReviews }
        val accuracy = if (totalAnswers > 0) (correct * 100 / totalAnswers) else 0
        return Triple(total, mastered, accuracy)
    }

    suspend fun getDailyPlanStats(userId: String, startOfDay: Long, endOfDay: Long): Pair<Int, Int> {
        val snapshot = recordsRef.whereEqualTo("userId", userId).get().await()
        val records = snapshot.toObjects(LearningRecord::class.java)

        val newToday = records.count { record ->
            val firstLearnedAt = record.firstLearnedAt ?: return@count false
            firstLearnedAt in startOfDay..endOfDay
        }

        val reviewToday = records.count { record ->
            val lastReviewedAt = record.lastReviewedAt ?: return@count false
            lastReviewedAt in startOfDay..endOfDay
        }

        return Pair(newToday, reviewToday)
    }
}
