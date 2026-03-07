package com.mathapp.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

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
        val operation = try { MathOperation.valueOf(opName) } catch (e: Exception) { return null }
        val attempts = loadAttempts(questionKey)
        if (attempts.isEmpty()) return null
        return QuestionStats(questionKey, operation, attempts)
    }

    fun getAllQuestionStats(operation: MathOperation): List<QuestionStats> {
        // Fix: filter by value == operation.name, reconstruct questionKey from key.removePrefix("op_")
        return prefs.all.entries
            .filter { (key, value) -> key.startsWith("op_") && value == operation.name }
            .mapNotNull { (key, _) ->
                val questionKey = key.removePrefix("op_")
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
        val text = questionKey.removePrefix("${operation.name}_")
        val num: Int? = when (operation) {
            MathOperation.MULTIPLICATION -> text.substringBefore(" \u00d7").toIntOrNull()
            MathOperation.DIVISION       -> text.substringAfterLast("\u00f7 ").trim().toIntOrNull()
            MathOperation.ADDITION       -> text.substringBefore(" + ").toIntOrNull()
            MathOperation.SUBTRACTION    -> text.substringBefore(" \u2212 ").toIntOrNull()
            MathOperation.POWERS         -> text.substringBefore("^").toIntOrNull()
            MathOperation.SQUARE_ROOTS   -> {
                val n = text.removePrefix("\u221a").toIntOrNull()
                n?.let { kotlin.math.sqrt(it.toDouble()).toInt() }
            }
            MathOperation.CUBE_ROOTS     -> {
                val n = text.removePrefix("\u221b").toIntOrNull()
                n?.let { Math.cbrt(it.toDouble()).toInt() }
            }
            MathOperation.FRACTIONS      -> text.substringAfterLast("/").toIntOrNull()
            MathOperation.PERCENTAGES    -> text.substringAfterLast("of ").trim().toIntOrNull()
            MathOperation.FACTORIZATION  -> text.toIntOrNull()
            MathOperation.MIXED          -> null
        }
        return num != null && num in range
    }

    // ── Group stats for Journey ──────────────────────────────────────────────

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

    // ── SRS (Spaced Repetition) ──────────────────────────────────────────────

    fun getSrsInterval(questionKey: String): Int =
        prefs.getInt(srsIntervalKey(questionKey), 0)

    fun getSrsDueDate(questionKey: String): Long =
        prefs.getLong(srsDueKey(questionKey), 0L)

    fun updateSrs(questionKey: String, wasCorrect: Boolean, timeMs: Long) {
        val prevInterval = getSrsInterval(questionKey)
        val nextInterval = SrsScheduler.nextIntervalDays(wasCorrect, SrsScheduler.isFast(timeMs), prevInterval)
        val nextDue = System.currentTimeMillis() + nextInterval * 24L * 60 * 60 * 1000
        prefs.edit()
            .putInt(srsIntervalKey(questionKey), nextInterval)
            .putLong(srsDueKey(questionKey), nextDue)
            .apply()
    }

    /** Returns a map of operation → count of SRS-due questions for that operation. */
    fun getDueQuestionCounts(operations: List<MathOperation>): Map<MathOperation, Int> {
        val now = System.currentTimeMillis()
        return operations.associateWith { op ->
            val prefix = "srs_due_${op.name}_"
            prefs.all.entries
                .filter { (key, value) -> key.startsWith(prefix) && value is Long }
                .count { (_, value) -> (value as Long) <= now }
        }
    }

    // ── Daily goal / progress / streak ───────────────────────────────────────

    fun getDailyGoal(): Int = prefs.getInt("daily_goal", 20)

    fun setDailyGoal(goal: Int) {
        prefs.edit().putInt("daily_goal", goal).apply()
    }

    fun getDailyProgressToday(): Int {
        val today = currentDateString()
        val progressDate = prefs.getString("daily_progress_date", null)
        return if (progressDate == today) {
            prefs.getInt("daily_progress", 0)
        } else {
            prefs.edit()
                .putString("daily_progress_date", today)
                .putInt("daily_progress", 0)
                .apply()
            0
        }
    }

    fun incrementDailyProgress() {
        getDailyProgressToday()  // ensure reset if date changed
        val newProgress = prefs.getInt("daily_progress", 0) + 1
        prefs.edit().putInt("daily_progress", newProgress).apply()
        checkAndUpdateStreak()
    }

    fun getStreak(): Int {
        refreshStreakIfBroken()
        return prefs.getInt("streak_count", 0)
    }

    private fun refreshStreakIfBroken() {
        val lastMet = prefs.getString("last_goal_met_date", null) ?: return
        val yesterday = previousDateString()
        if (lastMet < yesterday) {
            prefs.edit().putInt("streak_count", 0).apply()
        }
    }

    private fun checkAndUpdateStreak() {
        val today = currentDateString()
        val lastMet = prefs.getString("last_goal_met_date", null)
        if (lastMet == today) return  // already counted today
        val goalMet = getDailyProgressToday() >= getDailyGoal()
        if (!goalMet) return
        val yesterday = previousDateString()
        val currentStreak = prefs.getInt("streak_count", 0)
        val newStreak = if (lastMet == yesterday) currentStreak + 1 else 1
        prefs.edit()
            .putInt("streak_count", newStreak)
            .putString("last_goal_met_date", today)
            .apply()
    }

    // ── Speed records ────────────────────────────────────────────────────────

    fun updateSpeedRecord(operation: MathOperation, groupId: Int, avgTimeMs: Long) {
        val key = speedRecordKey(operation, groupId)
        val current = prefs.getLong(key, Long.MAX_VALUE)
        if (avgTimeMs < current) {
            prefs.edit().putLong(key, avgTimeMs).apply()
        }
    }

    fun getSpeedRecord(operation: MathOperation, groupId: Int): Long? {
        val value = prefs.getLong(speedRecordKey(operation, groupId), Long.MAX_VALUE)
        return if (value == Long.MAX_VALUE) null else value
    }

    // ── Onboarding ───────────────────────────────────────────────────────────

    fun isOnboardingComplete(): Boolean = prefs.getBoolean("onboarding_complete", false)

    fun setOnboardingComplete() {
        prefs.edit().putBoolean("onboarding_complete", true).apply()
    }

    // ── Date helpers ─────────────────────────────────────────────────────────

    private fun currentDateString(): String {
        val c = Calendar.getInstance()
        return "%04d-%02d-%02d".format(
            c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun previousDateString(): String {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_MONTH, -1)
        return "%04d-%02d-%02d".format(
            c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)
        )
    }

    // ── Key helpers ──────────────────────────────────────────────────────────

    private fun prefKey(questionKey: String)      = "q_$questionKey"
    private fun opKey(questionKey: String)        = "op_$questionKey"
    private fun sessionKey(op: MathOperation)     = "sessions_${op.name}"
    private fun excludedKey(op: MathOperation)    = "excluded_${op.name}"
    private fun milestonesKey(op: MathOperation)  = "milestones_${op.name}"
    private fun groupStatusKey(op: MathOperation, groupId: Int) = "gstatus_${op.name}_$groupId"
    private fun srsIntervalKey(questionKey: String) = "srs_interval_$questionKey"
    private fun srsDueKey(questionKey: String)      = "srs_due_$questionKey"
    private fun speedRecordKey(op: MathOperation, groupId: Int) = "speed_${op.name}_$groupId"

    companion object {
        private const val PREFS_NAME = "mathapp_progress"
    }
}
