package com.example.workschedule.domain

import com.example.workschedule.data.ConditionType
import com.example.workschedule.data.RuleEntity
import org.json.JSONObject
import java.util.Calendar

/**
 * Проверяет, срабатывает ли правило в конкретную дату.
 * Время суток проверяется отдельно (см. isWithinTime).
 */
object ConditionEvaluator {

    fun matches(rule: RuleEntity, date: Calendar): Boolean {
        val p = JSONObject(rule.conditionParams)
        return when (rule.conditionType) {

            ConditionType.WEEKDAY -> {
                // params: {"days":[1,2,3,4,5]}  // 1=Mon..7=Sun (ISO)
                val days = p.getJSONArray("days").let { arr ->
                    (0 until arr.length()).map { arr.getInt(it) }
                }
                days.contains(isoDayOfWeek(date))
            }

            ConditionType.DAY_OF_MONTH -> {
                // params: {"days":[1,15,31]}
                val days = p.getJSONArray("days").let { arr ->
                    (0 until arr.length()).map { arr.getInt(it) }
                }
                days.contains(date.get(Calendar.DAY_OF_MONTH))
            }

            ConditionType.EVERY_N_DAYS -> {
                // params: {"n":3,"offset":0}  // каждый 3-й день месяца начиная с offset
                val n = p.getInt("n")
                val offset = p.optInt("offset", 0)
                val dom = date.get(Calendar.DAY_OF_MONTH)
                n > 0 && dom > offset && (dom - offset - 1) % n == 0
            }

            ConditionType.EVERY_N_WEEKDAY -> {
                // params: {"weekday":1,"n":2}  // каждый 2-й понедельник месяца
                val weekday = p.getInt("weekday")
                val n = p.getInt("n")
                if (isoDayOfWeek(date) != weekday) return false
                val occurrence = (date.get(Calendar.DAY_OF_MONTH) - 1) / 7 + 1
                occurrence == n
            }

            ConditionType.NTH_WEEKDAY_OF_MONTH -> {
                // params: {"weekday":1,"nth":3}  // 3-й понедельник месяца
                // То же, что EVERY_N_WEEKDAY, но семантически иначе.
                val weekday = p.getInt("weekday")
                val nth = p.getInt("nth")
                if (isoDayOfWeek(date) != weekday) return false
                val occurrence = (date.get(Calendar.DAY_OF_MONTH) - 1) / 7 + 1
                occurrence == nth
            }

            ConditionType.DATE_RANGE -> {
                // params: {"from":"2026-01-01","to":"2026-12-31"}
                val from = p.getString("from")
                val to = p.getString("to")
                val iso = isoDate(date)
                iso >= from && iso <= to
            }

            ConditionType.PLUGIN -> {
                // заглушка: сюда позже придёт вызов JAR-плагина
                PluginRegistry.evaluate(rule.conditionParams, date)
            }
        }
    }

    /** Проверка времени суток. */
    fun isWithinTime(rule: RuleEntity, minuteOfDay: Int): Boolean =
        minuteOfDay in rule.timeStartMinutes until rule.timeEndMinutes

    private fun isoDayOfWeek(c: Calendar): Int {
        // Calendar.MONDAY=2..SUNDAY=1 → ISO 1..7
        return when (c.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            else -> 7
        }
    }

    private fun isoDate(c: Calendar): String {
        val y = c.get(Calendar.YEAR)
        val m = c.get(Calendar.MONTH) + 1
        val d = c.get(Calendar.DAY_OF_MONTH)
        return "%04d-%02d-%02d".format(y, m, d)
    }
}