package com.mathapp.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class ProgressRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Answer recording ────────────────────────────────────────────────────

    fun recordAnswer(questionKey: String, operation: MathOperation, isCorrect: Boolean, timeMs: Long) {
        val existing = loadAttempts(questionKey).toMutableList()
        existing.add(Attempt(isCorrect, timeMs))
        val trimmed = if (existing.size > 5) existing.takeLast(5) else existing
        saveAttempts(questionKey, operation, trimmed)
    }

    private fun loadAttempts(questionKey: String): List<Attempt> {
        val json = prefs.getString(prefKey(questionKey), null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Attempt(obj.getBoolean("correct"), obj.getLong("timeMs"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveAttempts(questionKey: String, operation: MathOperation, attempts: List<Attempt>) {
        val arr = JSONArray()
        attempts.forEach { attempt ->
            arr.put(JSONObject().apply {
                put("correct", attempt.isCorrect)
                put("timeMs", attempt.timeMs)
            })
        }
        prefs.edit()
            .putString(prefKey(questionKey), arr.toString())
            .putString(opKey(questionKey), operation.name)
            .apply()
    }

    fun getQuestionStats(questionKey: String): QuestionStats? {
        val opName = prefs.getString(opKey(questionKey), null) ?: return null
        val operation = MathOperation.valueOf(opName)
        val attempts = loadAttempts(questionKey)
        if (attempts.isEmpty()) return null
        return QuestionStats(questionKey, operation, attempts)
    }

    fun getAllQuestionStats(operation: MathOperation): List<QuestionStats> {
        val prefix = "op_${operation.name}_"
        return prefs.all.keys
            .filter { it.startsWith(prefix) }
            .mapNotNull { key ->
                val questionKey = key.removePrefix(prefix)
                getQuestionStats(questionKey)
            }
    }

    fun getStrugglingCount(operation: MathOperation, groupIds: List<Int>): Int =
        getStrugglingKeys(operation, groupIds).size

    fun getStrugglingKeys(operation: MathOperation, groupIds: List<Int>): Set<String> {
        val allStats = getAllQuestionStats(operation)
        return allStats
            .filter { stats ->
                stats.isStruggling && groupIds.any { id -> isKeyInGroup(stats.questionKey, operation, id) }
            }
            .map { it.questionKey }
            .toSet()
    }

    private fun isKeyInGroup(questionKey: String, operation: MathOperation, groupId: Int): Boolean {
        val range = QuestionGroups.groupRange(groupId)
        val text = questionKey.substringAfter("_")
        val num = when (operation) {
            MathOperation.MULTIPLICATION -> text.substringBefore(" \u00d7").toIntOrNull()
            MathOperation.DIVISION       -> text.substringAfterLast("\u00f7 ").trim().toIntOrNull()
            MathOperation.POWERS         -> text.substringBefore("^").toIntOrNull()
            MathOperation.FRACTIONS      -> text.substringAfterLast("/").toIntOrNull()
        }
        return num != null && num in range
    }

    // ── Group stats for Journey ──────────────────────────────────────────────

    /** Returns (accuracy 0–100, totalAttempts). Accuracy = -1 if no data. */
    fun getGroupAccuracy(operation: MathOperation, groupId: Int): Pair<Float, Int> {
        val groupStats = getAllQuestionStats(operation)
            .filter { isKeyInGroup(it.questionKey, operation, groupId) }
        val totalAttempts = groupStats.sumOf { it.recentAttempts.size }
        if (totalAttempts == 0) return Pair(-1f, 0)
        val correct = groupStats.sumOf { stats -> stats.recentAttempts.count { it.isCorrect } }
        return Pair(correct.toFloat() / totalAttempts.toFloat() * 100f, totalAttempts)
    }

    // ── Excluded numbers ─────────────────────────────────────────────────────

    fun getExcludedNumbers(operation: MathOperation): Set<Int> {
        val json = prefs.getString(excludedKey(operation), null) ?: return emptySet()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getInt(it) }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    fun setExcludedNumbers(operation: MathOperation, excluded: Set<Int>) {
        val arr = JSONArray()
        excluded.forEach { arr.put(it) }
        prefs.edit().putString(excludedKey(operation), arr.toString()).apply()
    }

    // ── Session history ──────────────────────────────────────────────────────

    fun recordSession(record: SessionRecord) {
        val existing = getSessionHistory(record.operation).toMutableList()
        existing.add(record)
        val arr = JSONArray()
        existing.forEach { r ->
            arr.put(JSONObject().apply {
                put("ts", r.timestampMs)
                put("groups", JSONArray(r.selectedGroups))
                put("struggling", r.strugglingOnly)
                put("correct", r.correct)
                put("total", r.total)
                put("avgTime", r.avgTimeMs)
            })
        }
        prefs.edit()
            .putString(sessionKey(record.operation), arr.toString())
            .apply()
    }

    fun getSessionHistory(operation: MathOperation): List<SessionRecord> {
        val json = prefs.getString(sessionKey(operation), null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val groupsArr = obj.getJSONArray("groups")
                val groups = (0 until groupsArr.length()).map { groupsArr.getInt(it) }
                SessionRecord(
                    timestampMs    = obj.getLong("ts"),
                    operation      = operation,
                    selectedGroups = groups,
                    strugglingOnly = obj.getBoolean("struggling"),
                    correct        = obj.getInt("correct"),
                    total          = obj.getInt("total"),
                    avgTimeMs      = obj.getLong("avgTime")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getOverallAccuracy(operation: MathOperation): Float {
        val history = getSessionHistory(operation)
        val total = history.sumOf { it.total }
        val correct = history.sumOf { it.correct }
        return if (total == 0) 0f else correct.toFloat() / total.toFloat() * 100f
    }

    // ── Milestones ───────────────────────────────────────────────────────────

    fun getMilestones(operation: MathOperation): List<Milestone> {
        val json = prefs.getString(milestonesKey(operation), null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.getJSONObject(i)
                val status = try { GroupStatus.valueOf(obj.getString("status")) } catch (e: Exception) { return@mapNotNull null }
                Milestone(obj.getLong("ts"), operation, obj.getInt("groupId"), status)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveMilestones(operation: MathOperation, milestones: List<Milestone>) {
        val arr = JSONArray()
        milestones.forEach { m ->
            arr.put(JSONObject().apply {
                put("ts", m.timestampMs)
                put("groupId", m.groupId)
                put("status", m.newStatus.name)
            })
        }
        prefs.edit().putString(milestonesKey(operation), arr.toString()).apply()
    }

    fun getLastGroupStatus(operation: MathOperation, groupId: Int): GroupStatus {
        val name = prefs.getString(groupStatusKey(operation, groupId), null) ?: return GroupStatus.NONE
        return try { GroupStatus.valueOf(name) } catch (e: Exception) { GroupStatus.NONE }
    }

    private fun saveGroupStatus(operation: MathOperation, groupId: Int, status: GroupStatus) {
        prefs.edit().putString(groupStatusKey(operation, groupId), status.name).apply()
    }

    fun checkAndRecordMilestones(operation: MathOperation, groupIds: List<Int>) {
        val newMilestones = mutableListOf<Milestone>()
        val now = System.currentTimeMillis()
        for (groupId in groupIds) {
            val (accuracy, attempted) = getGroupAccuracy(operation, groupId)
            if (attempted == 0) continue
            val currentStatus = when {
                accuracy >= 80f -> GroupStatus.GREEN
                accuracy >= 50f -> GroupStatus.YELLOW
                else            -> GroupStatus.RED
            }
            val prevStatus = getLastGroupStatus(operation, groupId)
            if (currentStatus > prevStatus) {
                if (currentStatus == GroupStatus.YELLOW || currentStatus == GroupStatus.GREEN) {
                    newMilestones.add(Milestone(now, operation, groupId, currentStatus))
                }
                saveGroupStatus(operation, groupId, currentStatus)
            }
        }
        if (newMilestones.isNotEmpty()) {
            val existing = getMilestones(operation).toMutableList()
            existing.addAll(newMilestones)
            saveMilestones(operation, existing)
        }
    }

    // ── Key helpers ──────────────────────────────────────────────────────────

    private fun prefKey(questionKey: String)  = "q_$questionKey"
    private fun opKey(questionKey: String)    = "op_$questionKey"
    private fun sessionKey(op: MathOperation) = "sessions_${op.name}"
    private fun excludedKey(op: MathOperation) = "excluded_${op.name}"
    private fun milestonesKey(op: MathOperation) = "milestones_${op.name}"
    private fun groupStatusKey(op: MathOperation, groupId: Int) = "gstatus_${op.name}_$groupId"

    companion object {
        private const val PREFS_NAME = "mathapp_progress"
    }
}
