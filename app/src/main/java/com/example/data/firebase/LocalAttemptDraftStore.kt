package com.example.data.firebase

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class LocalAttemptDraft(
    val testId: String,
    val answers: Map<String, Int>,
    val markedForReview: Set<String>,
    val currentQuestionIndex: Int,
    val remainingSeconds: Long,
    val elapsedSeconds: Long,
    val savedAtMillis: Long
)

/**
 * Local persistent draft cache ensuring zero progress loss if app restarts mid-exam.
 */
class LocalAttemptDraftStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "mbs_exam_attempt_drafts",
        Context.MODE_PRIVATE
    )

    private fun key(uid: String, testId: String) = "draft_${uid}_${testId}"

    fun saveDraft(uid: String, draft: LocalAttemptDraft) {
        val answersJson = JSONObject()
        draft.answers.forEach { (qId, opt) -> answersJson.put(qId, opt) }
        val reviewJson = JSONArray()
        draft.markedForReview.forEach { reviewJson.put(it) }

        val root = JSONObject().apply {
            put("testId", draft.testId)
            put("answers", answersJson)
            put("markedForReview", reviewJson)
            put("currentQuestionIndex", draft.currentQuestionIndex)
            put("remainingSeconds", draft.remainingSeconds)
            put("elapsedSeconds", draft.elapsedSeconds)
            put("savedAtMillis", draft.savedAtMillis)
        }
        prefs.edit().putString(key(uid, draft.testId), root.toString()).apply()
    }

    fun loadDraft(uid: String, testId: String): LocalAttemptDraft? {
        val raw = prefs.getString(key(uid, testId), null) ?: return null
        return try {
            val root = JSONObject(raw)
            val answersObj = root.optJSONObject("answers") ?: JSONObject()
            val answers = mutableMapOf<String, Int>()
            answersObj.keys().forEach { k -> answers[k] = answersObj.optInt(k, -1) }
            val reviewArr = root.optJSONArray("markedForReview") ?: JSONArray()
            val reviewSet = mutableSetOf<String>()
            for (i in 0 until reviewArr.length()) {
                reviewSet.add(reviewArr.optString(i))
            }
            LocalAttemptDraft(
                testId = testId,
                answers = answers.filterValues { it >= 0 },
                markedForReview = reviewSet,
                currentQuestionIndex = root.optInt("currentQuestionIndex", 0),
                remainingSeconds = root.optLong("remainingSeconds", 0L),
                elapsedSeconds = root.optLong("elapsedSeconds", 0L),
                savedAtMillis = root.optLong("savedAtMillis", 0L)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun clearDraft(uid: String, testId: String) {
        prefs.edit().remove(key(uid, testId)).apply()
    }
}
