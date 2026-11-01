package com.example.workschedule.data

import kotlinx.coroutines.flow.Flow

class ScheduleRepository(
    private val scheduleDao: ScheduleDao,
    private val ruleDao: RuleDao
) {
    fun observeAll(): Flow<List<ScheduleEntity>> = scheduleDao.observeAll()

    fun observeRules(scheduleId: Long): Flow<List<RuleWithIntervals>> =
        ruleDao.observeForSchedule(scheduleId)

    suspend fun getSchedule(id: Long) = scheduleDao.getById(id)
    suspend fun getRules(id: Long): List<RuleWithIntervals> =
        ruleDao.getForSchedule(id)

    suspend fun save(schedule: ScheduleEntity, rules: List<RuleWithIntervals>): Long {
        val id = if (schedule.id == 0L) {
            scheduleDao.insert(schedule)
        } else {
            scheduleDao.update(schedule); schedule.id
        }
        ruleDao.deleteRulesForSchedule(id)
        rules.forEach { rw ->
            val newRuleId = ruleDao.insertRule(rw.rule.copy(id = 0, scheduleId = id))
            rw.intervals.forEach { iv ->
                ruleDao.insertInterval(
                    iv.copy(id = 0, ruleId = newRuleId)
                )
            }
        }
        return id
    }

    suspend fun delete(schedule: ScheduleEntity) = scheduleDao.delete(schedule)
}