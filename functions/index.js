/**
 * Firebase Cloud Functions for Maths By Mithlesh Sir
 *
 * Provides trusted server-side capabilities when deployed on Firebase Blaze plan:
 *  1. setAdminCustomClaim: Grants/revokes verified admin custom claims.
 *  2. submitTestAttempt: Authoritative backend scoring using protected answer_keys.
 *  3. onAnnouncementPublished: Sends FCM topic notification for new announcements.
 *  4. createOrResetStudentAuth: Admin-only Student account management without storing passwords in Firestore.
 */

const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");

admin.initializeApp();

// Matches the provisioned named Firestore database ID from firebase_applet_config.xml
const DATABASE_ID =
  process.env.FIRESTORE_DATABASE_ID ||
  "ai-studio-android-mathsbym-9071d880-3944-40ae-95ed-3772f3ace1d9";

const db = getFirestore(DATABASE_ID);

async function assertAdmin(request) {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Authentication required.");
  }
  const uid = request.auth.uid;
  const token = request.auth.token;
  if (token.admin === true) return uid;
  if (token.email === "mithleshyadaw0@gmail.com" && token.email_verified === true) {
    return uid;
  }
  const adminDoc = await db.collection("admins").doc(uid).get();
  if (!adminDoc.exists) {
    throw new HttpsError("permission-denied", "Administrator privileges required.");
  }
  return uid;
}

/**
 * 1. Grant or revoke Firebase Auth Custom Claim `admin: true`
 */
exports.setAdminCustomClaim = onCall(async (request) => {
  await assertAdmin(request);
  const { targetUid, isAdmin } = request.data || {};
  if (!targetUid || typeof isAdmin !== "boolean") {
    throw new HttpsError("invalid-argument", "targetUid and boolean isAdmin are required.");
  }
  await admin.auth().setCustomUserClaims(targetUid, { admin: isAdmin });
  if (isAdmin) {
    const userRecord = await admin.auth().getUser(targetUid);
    await db.collection("admins").doc(targetUid).set({
      email: userRecord.email || "",
      name: userRecord.displayName || "Admin",
      createdAt: FieldValue.serverTimestamp(),
    });
  } else {
    await db.collection("admins").doc(targetUid).delete();
  }
  return { success: true, targetUid, isAdmin };
});

/**
 * 2. Authoritative Server-Side Exam Scoring & Duplicate Prevention
 */
