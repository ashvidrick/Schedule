package com.example.workschedule.data

import kotlinx.coroutines.flow.Flow

class ScheduleRepository(
    private val scheduleDao: ScheduleDao,
    private val ruleDao: RuleDao
) {
    fun observeAll(): Flow<List<ScheduleEntity>> = scheduleDao.observeAll()

    fun observeRules(scheduleId: Long): Flow<List<RuleEntity>> =
        ruleDao.observeForSchedule(scheduleId)

    suspend fun getSchedule(id: Long) = scheduleDao.getById(id)
    suspend fun getRules(id: Long) = ruleDao.getForSchedule(id)

    suspend fun save(schedule: ScheduleEntity, rules: List<RuleEntity>): Long {
        val id = if (schedule.id == 0L) {
            scheduleDao.insert(schedule)
        } else {
            scheduleDao.update(schedule); schedule.id
        }
        ruleDao.deleteForSchedule(id)
        rules.forEach { ruleDao.insert(it.copy(id = 0, scheduleId = id)) }
        return id
    }

    suspend fun delete(schedule: ScheduleEntity) = scheduleDao.delete(schedule)
}