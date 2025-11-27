const { onCall } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { logger } = require("firebase-functions");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");

admin.initializeApp();

// ======================================================
//  Email Verification
// ======================================================

// 환경변수 가져오기
const gmailUser = process.env.GMAIL_USER;
const gmailPass = process.env.GMAIL_PASS;

// Gmail SMTP 설정
const transporter = nodemailer.createTransport({
  service: "gmail",
  auth: {
    user: gmailUser,
    pass: gmailPass,
  },
});

// 이메일 인증 코드 발송
exports.sendVerificationCode = onCall(async (req) => {
  const rawEmail = req.data?.email || "";
  const email = rawEmail.trim().toLowerCase();
  if (!email) throw new Error("Missing email");

  const code = String(Math.floor(100000 + Math.random() * 900000)); // 6자리 코드
  const expiresAt = Date.now() + 10 * 60 * 1000;

  await admin.firestore().collection("email_verifications")
    .doc(email).set({ code, expiresAt });

  await transporter.sendMail({
    from: `"LendMark" <${gmailUser}>`,
    to: email,
    subject: "[LendMark] Email Authentication Code",
    html: `
      <div style="font-family:sans-serif;">
        <h2>Welcome to the LendMark application!</h2>
        <p>Please enter the authentication code below into the app:</p>
        <h1 style="letter-spacing:4px;">${code}</h1>
        <p>Valid time: 10 minutes</p>
      </div>
    `,
  });

  return { ok: true };
});

exports.verifyEmailCode = onCall(async (req) => {
  const rawEmail = req.data?.email || "";
  const email = rawEmail.trim().toLowerCase();
  const code = String((req.data?.code || "").toString().trim());

  const snap = await admin.firestore().collection("email_verifications")
    .doc(email).get();
  if (!snap.exists) return { ok: false, reason: "NOT_FOUND" };

  const { code: saved, expiresAt } = snap.data();

  console.log("verify", { email, inputCode: code, saved, expiresAt });

  if (Date.now() > expiresAt) return { ok: false, reason: "EXPIRED" };
  if (saved !== code)         return { ok: false, reason: "INVALID" };

  await snap.ref.delete();
  return { ok: true };
});


 exports.finishPastReservations = onSchedule("every 30 minutes", async () => {
   const now = new Date();
   const db = admin.firestore();

   // 오늘 날짜 (YYYY-MM-DD)
   const yyyy = now.getFullYear();
   const mm = String(now.getMonth() + 1).padStart(2, "0");
   const dd = String(now.getDate()).padStart(2, "0");
   const todayStr = `${yyyy}-${mm}-${dd}`;

   console.log("Running finishPastReservations:", todayStr);

   // 1) 아직 approved 상태인 예약 전부 가져오기
   const snapshot = await db.collection("reservations")
     .where("status", "==", "approved")
     .get();

   if (snapshot.empty) {
     console.log("No approved reservations found.");
     return null;
   }

   const batch = db.batch();

   snapshot.forEach(doc => {
     const data = doc.data();
     const dateStr = data.date; // yyyy-MM-dd
     const periodEnd = data.periodEnd; // integer (e.g., 1 = 09:00~10:00)
     const reservationDay = new Date(dateStr);

     // → 종료 시간 = 오전 8시 기준 periodEnd + 1
     const endHour = 8 + (periodEnd + 1);

     // ============ 1) 날짜가 오늘 이전이면 finished ============
     if (dateStr < todayStr) {
       batch.update(doc.ref, { status: "finished" });
       return;
     }

     // 날짜가 오늘 이후이면 아직 finished 아니므로 return
     if (dateStr > todayStr) return;

     // ============ 2) 날짜가 오늘과 같으면 시간 비교 ============
     const nowHour = now.getHours();

     if (nowHour >= endHour) {
       batch.update(doc.ref, { status: "finished" });
     }
   });

   await batch.commit();
   console.log("finishPastReservations completed.");
   return null;
 });


/* ============================================================
   2) 매일 실행 → 1주일 지난 finished 예약을 expired 처리
   ============================================================ */
exports.expireOldReservations = onSchedule("every day 00:00", async () => {
  const now = Date.now();
  const oneWeekAgo = now - 7 * 24 * 60 * 60 * 1000;

  console.log("Running expireOldReservations");

  const snapshot = await db.collection("reservations")
    .where("status", "==", "finished")
    .where("timestamp", "<", oneWeekAgo)
    .get();

  if (snapshot.empty) {
    console.log("No reservations to expire.");
    return null;
  }

  console.log(`Expiring ${snapshot.size} reservations...`);

  const batch = db.batch();
  snapshot.forEach(doc => {
    batch.update(doc.ref, { status: "expired" });
  });

  await batch.commit();

  console.log("Old reservations expired!");
  return null;
});

// (B) 예약 생성 시 충돌 체크 + 저장 (안드로이드에서 호출)
exports.createReservation = onCall(async (req) => {
  const db = admin.firestore();

  const {
    userId,
    userName,
    major,
    people,
    purpose,
    buildingId,
    roomId,
    day,
    date,
    periodStart,
    periodEnd
  } = req.data;

  // 1) 충돌(오버랩) 검사
  const conflict = await db.collection("reservations")
    .where("buildingId", "==", buildingId)
    .where("roomId", "==", roomId)
    .where("date", "==", date)
    .where("status", "==", "approved")
    .get();

  for (const doc of conflict.docs) {
    const r = doc.data();
    const s = r.periodStart;
    const e = r.periodEnd;

    const overlapped = !(periodEnd < s || periodStart > e);
    if (overlapped) {
      return { success: false, reason: "TIME_CONFLICT" };
    }
  }

  // 2) 충돌 없으면 예약 저장
  const newReservation = {
    userId,
    userName,
    major,
    people,
    purpose,
    buildingId,
    roomId,
    day,
    date,
    periodStart,
    periodEnd,
    timestamp: Date.now(),
    status: "approved"
  };

  await db.collection("reservations").add(newReservation);
  return { success: true };
});
