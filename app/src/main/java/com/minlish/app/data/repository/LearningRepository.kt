package com.minlish.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.minlish.app.data.model.DailyStats
import com.minlish.app.data.model.LearningRecord
import com.minlish.app.data.model.Streak
import com.minlish.app.data.model.Word
import kotlinx.coroutines.tasks.await
import kotlin.math.max

data class BalancedDailyPlan(
    val plannedReview: List<Word>,
    val plannedNew: List<Word>
) {
    fun allStudyWords(): List<Word> = (plannedReview + plannedNew).distinctBy { it.id }
}

class LearningRepository {
    private val db = FirebaseFirestore.getInstance()
    private val recordsRef = db.collection("learning_records")
    private val userRepo = UserRepository()

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
        
        try {
            updateDailyStatsForReview(record.userId, record.repetitions == 0L && grade >= 1, grade >= 2)
            maybeUpdateStreakOnDailyGoalMet(record.userId)
        } catch (e: Exception) {
            // Silence stats error to ensure learning is uninterrupted
        }
        
        return updated
    }

    fun previewGrade(record: LearningRecord, grade: Int): LearningRecord = applySM2(record, grade)

    fun formatInterval(days: Long): String = when {
        days <= 0L -> "< 1m"
        days == 1L -> "1d"
        else -> "${days}d"
    }

    fun formatTimeRemaining(nextReviewDate: Long): String {
        val now = System.currentTimeMillis()
        if (nextReviewDate <= now) return "Cần ôn ngay"
        val diffMs = nextReviewDate - now
        val diffMinutes = diffMs / (60 * 1000)
        val diffHours = diffMs / (60 * 60 * 1000)
        val diffDays = diffMs / (24L * 60 * 60 * 1000)

        return when {
            diffMinutes < 60 -> "${diffMinutes} phút nữa"
            diffHours < 24 -> "${diffHours} giờ nữa"
            else -> "${diffDays} ngày nữa"
        }
    }

    // SM-2 algorithm: grade 0=Again 1=Hard 2=Good 3=Easy
    private fun applySM2(record: LearningRecord, grade: Int): LearningRecord {
        val now = System.currentTimeMillis()
        var ef = record.easeFactor
        var interval = record.interval
        var repetitions = record.repetitions

        if (grade < 1) {
            repetitions = 0L
            interval = 0L
        } else {
            ef = max(1.3f, ef + (0.1f - (3 - grade) * (0.08f + (3 - grade) * 0.02f)))
            interval = when (repetitions) {
                0L -> 1L
                1L -> 4L
                else -> (interval * ef).toLong().coerceAtLeast(interval + 1L)
            }
            repetitions++
        }

        val nextReview = now + interval * 24L * 60 * 60 * 1000
        val status = when {
            repetitions == 0L -> "LEARNING"
            interval >= 21L  -> "MASTERED"
            interval >= 4L   -> "REVIEW"
            else            -> "LEARNING"
        }

        return record.copy(
            status = status,
            easeFactor = ef,
            interval = interval,
            repetitions = repetitions,
            nextReviewDate = nextReview,
            lastGrade = grade.toLong(),
            totalReviews = record.totalReviews + 1L,
            correctReviews = if (grade >= 2) record.correctReviews + 1L else record.correctReviews,
            firstLearnedAt = record.firstLearnedAt ?: if (grade >= 1) now else null,
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
        val accuracy = if (totalAnswers > 0L) (correct * 100L / totalAnswers).toInt() else 0
        return Triple(total, mastered, accuracy)
    }

    suspend fun getDailyPlanStats(userId: String, startOfDay: Long, endOfDay: Long): Pair<Int, Int> {
        val records = getUserRecords(userId)

        val newToday = records.count { record ->
            val firstLearnedAt = record.firstLearnedAt ?: return@count false
            firstLearnedAt in startOfDay..endOfDay
        }

        val reviewToday = records.count { record ->
            val lastReviewedAt = record.lastReviewedAt ?: return@count false
            val firstLearnedAt = record.firstLearnedAt ?: return@count false
            lastReviewedAt in startOfDay..endOfDay && firstLearnedAt < startOfDay
        }

        return Pair(newToday, reviewToday)
    }

    suspend fun getDailyPlanNewWords(userId: String, allWords: List<Word>): List<Word> {
        val recordByWordId = getUserRecords(userId).associateBy { it.wordId }
        return allWords.filter { word ->
            val record = recordByWordId[word.id]
            record == null || record.firstLearnedAt == null
        }
    }

    suspend fun getDailyPlanReviewWords(
        userId: String,
        allWords: List<Word>,
        endOfDay: Long
    ): List<Word> {
        val recordByWordId = getUserRecords(userId).associateBy { it.wordId }
        return allWords.filter { word ->
            val record = recordByWordId[word.id] ?: return@filter false
            record.firstLearnedAt != null && record.nextReviewDate <= endOfDay
        }
    }

    suspend fun getCompletedNewWordsToday(
        userId: String,
        allWords: List<Word>,
        startOfDay: Long,
        endOfDay: Long
    ): List<Word> {
        val wordIds = getUserRecords(userId)
            .filter { record ->
                val firstLearnedAt = record.firstLearnedAt ?: return@filter false
                firstLearnedAt in startOfDay..endOfDay
            }
            .map { it.wordId }
            .toSet()
        return allWords.filter { it.id in wordIds }
    }

    suspend fun getCompletedReviewWordsToday(
        userId: String,
        allWords: List<Word>,
        startOfDay: Long,
        endOfDay: Long
    ): List<Word> {
        val wordIds = getUserRecords(userId)
            .filter { record ->
                val lastReviewedAt = record.lastReviewedAt ?: return@filter false
                val firstLearnedAt = record.firstLearnedAt ?: return@filter false
                lastReviewedAt in startOfDay..endOfDay && firstLearnedAt < startOfDay
            }
            .map { it.wordId }
            .toSet()
        return allWords.filter { it.id in wordIds }
    }

    suspend fun getAllWordsStudiedToday(
        userId: String,
        allWords: List<Word>,
        startOfDay: Long,
        endOfDay: Long
    ): List<Word> {
        val newWords = getCompletedNewWordsToday(userId, allWords, startOfDay, endOfDay)
        val reviewWords = getCompletedReviewWordsToday(userId, allWords, startOfDay, endOfDay)
        return (newWords + reviewWords).distinctBy { it.id }
    }

    data class StudySessionWords(
        val words: List<Word>,
        val isTodayReview: Boolean
    )

    suspend fun resolveStudySessionWords(
        userId: String,
        allWords: List<Word>,
        dailyTarget: Int,
        startOfDay: Long,
        endOfDay: Long,
        todayReviewOnly: Boolean = false
    ): StudySessionWords {
        if (todayReviewOnly) {
            val todayWords = getAllWordsStudiedToday(userId, allWords, startOfDay, endOfDay).shuffled()
            return StudySessionWords(words = todayWords, isTodayReview = true)
        }
        val reviewWords = getDailyPlanReviewWords(userId, allWords, endOfDay).shuffled()
        return StudySessionWords(words = reviewWords, isTodayReview = false)
    }

    fun balanceDailyPlan(
        pendingReview: List<Word>,
        pendingNew: List<Word>,
        remainingQuota: Int
    ): BalancedDailyPlan {
        if (remainingQuota <= 0) {
            return BalancedDailyPlan(plannedReview = emptyList(), plannedNew = emptyList())
        }

        val reviewSlot = remainingQuota / 2
        val newSlot = remainingQuota - reviewSlot

        var selectedReview = pendingReview.take(reviewSlot)
        var selectedNew = pendingNew.take(newSlot)

        var unfilled = remainingQuota - selectedReview.size - selectedNew.size

        if (unfilled > 0) {
            val extraNew = pendingNew
                .drop(selectedNew.size)
                .take(unfilled)
            selectedNew = selectedNew + extraNew
            unfilled -= extraNew.size
        }

        if (unfilled > 0) {
            val extraReview = pendingReview
                .drop(selectedReview.size)
                .take(unfilled)
            selectedReview = selectedReview + extraReview
        }

        return BalancedDailyPlan(
            plannedReview = selectedReview,
            plannedNew = selectedNew
        )
    }

    suspend fun getBalancedDailyPlan(
        userId: String,
        allWords: List<Word>,
        dailyTarget: Int,
        startOfDay: Long,
        endOfDay: Long
    ): BalancedDailyPlan {
        val pendingNew = getDailyPlanNewWords(userId, allWords)
        val pendingReview = getDailyPlanReviewWords(userId, allWords, endOfDay)
        val (completedNew, completedReview) = getDailyPlanStats(userId, startOfDay, endOfDay)
        val remainingQuota = (dailyTarget - completedNew - completedReview).coerceAtLeast(0)
        return balanceDailyPlan(pendingReview, pendingNew, remainingQuota)
    }

    suspend fun getDailyPlanStudyWords(
        userId: String,
        allWords: List<Word>,
        dailyTarget: Int,
        startOfDay: Long,
        endOfDay: Long
    ): List<Word> {
        return getBalancedDailyPlan(userId, allWords, dailyTarget, startOfDay, endOfDay)
            .allStudyWords()
    }

    suspend fun getDailyPlanSessionWords(
        userId: String,
        allWords: List<Word>,
        dailyTarget: Int,
        startOfDay: Long,
        endOfDay: Long
    ): List<Word> = getDailyPlanStudyWords(userId, allWords, dailyTarget, startOfDay, endOfDay)

    suspend fun getDueRecords(userId: String, limit: Int = 5): List<LearningRecord> {
        val currentTime = System.currentTimeMillis()
        val snapshot = recordsRef
            .whereEqualTo("userId", userId)
            .get().await()
        return snapshot.toObjects(LearningRecord::class.java)
            .filter { it.status != "NEW" && it.nextReviewDate <= currentTime }
            .sortedBy { it.nextReviewDate }
            .take(limit)
    }

    suspend fun getDueRecordsCount(userId: String): Int {
        val currentTime = System.currentTimeMillis()
        val snapshot = recordsRef
            .whereEqualTo("userId", userId)
            .get().await()
        return snapshot.toObjects(LearningRecord::class.java)
            .count { it.status != "NEW" && it.nextReviewDate <= currentTime }
    }

    private val statsRef = db.collection("daily_stats")
    private val streaksRef = db.collection("streaks")

    private fun getTodayStartTimestamp(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    suspend fun getDailyStats(userId: String): DailyStats {
        val today = getTodayStartTimestamp()
        val docId = "${userId}_$today"
        val doc = statsRef.document(docId).get().await()

        if (doc.exists()) {
            return doc.toObject(DailyStats::class.java)!!
        }

        val newStats = DailyStats(
            id = docId,
            userId = userId,
            date = today,
            newWordsLearned = 0,
            wordsReviewed = 0,
            correctAnswers = 0,
            totalAnswers = 0,
            studyMinutes = 0,
            goalMet = false
        )
        statsRef.document(docId).set(newStats).await()
        return newStats
    }

    private suspend fun updateDailyStatsForReview(userId: String, isNewWord: Boolean, isCorrect: Boolean) {
        val stats = getDailyStats(userId)
        val updates = mutableMapOf<String, Any>(
            "totalAnswers" to stats.totalAnswers + 1L,
            "correctAnswers" to if (isCorrect) stats.correctAnswers + 1L else stats.correctAnswers
        )
        if (isNewWord) {
            updates["newWordsLearned"] = stats.newWordsLearned + 1L
        } else {
            updates["wordsReviewed"] = stats.wordsReviewed + 1L
        }
        statsRef.document(stats.id).update(updates).await()
    }

    private fun getTodayEndTimestamp(): Long =
        getTodayStartTimestamp() + 24L * 60 * 60 * 1000 - 1

    suspend fun isStreakGoalMet(userId: String): Boolean {
        val dailyTarget = userRepo.getUser(userId)?.dailyTarget?.toInt()?.coerceAtLeast(1) ?: 10
        val startOfDay = getTodayStartTimestamp()
        val endOfDay = getTodayEndTimestamp()
        val (completedNew, _) = getDailyPlanStats(userId, startOfDay, endOfDay)
        return completedNew >= dailyTarget
    }

    private suspend fun maybeUpdateStreakOnDailyGoalMet(userId: String) {
        if (!isStreakGoalMet(userId)) return

        val stats = getDailyStats(userId)
        if (stats.goalMet) return

        statsRef.document(stats.id).update("goalMet", true).await()
        updateStreakAfterReview(userId)
    }

    suspend fun getStreak(userId: String): Streak {
        val snapshot = streaksRef.document(userId).get().await()
        if (snapshot.exists()) {
            return snapshot.toObject(Streak::class.java)!!
        }
        
        val newStreak = Streak(
            userId = userId,
            currentStreak = 0,
            longestStreak = 0,
            lastStudyDate = 0L,
            totalDaysStudied = 0,
            freezesUsed = 0
        )
        streaksRef.document(userId).set(newStreak).await()
        return newStreak
    }

    private suspend fun updateStreakAfterReview(userId: String) {
        val streak = getStreak(userId)
        val today = getTodayStartTimestamp()
        val lastStudy = streak.lastStudyDate
        
        if (lastStudy == today) {
            return
        }
        
        val oneDayMs = 24L * 60 * 60 * 1000
        val isYesterday = (today - lastStudy) <= oneDayMs
        
        val newCurrent = if (isYesterday || lastStudy == 0L) {
            streak.currentStreak + 1L
        } else {
            1L
        }
        
        val newLongest = maxOf(streak.longestStreak, newCurrent)
        val newStreak = streak.copy(
            currentStreak = newCurrent,
            longestStreak = newLongest,
            lastStudyDate = today,
            totalDaysStudied = streak.totalDaysStudied + 1L
        )
        streaksRef.document(userId).set(newStreak).await()
    }

    private suspend fun getUserRecords(userId: String): List<LearningRecord> {
        val snapshot = recordsRef.whereEqualTo("userId", userId).get().await()
        return snapshot.toObjects(LearningRecord::class.java)
    }

    suspend fun getAllRecords(userId: String): List<LearningRecord> = getUserRecords(userId)

    suspend fun getWeeklyActivity(userId: String): List<DailyStats> {
        val records = getUserRecords(userId)
        val today = getTodayStartTimestamp()
        
        val activityMap = mutableMapOf<Long, DailyStats>()
        
        // Initialize last 7 days with 0
        for (i in 0..6) {
            val cal = java.util.Calendar.getInstance().apply {
                timeInMillis = today
                add(java.util.Calendar.DAY_OF_YEAR, -i)
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val date = cal.timeInMillis
            activityMap[date] = DailyStats(date = date, userId = userId)
        }

        records.forEach { record ->
            val lastReviewed = record.lastReviewedAt ?: return@forEach
            val cal = java.util.Calendar.getInstance().apply {
                timeInMillis = lastReviewed
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val date = cal.timeInMillis
            
            if (activityMap.containsKey(date)) {
                val stats = activityMap[date]!!
                // If repetitions is 1, it was likely first learned on this date
                val isNew = record.repetitions == 1L && (record.firstLearnedAt ?: 0L) >= date && (record.firstLearnedAt ?: 0L) < (date + 24*60*60*1000)
                
                activityMap[date] = stats.copy(
                    wordsReviewed = if (!isNew) stats.wordsReviewed + 1L else stats.wordsReviewed,
                    newWordsLearned = if (isNew) stats.newWordsLearned + 1L else stats.newWordsLearned
                )
            }
        }

        return activityMap.values.sortedBy { it.date }
    }
}
