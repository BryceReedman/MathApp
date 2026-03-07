package com.mathapp.data

data class Milestone(
    val timestampMs: Long,
    val operation: MathOperation,
    val groupId: Int,
    val newStatus: GroupStatus
)

// Ordered by progression: NONE < RED < YELLOW < GREEN
enum class GroupStatus { NONE, RED, YELLOW, GREEN }
