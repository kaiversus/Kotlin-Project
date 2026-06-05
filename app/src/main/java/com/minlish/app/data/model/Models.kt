package com.minlish.app.data.model

import com.google.firebase.firestore.PropertyName

/**
 * users/{userId}
 * Tài khoản người dùng — xác thực, profile, cài đặt
 */
data class User(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val avatarUrl: String? = null,
    val englishLevel: String = "A1",        // A1 / A2 / B1 / B2 / C1 / C2
    val learningGoal: String = "General",   // IELTS / TOEIC / Communication / General
    val dailyTarget: Long = 10,
    val notificationTime: String? = "20:00",
    @get:PropertyName("darkMode") @set:PropertyName("darkMode")
    var darkMode: Boolean = false,
    val appLanguage: String = "VI",         // VI / EN
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * vocab_sets/{setId}
 * Bộ từ vựng — container chứa các từ
 */
data class VocabSet(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String? = null,
    val tags: String = "[]",                // JSON array: ["IELTS","Academic"]
    @get:PropertyName("isFavorite") val isFavorite: Boolean = false,
    val totalWords: Long = 0,                // Cached count
    val learnedWords: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * words/{wordId}
 * Từ vựng — đơn vị học cơ bản
 */
data class Word(
    val id: String = "",
    val vocabSetId: String = "",
    val word: String = "",
    val pronunciation: String? = null,      // IPA: "/məˈtɪk.jə.ləs/"
    val audioUrl: String? = null,
    val meaning: String = "",
    val description: String? = null,
    val exampleSentence: String? = null,
    val collocation: String? = null,
    val relatedWords: String? = null,       // JSON array
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * learning_records/{recordId}
 * Trạng thái SM-2 của mỗi từ với mỗi user
 */
data class LearningRecord(
    val id: String = "",
    val userId: String = "",
    val wordId: String = "",
    val status: String = "NEW",             // NEW / LEARNING / REVIEW / MASTERED
    val easeFactor: Float = 2.5f,           // SM-2: min 1.3
    val interval: Long = 0,
    val repetitions: Long = 0,
    val nextReviewDate: Long = 0L,
    val lastGrade: Long = 0,                 // 0=Again, 1=Hard, 2=Good, 3=Easy
    val totalReviews: Long = 0,
    val correctReviews: Long = 0,
    val firstLearnedAt: Long? = null,
    val lastReviewedAt: Long? = null
)

/**
 * review_sessions/{sessionId}
 * Lịch sử mỗi phiên học
 */
data class ReviewSession(
    val id: String = "",
    val userId: String = "",
    val vocabSetId: String? = null,
    val mode: String = "FLASHCARD",         // FLASHCARD / MULTIPLE_CHOICE / TYPING / LISTENING
    val sessionType: String = "NEW_WORDS",  // NEW_WORDS / REVIEW_DUE / CUSTOM
    val status: String = "IN_PROGRESS",     // IN_PROGRESS / COMPLETED / PAUSED
    val totalCards: Long = 0,
    val completedCards: Long = 0,
    val correctCount: Long = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null
)

/**
 * daily_stats/{statsId}
 * Hoạt động mỗi ngày — vẽ biểu đồ
 */
data class DailyStats(
    val id: String = "",
    val userId: String = "",
    val date: Long = 0L,                    // Timestamp 00:00 của ngày
    val newWordsLearned: Long = 0,
    val wordsReviewed: Long = 0,
    val correctAnswers: Long = 0,
    val totalAnswers: Long = 0,
    val studyMinutes: Long = 0,
    val goalMet: Boolean = false
)

/**
 * streaks/{userId}
 * Chuỗi ngày học liên tiếp (1-1 với user)
 */
data class Streak(
    val userId: String = "",
    val currentStreak: Long = 0,
    val longestStreak: Long = 0,
    val lastStudyDate: Long = 0L,
    val totalDaysStudied: Long = 0,
    val freezesUsed: Long = 0
)

/**
 * achievements/{achievementId}
 * Badges và milestones
 */
data class Achievement(
    val id: String = "",
    val userId: String = "",
    val type: String = "",                  // STREAK_7 / WORDS_100 / ACCURACY_90 / FIRST_SET
    val title: String = "",
    val description: String = "",
    val iconRes: String = "",
    val earnedAt: Long = System.currentTimeMillis()
)

/**
 * notifications/{notificationId}
 * Cấu hình thông báo nhắc học
 */
data class Notification(
    val id: String = "",
    val userId: String = "",
    val type: String = "DAILY_REMINDER",    // DAILY_REMINDER / REVIEW_DUE / STREAK_RISK
    val scheduledTime: String = "20:00",
    @get:PropertyName("isEnabled") val isEnabled: Boolean = true,
    val workerTag: String? = null
)
