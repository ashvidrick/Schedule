package com.example.workschedule.data

import androidx.room.*

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Одна "пара" (условие + диапазон времени).
 * У одной записи может быть несколько.
 */
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
    val conditionType: ConditionType,
    /** JSON-строка с параметрами условия. Формат зависит от типа. */
    val conditionParams: String,
    /** Минуты от начала суток, напр. 9*60 = 540 */
    val timeStartMinutes: Int,
    val timeEndMinutes: Int
)