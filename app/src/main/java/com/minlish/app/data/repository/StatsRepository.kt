package com.minlish.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.minlish.app.data.model.DailyStats
import com.minlish.app.data.model.Streak
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class StatsRepository {
    private val db = FirebaseFirestore.getInstance()
    private val statsRef = db.collection("daily_stats")
    private val streaksRef = db.collection("streaks")

    fun getStreakFlow(userId: String): Flow<Streak?> = callbackFlow {
        val listener = streaksRef.document(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(Streak::class.java))
        }
        awaitClose { listener.remove() }
    }

    suspend fun getDailyStats(userId: String, limit: Int = 7): List<DailyStats> {
        val snapshot = statsRef
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get().await()
        return snapshot.toObjects(DailyStats::class.java).reversed()
    }

    suspend fun getStreak(userId: String): Streak? {
        val snapshot = streaksRef.document(userId).get().await()
        return snapshot.toObject(Streak::class.java)
    }

    suspend fun updateDailyStats(userId: String, stats: DailyStats) {
        val docId = "${userId}_${stats.date}"
        statsRef.document(docId).set(stats.copy(id = docId, userId = userId)).await()
    }

    suspend fun updateStreak(userId: String, streak: Streak) {
        streaksRef.document(userId).set(streak.copy(userId = userId)).await()
    }
}
