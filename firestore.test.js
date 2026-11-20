const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_mithlesh_01";
const STUDENT_A_UID = "student_aarav_01";
const STUDENT_B_UID = "student_rohit_02";
const SPOOF_UID = "spoof_attacker_99";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTime(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

function adminContext() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: "mithleshyadaw0@gmail.com",
    email_verified: true,
  });
}

function spoofAdminContext() {
  return testEnv.authenticatedContext(SPOOF_UID, {
    email: "mithleshyadaw0@gmail.com",
    email_verified: false,
  });
}

function studentContext(uid) {
  return testEnv.authenticatedContext(uid, {
    email: `${uid}@student.local`,
    email_verified: true,
  });
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users are denied all reads and writes", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("tests").where("published", "==", true).get());
  await assertFails(unauth.collection("users").doc(STUDENT_A_UID).get());
  await assertFails(unauth.collection("leaderboard").where("visible", "==", true).get());
  await assertFails(unauth.collection("notes").where("published", "==", true).get());
});

test("Email spoofing (unverified admin email) is strictly rejected", async () => {
  const spoofDb = spoofAdminContext().firestore();
  const now = pastTime();
  await assertFails(
    spoofDb.collection("tests").doc("test_spoof").set({
      title: "Fake Admin Test",
      category: "Navodaya",
      testType: "FULL",
      subject: "All Subjects",
      durationMinutes: 60,
      totalMarks: 100,
      negativeMarking: 0,
      questionCount: 1,
      published: true,
      featuredToday: false,
      assignmentMode: "ALL",
      assignedStudentUids: [],
      createdBy: SPOOF_UID,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Student profile isolation, anti-privilege escalation, and shadow update rejection", async () => {
  const studentA = studentContext(STUDENT_A_UID).firestore();
  const studentB = studentContext(STUDENT_B_UID).firestore();
  const now = pastTime();

  // Valid student self-registration
  await assertSucceeds(
    studentA.collection("users").doc(STUDENT_A_UID).set({
      userId: STUDENT_A_UID,
      name: "Aarav Kumar",
      email: "aarav@student.local",
      studentId: "MBS-2026-001",
      batch: "Navodaya-A",
      role: "student",
      active: true,
      streakDays: 0,
      totalTestsAttempted: 0,
      totalMarksObtained: 0,
      averagePercentage: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Student CANNOT self-assign admin role
  await assertFails(
    studentB.collection("users").doc(STUDENT_B_UID).set({
      userId: STUDENT_B_UID,
      name: "Fake Admin",
      email: "rohit@student.local",
      studentId: "MBS-2026-002",
      batch: "Navodaya-A",
      role: "admin",
      active: true,
      streakDays: 0,
      totalTestsAttempted: 0,
      totalMarksObtained: 0,
      averagePercentage: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Student B cannot read Student A's private profile (PII isolation)
  await assertFails(studentB.collection("users").doc(STUDENT_A_UID).get());
  await assertFails(studentB.collection("users").get());

  // Student A cannot escalate role or inject ghost field
  await assertFails(
    studentA.collection("users").doc(STUDENT_A_UID).update({
      role: "admin",
      updatedAt: pastTime(1000),
    })
  );
  await assertFails(
    studentA.collection("users").doc(STUDENT_A_UID).update({
      ghostField: true,
      updatedAt: pastTime(1000),
    })
  );

  // Valid student partial update of allowed stats
  await assertSucceeds(
    studentA.collection("users").doc(STUDENT_A_UID).update({
      streakDays: 1,
      totalTestsAttempted: 1,
      totalMarksObtained: 23,
      averagePercentage: 76.7,
      lastTestDate: "2026-11-20",
      updatedAt: pastTime(1000),
    })
  );
});

test("End-to-end test publishing, answer key lock before submission, attempt lock, and result creation", async () => {
  const adminDb = adminContext().firestore();
  const studentA = studentContext(STUDENT_A_UID).firestore();
  const studentB = studentContext(STUDENT_B_UID).firestore();
  const now = pastTime(10000);

  // 1. Admin creates and publishes a test + question + answer key
  await assertSucceeds(
    adminDb.collection("tests").doc("navodaya_mock_1").set({
      title: "Navodaya Mock 1",
      description: "Full JNVST practice test",
      category: "Navodaya",
      testType: "FULL",
      subject: "All Subjects",
      durationMinutes: 45,
      totalMarks: 10,
      negativeMarking: 0.25,
      questionCount: 1,
      published: true,
      featuredToday: true,
      assignmentMode: "ALL",
      assignedStudentUids: [],
      createdBy: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    adminDb
      .collection("tests")
      .doc("navodaya_mock_1")
      .collection("questions")
      .doc("q1")
      .set({
        testId: "navodaya_mock_1",
        orderIndex: 0,
        subject: "Reasoning",
        questionText: "Next number: 2, 4, 8, 16, ?",
        options: ["24", "30", "32", "36"],
        marks: 10,
        published: true,
        createdAt: now,
      })
  );

  await assertSucceeds(
    adminDb
      .collection("tests")
      .doc("navodaya_mock_1")
      .collection("answer_keys")
      .doc("q1")
      .set({
        testId: "navodaya_mock_1",
        orderIndex: 0,
        correctOptionIndex: 2,
        explanation: "Each term doubles: 16 * 2 = 32.",
        createdAt: now,
      })
  );

  // 2. Student can list published tests & questions ONLY with published == true filter
  await assertSucceeds(studentA.collection("tests").where("published", "==", true).get());
  await assertFails(studentA.collection("tests").get());
  await assertSucceeds(
    studentA
      .collection("tests")
      .doc("navodaya_mock_1")
      .collection("questions")
      .where("published", "==", true)
      .get()
  );

  // 3. CRITICAL: Student CANNOT read answer_keys BEFORE submitting!
  await assertFails(
    studentA.collection("tests").doc("navodaya_mock_1").collection("answer_keys").doc("q1").get()
  );
  await assertFails(
    studentA.collection("tests").doc("navodaya_mock_1").collection("answer_keys").get()
  );

  // 4. Student starts attempt (in_progress) and auto-saves
  await assertSucceeds(
    studentA
      .collection("users")
      .doc(STUDENT_A_UID)
      .collection("attempts")
      .doc("navodaya_mock_1")
      .set({
        userId: STUDENT_A_UID,
        testId: "navodaya_mock_1",
        status: "in_progress",
        answers: {},
        markedForReview: [],
        currentQuestionIndex: 0,
        remainingSeconds: 2700,
        elapsedSeconds: 0,
        startedAt: now,
        updatedAt: now,
      })
  );

  await assertSucceeds(
    studentA
      .collection("users")
      .doc(STUDENT_A_UID)
      .collection("attempts")
      .doc("navodaya_mock_1")
      .update({
        answers: { q1: 2 },
        markedForReview: ["q1"],
        currentQuestionIndex: 0,
        remainingSeconds: 2650,
        elapsedSeconds: 50,
        updatedAt: pastTime(3000),
      })
  );

  // Answer keys STILL blocked while in_progress!
  await assertFails(
    studentA.collection("tests").doc("navodaya_mock_1").collection("answer_keys").get()
  );

  // Student B cannot access Student A's attempt
  await assertFails(
    studentB
      .collection("users")
      .doc(STUDENT_A_UID)
      .collection("attempts")
      .doc("navodaya_mock_1")
      .get()
  );

  // 5. Student submits attempt (terminal state lock)
  const submitTime = pastTime(2000);
  await assertSucceeds(
    studentA
      .collection("users")
      .doc(STUDENT_A_UID)
      .collection("attempts")
      .doc("navodaya_mock_1")
      .update({
        answers: { q1: 2 },
        markedForReview: [],
        currentQuestionIndex: 0,
        remainingSeconds: 2600,
        elapsedSeconds: 100,
        status: "submitted",
        submittedAt: submitTime,
        updatedAt: submitTime,
      })
  );

  // 6. Terminal lock: Student CANNOT modify or re-submit attempt after submission!
  await assertFails(
    studentA
      .collection("users")
      .doc(STUDENT_A_UID)
      .collection("attempts")
      .doc("navodaya_mock_1")
      .update({
        answers: { q1: 1 },
        updatedAt: pastTime(500),
      })
  );

  // 7. Answer keys now unlock ONLY for Student A (Student B still blocked!)
  await assertSucceeds(
    studentA.collection("tests").doc("navodaya_mock_1").collection("answer_keys").get()
  );
  await assertFails(
    studentB.collection("tests").doc("navodaya_mock_1").collection("answer_keys").get()
  );

  // 8. Student A writes immutable result with deterministic ID {uid}_{testId}
  const resultId = `${STUDENT_A_UID}_navodaya_mock_1`;
  await assertSucceeds(
    studentA.collection("results").doc(resultId).set({
      userId: STUDENT_A_UID,
      studentName: "Aarav Kumar",
      studentRollNo: "MBS-2026-001",
      testId: "navodaya_mock_1",
      testTitle: "Navodaya Mock 1",
      category: "Navodaya",
      subject: "All Subjects",
      totalMarks: 10,
      obtainedMarks: 10,
      percentage: 100,
      correctCount: 1,
      wrongCount: 0,
      unattemptedCount: 0,
      accuracy: 100,
      timeTakenSeconds: 100,
      subjectBreakdown: { Reasoning: { correct: 1, total: 1 } },
      answers: { q1: 2 },
      submittedAt: submitTime,
      createdAt: submitTime,
    })
  );

  // Student A cannot tamper with result after creation or create arbitrary resultId
  await assertFails(
    studentA.collection("results").doc(resultId).update({ obtainedMarks: 999 })
  );
  await assertFails(
    studentA.collection("results").doc("arbitrary_dup_id").set({
      userId: STUDENT_A_UID,
      studentName: "Aarav Kumar",
      studentRollNo: "MBS-2026-001",
      testId: "navodaya_mock_1",
      testTitle: "Navodaya Mock 1",
      category: "Navodaya",
      subject: "All Subjects",
      totalMarks: 10,
      obtainedMarks: 10,
      percentage: 100,
      correctCount: 1,
      wrongCount: 0,
      unattemptedCount: 0,
      accuracy: 100,
      timeTakenSeconds: 100,
      subjectBreakdown: { Reasoning: { correct: 1, total: 1 } },
      answers: { q1: 2 },
      submittedAt: submitTime,
      createdAt: submitTime,
    })
  );

  // Student A can query own results; Student B cannot read Student A's results
  await assertSucceeds(
    studentA.collection("results").where("userId", "==", STUDENT_A_UID).get()
  );
  await assertFails(studentB.collection("results").doc(resultId).get());
  await assertFails(studentB.collection("results").get());
});

test("Leaderboard, Notes, Announcements, and Settings access controls", async () => {
  const adminDb = adminContext().firestore();
  const studentA = studentContext(STUDENT_A_UID).firestore();
  const now = pastTime();

  // Admin publishes a note, announcement, and settings
  await assertSucceeds(
    adminDb.collection("notes").doc("note_num_sys").set({
      title: "Number System Formula Sheet",
      subject: "Mathematics",
      chapter: "Number System",
      materialType: "FORMULA_SHEET",
      category: "Navodaya",
      description: "Complete JNVST Number System rules and shortcuts",
      contentMarkdown: "# Number System\nNatural numbers start from 1.",
      fileSizeLabel: "1.2 MB",
      published: true,
      createdBy: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    adminDb.collection("announcements").doc("ann_1").set({
      title: "Navodaya Mock 4 Live!",
      message: "Attempt before Sunday 8 PM.",
      targetCategory: "ALL",
      published: true,
      createdBy: ADMIN_UID,
      createdAt: now,
    })
  );

  await assertSucceeds(
    adminDb.collection("settings").doc("app_config").set({
      leaderboardEnabled: true,
      rankingMode: "PERCENTAGE",
      welcomeMessage: "सीखें गणित, बढ़ें सफलता की ओर",
      updatedBy: ADMIN_UID,
      updatedAt: now,
    })
  );

  // Student can read published notes, announcements, settings
  await assertSucceeds(studentA.collection("notes").where("published", "==", true).get());
  await assertSucceeds(studentA.collection("announcements").where("published", "==", true).get());
  await assertSucceeds(studentA.collection("settings").doc("app_config").get());

  // Student CANNOT write notes, announcements, or settings
  await assertFails(studentA.collection("notes").doc("note_num_sys").delete());
  await assertFails(
    studentA.collection("settings").doc("app_config").update({
      leaderboardEnabled: false,
      updatedBy: STUDENT_A_UID,
      updatedAt: pastTime(500),
    })
  );

  // Student updates own leaderboard entry & queries visible entries
  await assertSucceeds(
    studentA.collection("leaderboard").doc(STUDENT_A_UID).set({
      userId: STUDENT_A_UID,
      studentName: "Aarav Kumar",
      batch: "Navodaya-A",
      totalMarks: 85,
      averagePercentage: 85,
      testsCompleted: 1,
      categoryStats: { Navodaya: { totalMarks: 85, averagePercentage: 85, testsCompleted: 1 } },
      visible: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(studentA.collection("leaderboard").where("visible", "==", true).get());
  await assertFails(studentA.collection("leaderboard").get());
});
