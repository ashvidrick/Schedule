package com.example.workschedule.domain

import com.example.workschedule.data.ConditionType
import com.example.workschedule.data.RuleEntity
import org.json.JSONObject
import java.util.Calendar

object ConditionEvaluator {

    fun matches(rule: RuleEntity, date: Calendar): Boolean {
        return try {
            val p = JSONObject(rule.conditionParams)
            when (rule.conditionType) {
                ConditionType.WEEKDAY -> {
                    val days = p.getJSONArray("days").let { arr ->
                        (0 until arr.length()).map { arr.getInt(it) }
                    }
                    days.contains(isoDayOfWeek(date))
                }
                ConditionType.DAY_OF_MONTH -> {
                    val days = p.getJSONArray("days").let { arr ->
                        (0 until arr.length()).map { arr.getInt(it) }
                    }
                    days.contains(date.get(Calendar.DAY_OF_MONTH))
                }
                ConditionType.EVERY_N_DAYS -> {
                    val n = p.getInt("n")
                    val offset = p.optInt("offset", 0)
                    val dom = date.get(Calendar.DAY_OF_MONTH)
                    n > 0 && dom > offset && (dom - offset - 1) % n == 0
                }
                ConditionType.EVERY_N_WEEKDAY -> {
                    val weekday = p.getInt("weekday")
                    val n = p.getInt("n")
                    if (isoDayOfWeek(date) != weekday) return false
                    val occurrence = (date.get(Calendar.DAY_OF_MONTH) - 1) / 7 + 1
                    occurrence == n
                }
                ConditionType.NTH_WEEKDAY_OF_MONTH -> {
                    val weekday = p.getInt("weekday")
                    val nth = p.getInt("nth")
                    if (isoDayOfWeek(date) != weekday) return false
                    val occurrence = (date.get(Calendar.DAY_OF_MONTH) - 1) / 7 + 1
                    occurrence == nth
                }
                ConditionType.DATE_RANGE -> {
                    val from = p.getString("from")
                    val to = p.getString("to")
                    val iso = isoDate(date)
                    iso >= from && iso <= to
                }
                ConditionType.PLUGIN -> PluginRegistry.evaluate(rule.conditionParams, date)
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun isoDayOfWeek(c: Calendar): Int = when (c.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        else -> 7
    }

    private fun isoDate(c: Calendar): String {
        val y = c.get(Calendar.YEAR)
        val m = c.get(Calendar.MONTH) + 1
        val d = c.get(Calendar.DAY_OF_MONTH)
        return "%04d-%02d-%02d".format(y, m, d)
    }
}