package com.example.workschedule.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.workschedule.data.AppDatabase
import com.example.workschedule.data.ConditionType
import com.example.workschedule.data.RuleEntity
import com.example.workschedule.data.ScheduleEntity
import com.example.workschedule.data.ScheduleRepository
import com.example.workschedule.domain.ConditionEvaluator
import com.example.workschedule.domain.ScheduleCalculator
import kotlinx.coroutines.launch
import java.util.Calendar

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

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var rules by remember { mutableStateOf(listOf<RuleEntity>()) }
    var loaded by remember { mutableStateOf(scheduleId == 0L) }

    var tempIdCounter by remember { mutableStateOf(-1L) }

    // Если редактируем существующую — подгружаем. Для новой — просто ставим loaded=true.
    LaunchedEffect(scheduleId) {
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
            // Название
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Описание
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Заголовок секции "Правила" + кнопка добавить
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Правила", style = MaterialTheme.typography.titleMedium)
                    Row(modifier = Modifier.weight(1f)) {}
                    Button(onClick = {
                        // Новое правило с дефолтным типом и временем 9:00–18:00
                        rules = rules + RuleEntity(
                            id = tempIdCounter--,   // уникальный отрицательный id
                            scheduleId = scheduleId,
                            conditionType = ConditionType.WEEKDAY,
                            conditionParams = """{"days":[1,2,3,4,5]}""",
                            timeStartMinutes = 9 * 60,
                            timeEndMinutes = 18 * 60
                        )
                    }) { Text("Добавить") }
                }
            }

            // Список правил. ВАЖНО: key обязателен, иначе Compose путает состояния редакторов.
            itemsIndexed(rules, key = { _, r -> r.id }) { idx, rule ->
                RuleCard(
                    rule = rule,
                    onChange = { newRule ->
                        rules = rules.toMutableList().also { it[idx] = newRule }
                    },
                    onRemove = {
                        rules = rules.toMutableList().also { it.removeAt(idx) }
                    }
                )
            }

            // Предпросмотр на 7 дней
            if (rules.isNotEmpty()) {
                item { PreviewCard(rules = rules) }
            }
        }
    }
}

@Composable
private fun PreviewCard(rules: List<RuleEntity>) {
    val cal = Calendar.getInstance()
    val rows = (0..6).map {
        val c = cal.clone() as Calendar
        c.add(Calendar.DAY_OF_MONTH, it)
        val minutes = ScheduleCalculator.minutesForDay(rules, c)
        val dateStr = "%04d-%02d-%02d".format(
            c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)
        )
        Triple(dateStr, weekdayRu(c), minutes)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Предпросмотр (7 дней)", style = MaterialTheme.typography.titleSmall)
            rows.forEach { (d, wd, m) ->
                Text("$d ($wd) — ${m / 60} ч ${m % 60} мин")
            }
        }
    }
}

/**
 * Превью-заглушка. Обязательно без параметров.
 * Не вешай @Preview на рабочую ScheduleEditScreen — она с параметрами,
 * и студия ругается на "non-default parameters are not supported".
 */
@Preview(showBackground = true, widthDp = 380)
@Composable
fun ScheduleEditScreenPreview() {
    MaterialTheme {
        ScheduleEditScreen(scheduleId = 0L, onBack = {})
    }
}

private fun weekdayRu(cal: Calendar): String = when (cal.get(Calendar.DAY_OF_WEEK)) {
    Calendar.MONDAY -> "пн"
    Calendar.TUESDAY -> "вт"
    Calendar.WEDNESDAY -> "ср"
    Calendar.THURSDAY -> "чт"
    Calendar.FRIDAY -> "пт"
    Calendar.SATURDAY -> "сб"
    else -> "вс"
}