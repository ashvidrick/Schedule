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
    @Query("SELECT * FROM rules WHERE scheduleId = :scheduleId")
    fun observeForSchedule(scheduleId: Long): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules WHERE scheduleId = :scheduleId")
    suspend fun getForSchedule(scheduleId: Long): List<RuleEntity>

    @Insert suspend fun insert(r: RuleEntity): Long
    @Update suspend fun update(r: RuleEntity)
    @Delete suspend fun delete(r: RuleEntity)

    @Query("DELETE FROM rules WHERE scheduleId = :scheduleId")
    suspend fun deleteForSchedule(scheduleId: Long)
}