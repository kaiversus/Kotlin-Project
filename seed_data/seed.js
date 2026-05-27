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

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});

const db = admin.firestore();

// ======== CẤU HÌNH ========
// Thay bằng UID của tài khoản bạn đã đăng ký trên app
// Lấy UID: Firebase Console → Authentication → Users → copy cột "User UID"
const TARGET_USER_ID = "j6YuOWYdf8gOSUmKrzNP92mh72N2";
// ==========================

async function seed() {
  if (TARGET_USER_ID === "PASTE_YOUR_UID_HERE") {
    console.error("❌ Chưa điền TARGET_USER_ID. Mở file seed.js và paste UID vào.");
    process.exit(1);
  }

  console.log(`\n🚀 Bắt đầu tạo dữ liệu mẫu cho user: ${TARGET_USER_ID}\n`);

  for (const set of data.sets) {
    // Tạo vocab set
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

    // Tạo từng từ trong bộ
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
      console.log(`   + ${word.word}`);
    }
  }

  console.log("\n🎉 Hoàn thành! Mở app và kiểm tra bộ từ vựng.");
  process.exit(0);
}

seed().catch((err) => {
  console.error("❌ Lỗi:", err.message);
  process.exit(1);
});
