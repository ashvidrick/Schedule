package com.example.workschedule.domain

import com.example.workschedule.data.RuleEntity
import java.util.Calendar

object ScheduleCalculator {

    /**
     * Возвращает список интервалов (start..end в минутах) на конкретный день,
     * если хотя бы одно правило срабатывает. Пустой список = нерабочий день.
     */
    fun intervalsForDay(rules: List<RuleEntity>, date: Calendar): List<IntRange> {
        return rules
            .filter { ConditionEvaluator.matches(it, date) }
            .map { it.timeStartMinutes until it.timeEndMinutes }
            .sortedBy { it.first }
    }

    /** Суммарные рабочие минуты за день. */
    fun minutesForDay(rules: List<RuleEntity>, date: Calendar): Int =
        intervalsForDay(rules, date).sumOf { it.last - it.first + 1 }
}