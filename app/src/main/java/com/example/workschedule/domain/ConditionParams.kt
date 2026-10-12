package com.example.workschedule.domain

import com.example.workschedule.data.ConditionType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Разобранные параметры условия. Один sealed-класс на все типы.
 * Используется и для чтения (парсинг JSON), и для записи (сборка JSON).
 */
sealed interface ConditionParams {

    data class Weekdays(val days: Set<Int>) : ConditionParams
    data class DaysOfMonth(val days: Set<Int>) : ConditionParams
    data class EveryNDays(val n: Int, val offset: Int) : ConditionParams
    data class EveryNWeekday(val weekday: Int, val n: Int) : ConditionParams
    data class NthWeekdayOfMonth(val weekday: Int, val nth: Int) : ConditionParams
    data class DateRange(val from: String, val to: String) : ConditionParams
    data object Plugin : ConditionParams

    companion object {

        /** Безопасный парсинг: никогда не бросает, возвращает дефолт. */
        fun parse(type: ConditionType, json: String): ConditionParams {
            return try {
                val o = JSONObject(json)
                when (type) {
                    ConditionType.WEEKDAY -> Weekdays(
                        days = o.optJSONArray("days").toIntSet()
                    )
                    ConditionType.DAY_OF_MONTH -> DaysOfMonth(
                        days = o.optJSONArray("days").toIntSet()
                    )
                    ConditionType.EVERY_N_DAYS -> EveryNDays(
                        n = o.optInt("n", 2),
                        offset = o.optInt("offset", 0)
                    )
                    ConditionType.EVERY_N_WEEKDAY -> EveryNWeekday(
                        weekday = o.optInt("weekday", 1),
                        n = o.optInt("n", 1)
                    )
                    ConditionType.NTH_WEEKDAY_OF_MONTH -> NthWeekdayOfMonth(
                        weekday = o.optInt("weekday", 1),
                        nth = o.optInt("nth", 1)
                    )
                    ConditionType.DATE_RANGE -> DateRange(
                        from = o.optString("from", "2026-01-01"),
                        to = o.optString("to", "2026-12-31")
                    )
                    ConditionType.PLUGIN -> Plugin
                }
            } catch (_: Exception) {
                default(type)
            }
        }

        /** Дефолт для нового правила. */
        fun default(type: ConditionType): ConditionParams = when (type) {
            ConditionType.WEEKDAY -> Weekdays(setOf(1, 2, 3, 4, 5))
            ConditionType.DAY_OF_MONTH -> DaysOfMonth(setOf(1))
            ConditionType.EVERY_N_DAYS -> EveryNDays(n = 2, offset = 0)
            ConditionType.EVERY_N_WEEKDAY -> EveryNWeekday(weekday = 1, n = 1)
            ConditionType.NTH_WEEKDAY_OF_MONTH -> NthWeekdayOfMonth(weekday = 1, nth = 1)
            ConditionType.DATE_RANGE -> DateRange("2026-01-01", "2026-12-31")
            ConditionType.PLUGIN -> Plugin
        }

        /** Сборка JSON. Всегда валиден, потому что строит JSONObject. */
        fun toJson(params: ConditionParams): String = when (params) {
            is Weekdays -> JSONObject().put("days", JSONArray(params.days.sorted())).toString()
            is DaysOfMonth -> JSONObject().put("days", JSONArray(params.days.sorted())).toString()
            is EveryNDays -> JSONObject()
                .put("n", params.n)
                .put("offset", params.offset)
                .toString()
            is EveryNWeekday -> JSONObject()
                .put("weekday", params.weekday)
                .put("n", params.n)
                .toString()
            is NthWeekdayOfMonth -> JSONObject()
                .put("weekday", params.weekday)
                .put("nth", params.nth)
                .toString()
            is DateRange -> JSONObject()
                .put("from", params.from)
                .put("to", params.to)
                .toString()
            Plugin -> "{}"
        }

        private fun JSONArray?.toIntSet(): Set<Int> {
            if (this == null) return emptySet()
            return (0 until length()).mapNotNull { i ->
                optInt(i).takeIf { it != 0 }
            }.toSet()
        }
    }
}