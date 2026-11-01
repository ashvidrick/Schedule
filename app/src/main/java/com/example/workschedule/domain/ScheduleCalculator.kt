package com.example.workschedule.domain

import com.example.workschedule.data.RuleWithIntervals
import java.util.Calendar

object ScheduleCalculator {

    fun intervalsForDay(rules: List<RuleWithIntervals>, date: Calendar): List<IntRange> {
        val matched = rules.filter { ConditionEvaluator.matches(it.rule, date) }
        if (matched.isEmpty()) return emptyList()

        val exceptions = matched.filter { it.rule.isException }

        if (exceptions.isNotEmpty()) {
            // Если хотя бы одно исключение не имеет рабочих интервалов — день закрыт
            val anyClosed = exceptions.any { ex ->
                ex.intervals.none { !it.isBreak }
            }
            if (anyClosed) return emptyList()

            // Иначе складываем все исключения
            val result = mutableListOf<IntRange>()
            for (rw in exceptions) {
                val work = rw.intervals.filter { !it.isBreak }
                    .map { it.startMinutes until (it.startMinutes + it.durationMinutes) }
                val breaks = rw.intervals.filter { it.isBreak }
                    .map { it.startMinutes until (it.startMinutes + it.durationMinutes) }
                result += subtract(work, breaks)
            }
            return mergeIntervals(result.sortedBy { it.first })
        }

        // Обычные правила
        val result = mutableListOf<IntRange>()
        for (rw in matched) {
            val work = rw.intervals.filter { !it.isBreak }
                .map { it.startMinutes until (it.startMinutes + it.durationMinutes) }
            val breaks = rw.intervals.filter { it.isBreak }
                .map { it.startMinutes until (it.startMinutes + it.durationMinutes) }
            result += subtract(work, breaks)
        }
        return mergeIntervals(result.sortedBy { it.first })
    }

    fun minutesForDay(rules: List<RuleWithIntervals>, date: Calendar): Int =
        intervalsForDay(rules, date).sumOf { it.last - it.first + 1 }

    /**
     * Вычитает перерывы из рабочих интервалов.
     * Все интервалы — полуоткрытые [start, end) в минутах.
     */
    private fun subtract(work: List<IntRange>, breaks: List<IntRange>): List<IntRange> {
        if (breaks.isEmpty()) return work
        var current = work
        for (b in breaks) {
            val bStart = b.first
            val bEnd = b.last// + 1 // полуоткрытый
            val next = mutableListOf<IntRange>()
            for (w in current) {
                val wStart = w.first
                val wEnd = w.last// + 1
                if (bEnd <= wStart || bStart >= wEnd) {
                    next += w
                } else {
                    if (wStart < bStart) next += wStart until bStart
                    if (bEnd < wEnd) next += bEnd until wEnd
                }
            }
            current = next
        }
        // превратить полуоткрытые [start, end) обратно в закрытые [start, end]
        return current.map { it.first..(it.last /*- 1*/) }
    }

    private fun mergeIntervals(intervals: List<IntRange>): List<IntRange> {
        if (intervals.isEmpty()) return emptyList()
        val sorted = intervals.sortedBy { it.first }
        val result = mutableListOf(sorted.first())
        for (i in 1 until sorted.size) {
            val last = result.last()
            val cur = sorted[i]
            if (cur.first <= last.last + 1) {
                result[result.lastIndex] = last.first..maxOf(last.last, cur.last)
            } else {
                result.add(cur)
            }
        }
        return result
    }
}