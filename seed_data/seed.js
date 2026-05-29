/**
 * Script tạo dữ liệu mẫu cho Firestore
 *
 * HƯỚNG DẪN CHẠY:
 * 1. Cài Node.js nếu chưa có: https://nodejs.org
 * 2. Vào Firebase Console → Project Settings → Service accounts
 *    → "Generate new private key" → tải về → đổi tên thành serviceAccountKey.json
 *    → đặt vào cùng thư mục với file seed.js này
 * 3. Mở terminal trong thư mục seed_data, chạy:
 *      npm install firebase-admin
 *      node seed.js
 * 4. Kiểm tra Firestore Console xem data đã lên chưa
 */

const admin = require("firebase-admin");
const serviceAccount = require("./serviceAccountKey.json");
const data = require("./sample_vocab.json");
const dailyPlanData = require("./sample_daily_plan_timeline.json");

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});

const db = admin.firestore();

// ======== CẤU HÌNH ========
// Thay bằng UID của tài khoản bạn đã đăng ký trên app
// Lấy UID: Firebase Console → Authentication → Users → copy cột "User UID"
const TARGET_USER_ID = "j6YuOWYdf8gOSUmKrzNP92mh72N2";
// snapshot = test hôm nay (29/05), full = trạng thái cuối timeline (07/06)
const SEED_MODE = "snapshot";
// ==========================

async function seed() {
  if (TARGET_USER_ID === "PASTE_YOUR_UID_HERE") {
    console.error("❌ Chưa điền TARGET_USER_ID. Mở file seed.js và paste UID vào.");
    process.exit(1);
  }

  console.log(`\n🚀 Bắt đầu tạo dữ liệu mẫu cho user: ${TARGET_USER_ID}\n`);

  const firstSetWordIds = [];

  for (const set of data.sets) {
    const setRef = db.collection("vocab_sets").doc();
    const setData = {
      id: setRef.id,
      userId: TARGET_USER_ID,
      name: set.name,
      description: set.description || null,
      tags: "[]",
      isFavorite: false,
      totalWords: set.words.length,
      learnedWords: 0,
      createdAt: Date.now(),
      updatedAt: Date.now(),
    };
    await setRef.set(setData);
    console.log(`✅ Bộ từ: "${set.name}" (${set.words.length} từ)`);

    for (const word of set.words) {
      const wordRef = db.collection("words").doc();
      await wordRef.set({
        id: wordRef.id,
        vocabSetId: setRef.id,
        word: word.word,
        pronunciation: word.pronunciation || null,
        audioUrl: null,
        meaning: word.meaning,
        description: word.description || null,
        exampleSentence: word.exampleSentence || null,
        collocation: word.collocation || null,
        relatedWords: word.relatedWords || null,
        note: word.note || null,
        createdAt: Date.now(),
      });
      if (firstSetWordIds.length < 10) {
        firstSetWordIds.push(wordRef.id);
      }
      console.log(`   + ${word.word}`);
    }
  }

  // Cập nhật dailyTarget cho user
  await db.collection("users").doc(TARGET_USER_ID).set(
    {
      dailyTarget: dailyPlanData.meta.dailyTarget || 10,
    },
    { merge: true }
  );
  console.log(`✅ User dailyTarget = ${dailyPlanData.meta.dailyTarget || 10}`);

  // Seed daily_stats (29/05 -> 07/06)
  const dailyStats = dailyPlanData.dailyStats || [];
  for (const stats of dailyStats) {
    await db.collection("daily_stats").doc(stats.id).set({
      ...stats,
      userId: TARGET_USER_ID,
    });
  }
  console.log(`✅ Daily stats: ${dailyStats.length} ngày (29/05 - 07/06)`);

  // Seed learning_records theo timeline
  const recordSource =
    SEED_MODE === "full"
      ? dailyPlanData.fullTimelineEndState
      : dailyPlanData.snapshotAsOfReferenceDate;

  for (const record of recordSource) {
    const wordId = firstSetWordIds[record.wordIndex];
    if (!wordId) {
      console.warn(`⚠️  Bỏ qua record ${record.id}: không map được wordIndex ${record.wordIndex}`);
      continue;
    }

    await db.collection("learning_records").doc(record.id).set({
      id: record.id,
      userId: TARGET_USER_ID,
      wordId,
      status: record.status,
      easeFactor: record.easeFactor,
      interval: record.interval,
      repetitions: record.repetitions,
      nextReviewDate: record.nextReviewDate,
      lastGrade: record.lastGrade,
      totalReviews: record.totalReviews,
      correctReviews: record.correctReviews,
      firstLearnedAt: record.firstLearnedAt,
      lastReviewedAt: record.lastReviewedAt,
    });
  }
  console.log(`✅ Learning records (${SEED_MODE}): ${recordSource.length} documents`);

  const expected = dailyPlanData.expectedOnReferenceDate;
  console.log("\n📊 Kỳ vọng trên Learn (reference 29/05):");
  console.log(`   - New today: ${expected.newWordsToday}`);
  console.log(`   - Review today: ${expected.reviewToday}`);
  console.log(`   - Progress: ${expected.progressPercentWithTarget10}%`);
  console.log(`   - Flashcard queue indices: ${expected.flashcardQueueWordIndices.join(", ")}`);

  console.log("\n🎉 Hoàn thành! Mở app và kiểm tra Vocab + Learn + Flashcard.");
  process.exit(0);
}

seed().catch((err) => {
  console.error("❌ Lỗi:", err.message);
  process.exit(1);
});
