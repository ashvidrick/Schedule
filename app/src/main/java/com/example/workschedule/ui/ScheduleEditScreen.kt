package com.example.workschedule.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.workschedule.data.AppDatabase
import com.example.workschedule.data.ConditionType
import com.example.workschedule.data.IntervalEntity
import com.example.workschedule.data.RuleEntity
import com.example.workschedule.data.RuleWithIntervals
import com.example.workschedule.data.ScheduleEntity
import com.example.workschedule.data.ScheduleRepository
import com.example.workschedule.domain.ConditionEvaluator
import com.example.workschedule.domain.ConditionParams
import com.example.workschedule.domain.ScheduleCalculator
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

// --- Saver для списка правил ---

private fun ruleWithIntervalsToJson(rw: RuleWithIntervals): String {
    val o = JSONObject()
    o.put("id", rw.rule.id)
    o.put("scheduleId", rw.rule.scheduleId)
    o.put("conditionType", rw.rule.conditionType.name)
    o.put("conditionParams", rw.rule.conditionParams)
    val intervals = JSONArray()
    rw.intervals.forEach { iv ->
        intervals.put(JSONObject().apply {
            put("id", iv.id)
            put("ruleId", iv.ruleId)
            put("startMinutes", iv.startMinutes)
            put("durationMinutes", iv.durationMinutes)
            put("isBreak", iv.isBreak)
            put("orderIndex", iv.orderIndex)
        })
    }
    o.put("intervals", intervals)
    return o.toString()
}

private fun ruleWithIntervalsFromJson(json: String): RuleWithIntervals {
    val o = JSONObject(json)
    val rule = RuleEntity(
        id = o.getLong("id"),
        scheduleId = o.getLong("scheduleId"),
        conditionType = ConditionType.valueOf(o.getString("conditionType")),
        conditionParams = o.getString("conditionParams")
    )
    val intervals = mutableListOf<IntervalEntity>()
    val arr = o.getJSONArray("intervals")
    for (i in 0 until arr.length()) {
        val io = arr.getJSONObject(i)
        intervals += IntervalEntity(
            id = io.getLong("id"),
            ruleId = io.getLong("ruleId"),
            startMinutes = io.getInt("startMinutes"),
            durationMinutes = io.getInt("durationMinutes"),
            isBreak = io.getBoolean("isBreak"),
            orderIndex = io.getInt("orderIndex")
        )
    }
    return RuleWithIntervals(rule, intervals)
}

private val RulesListSaver = Saver<List<RuleWithIntervals>, ArrayList<String>>(
    save = { list -> ArrayList(list.map { ruleWithIntervalsToJson(it) }) },
    restore = { saved -> saved.map { ruleWithIntervalsFromJson(it) } }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditScreen(
    scheduleId: Long,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val repo = remember {
        val db = AppDatabase.get(ctx)
        ScheduleRepository(db.scheduleDao(), db.ruleDao())
    }
    val scope = rememberCoroutineScope()

    // rememberSaveable — переживает поворот и смерть процесса
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var rules by rememberSaveable(stateSaver = RulesListSaver) {
        mutableStateOf(listOf<RuleWithIntervals>())
    }
    var tempIdCounter by rememberSaveable { mutableStateOf(-1L) }

    // Флаг "данные уже загружены из БД". Тоже переживает поворот.
    var loaded by rememberSaveable { mutableStateOf(false) }

    // LaunchedEffect(Unit) перезапустится при повороте,
    // но loaded = true (восстановился из rememberSaveable) не даст перезаписать данные.
    LaunchedEffect(Unit) {
        if (!loaded) {
            if (scheduleId != 0L) {
                val s = repo.getSchedule(scheduleId)
                if (s != null) {
                    title = s.title
                    description = s.description
                }
                rules = repo.getRules(scheduleId)
            }
            loaded = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (scheduleId == 0L) "Новая запись" else "Редактирование") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        scope.launch {
                            repo.save(
                                ScheduleEntity(
                                    id = scheduleId,
                                    title = title.ifBlank { "Без названия" },
                                    description = description
                                ),
                                rules
                            )
                            onBack()
                        }
                    }) { Text("Сохранить") }
                }
            )
        }
    ) { pad ->
        if (!loaded) return@Scaffold

        LazyColumn(
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Правила", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    Button(onClick = {
                        val newRuleId = tempIdCounter--
                        val defaultParams = """{"days":[1,2,3,4,5]}"""
                        val defaultLabel = describeCondition(
                            ConditionType.WEEKDAY,
                            ConditionParams.parse(ConditionType.WEEKDAY, defaultParams)
                        )
                        rules = rules + RuleWithIntervals(
                            rule = RuleEntity(
                                id = newRuleId,
                                scheduleId = scheduleId,
                                label = defaultLabel,          // ← дефолтное название
                                conditionType = ConditionType.WEEKDAY,
                                conditionParams = defaultParams
                            ),
                            intervals = listOf(
                                IntervalEntity(
                                    id = tempIdCounter--,
                                    ruleId = newRuleId,
                                    startMinutes = 9 * 60,
                                    durationMinutes = 9 * 60,
                                    isBreak = false,
                                    orderIndex = 0
                                )
                            )
                        )
                    }) { Text("Добавить") }
                }
            }
            itemsIndexed(rules, key = { _, rw -> rw.rule.id }) { idx, rw ->
                RuleCard(
                    rw = rw,
                    onChange = { newRw ->
                        rules = rules.toMutableList().also { it[idx] = newRw }
                    },
                    onRemove = {
                        rules = rules.toMutableList().also { it.removeAt(idx) }
                    }
                )
            }
            if (rules.isNotEmpty()) {
                item { PreviewCard(rules = rules) }
            }
        }
    }
}

@Composable
private fun PreviewCard(rules: List<RuleWithIntervals>) {
    val cal = Calendar.getInstance()
    val rows = (0..6).map {
        val c = cal.clone() as Calendar
        c.add(Calendar.DAY_OF_MONTH, it)
        val minutes = ScheduleCalculator.minutesForDay(rules, c)
        val matched = rules.filter { ConditionEvaluator.matches(it.rule, c) }
        val dateStr = "%04d-%02d-%02d".format(
            c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)
        )
        Triple(dateStr, minutes, matched)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Предпросмотр (7 дней)", style = MaterialTheme.typography.titleSmall)
            rows.filter { it.second > 0 }.forEach { (d, m, matched) ->
                Text("$d (${weekdayRu(d)}) — ${m / 60} ч ${m % 60} мин")
                matched.forEach { r ->
                    val params = ConditionParams.parse(r.rule.conditionType, r.rule.conditionParams)
                    val autoDesc = describeCondition(r.rule.conditionType, params)/// MAKE separate describe condition  ???
                    val title = r.rule.label.ifBlank { autoDesc }
                    Text("    → $title", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
fun ScheduleEditScreenPreview() {
    MaterialTheme {
        ScheduleEditScreen(scheduleId = 0L, onBack = {})
    }
}

private fun weekdayRu(dateStr: String): String {
    return try {
        val parts = dateStr.split("-")
        val cal = Calendar.getInstance().apply {
            set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        }
        when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "пн"
            Calendar.TUESDAY -> "вт"
            Calendar.WEDNESDAY -> "ср"
            Calendar.THURSDAY -> "чт"
            Calendar.FRIDAY -> "пт"
            Calendar.SATURDAY -> "сб"
            else -> "вс"
        }
    } catch (_: Exception) { "?" }
}