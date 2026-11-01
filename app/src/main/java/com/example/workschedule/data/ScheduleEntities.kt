package com.example.workschedule.data

import androidx.room.*

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "rules",
    foreignKeys = [ForeignKey(
        entity = ScheduleEntity::class,
        parentColumns = ["id"],
        childColumns = ["scheduleId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("scheduleId")]
)
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scheduleId: Long,
    val label: String = "",   // ← новое
    val isException: Boolean = false,   // ← НОВОЕ
    val conditionType: ConditionType,
    val conditionParams: String
)

@Entity(
    tableName = "intervals",
    foreignKeys = [ForeignKey(
        entity = RuleEntity::class,
        parentColumns = ["id"],
        childColumns = ["ruleId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("ruleId")]
)
data class IntervalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ruleId: Long,
    val label: String = "",   // ← новое
    val startMinutes: Int,
    val durationMinutes: Int,
    val isBreak: Boolean = false,
    val orderIndex: Int = 0
)

data class RuleWithIntervals(
    @Embedded val rule: RuleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "ruleId"
    )
    val intervals: List<IntervalEntity>
)