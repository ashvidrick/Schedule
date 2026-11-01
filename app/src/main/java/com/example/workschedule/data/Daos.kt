package com.example.workschedule.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getById(id: Long): ScheduleEntity?

    @Insert suspend fun insert(s: ScheduleEntity): Long
    @Update suspend fun update(s: ScheduleEntity)
    @Delete suspend fun delete(s: ScheduleEntity)
}

@Dao
interface RuleDao {
    @Transaction
    @Query("SELECT * FROM rules WHERE scheduleId = :scheduleId ORDER BY id ASC")
    fun observeForSchedule(scheduleId: Long): Flow<List<RuleWithIntervals>>

    @Transaction
    @Query("SELECT * FROM rules WHERE scheduleId = :scheduleId ORDER BY id ASC")
    suspend fun getForSchedule(scheduleId: Long): List<RuleWithIntervals>

    @Insert suspend fun insertRule(r: RuleEntity): Long
    @Update suspend fun updateRule(r: RuleEntity)
    @Query("DELETE FROM rules WHERE id = :ruleId")
    suspend fun deleteRuleById(ruleId: Long)

    @Query("DELETE FROM rules WHERE scheduleId = :scheduleId")
    suspend fun deleteRulesForSchedule(scheduleId: Long)

    @Insert suspend fun insertInterval(i: IntervalEntity): Long
    @Query("DELETE FROM intervals WHERE ruleId = :ruleId")
    suspend fun deleteIntervalsForRule(ruleId: Long)
}