exports.submitTestAttempt = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Sign in required to submit tests.");
  }
  const uid = request.auth.uid;
  const { testId, answers = {}, timeTakenSeconds = 0 } = request.data || {};
  if (!testId || typeof testId !== "string") {
    throw new HttpsError("invalid-argument", "Valid testId is required.");
  }

  const resultId = `${uid}_${testId}`;
  const existingResult = await db.collection("results").doc(resultId).get();
  if (existingResult.exists) {
    throw new HttpsError("already-exists", "Duplicate submission blocked: result already recorded.");
  }

  const testDoc = await db.collection("tests").doc(testId).get();
  if (!testDoc.exists || !testDoc.data().published) {
    throw new HttpsError("not-found", "Published test not found.");
  }
  const testData = testDoc.data();

  const [questionsSnap, keysSnap, userDoc] = await Promise.all([
    db.collection("tests").doc(testId).collection("questions").get(),
    db.collection("tests").doc(testId).collection("answer_keys").get(),
    db.collection("users").doc(uid).get(),
  ]);

  const userData = userDoc.exists ? userDoc.data() : {};
  if (userData.active === false) {
    throw new HttpsError("permission-denied", "Student account is deactivated.");
  }

  const keyMap = {};
  keysSnap.forEach((doc) => {
    keyMap[doc.id] = doc.data();
  });

  let correctCount = 0;
  let wrongCount = 0;
  let unattemptedCount = 0;
  let obtainedMarks = 0;
  let computedTotalMarks = 0;
  const subjectBreakdown = {};
  const negMark = Number(testData.negativeMarking || 0);

  questionsSnap.forEach((qDoc) => {
    const q = qDoc.data();
    const key = keyMap[qDoc.id];
    const marks = Number(q.marks || 1);
    computedTotalMarks += marks;
    const subj = q.subject || testData.subject || "Mathematics";
    if (!subjectBreakdown[subj]) {
      subjectBreakdown[subj] = { correct: 0, wrong: 0, total: 0, obtained: 0, maxMarks: 0 };
    }
    subjectBreakdown[subj].total += 1;
    subjectBreakdown[subj].maxMarks += marks;

    const selected = answers[qDoc.id];
    if (selected === undefined || selected === null || selected < 0) {
      unattemptedCount += 1;
    } else if (key && Number(selected) === Number(key.correctOptionIndex)) {
      correctCount += 1;
      obtainedMarks += marks;
      subjectBreakdown[subj].correct += 1;
      subjectBreakdown[subj].obtained += marks;
    } else {
      wrongCount += 1;
      obtainedMarks -= negMark;
      subjectBreakdown[subj].wrong += 1;
      subjectBreakdown[subj].obtained -= negMark;
    }
  });

  const totalMarks = computedTotalMarks > 0 ? computedTotalMarks : Number(testData.totalMarks || 1);
  const clampedMarks = Math.max(0, Number(obtainedMarks.toFixed(2)));
  const percentage = Math.max(0, Math.min(100, Number(((clampedMarks / totalMarks) * 100).toFixed(2))));
  const attempted = correctCount + wrongCount;
  const accuracy = attempted > 0 ? Number(((correctCount / attempted) * 100).toFixed(2)) : 0;

  const now = FieldValue.serverTimestamp();
  const batch = db.batch();

  batch.set(
    db.collection("users").doc(uid).collection("attempts").doc(testId),
    {
      userId: uid,
      testId,
      status: "submitted",
      answers,
      markedForReview: [],
      currentQuestionIndex: 0,
      remainingSeconds: 0,
      elapsedSeconds: Math.max(0, Math.min(18000, Number(timeTakenSeconds))),
      startedAt: now,
      updatedAt: now,
      submittedAt: now,
    },
    { merge: true }
  );

  batch.set(db.collection("results").doc(resultId), {
    userId: uid,
    studentName: userData.name || "Student",
    studentRollNo: userData.studentId || "",
    testId,
    testTitle: testData.title,
    category: testData.category,
    subject: testData.subject,
    totalMarks,
    obtainedMarks: clampedMarks,
    percentage,
    correctCount,
    wrongCount,
    unattemptedCount,
    accuracy,
    timeTakenSeconds: Math.max(0, Math.min(18000, Number(timeTakenSeconds))),
    subjectBreakdown,
    answers,
    submittedAt: now,
    createdAt: now,
  });

  await batch.commit();
  return {
    resultId,
    obtainedMarks: clampedMarks,
    totalMarks,
    percentage,
    correctCount,
    wrongCount,
    unattemptedCount,
    accuracy,
  };
});

/**
 * 3. Broadcast FCM push notification when Admin publishes a new Announcement
 */
exports.onAnnouncementPublished = onDocumentCreated(
  {
    document: "announcements/{announcementId}",
    database: DATABASE_ID,
  },
  async (event) => {
    const data = event.data?.data();
    if (!data || !data.published) return;
    const topic =
      data.targetCategory && data.targetCategory !== "ALL"
        ? `cat_${data.targetCategory.toLowerCase().replace(/\s+/g, "_")}`
        : "all_students";
    await admin.messaging().send({
      topic,
      notification: {
        title: data.title || "Maths By Mithlesh Sir",
        body: data.message || "New announcement available",
      },
      data: {
        announcementId: event.params.announcementId,
        category: data.targetCategory || "ALL",
      },
    });
  }
);

/**
 * 4. Admin-only Student password reset link generation (never stores passwords in Firestore)
 */
exports.generateStudentPasswordReset = onCall(async (request) => {
  await assertAdmin(request);
  const { studentEmail } = request.data || {};
  if (!studentEmail || typeof studentEmail !== "string") {
    throw new HttpsError("invalid-argument", "studentEmail is required.");
  }
  const resetLink = await admin.auth().generatePasswordResetLink(studentEmail);
  return { success: true, resetLink };
});
