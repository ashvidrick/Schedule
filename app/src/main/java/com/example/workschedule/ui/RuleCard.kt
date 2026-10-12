package com.example.workschedule.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.workschedule.data.ConditionType
import com.example.workschedule.data.RuleEntity
import com.example.workschedule.domain.ConditionParams

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RuleCard(
    rule: RuleEntity,
    onChange: (RuleEntity) -> Unit,
    onRemove: () -> Unit
) {
    // Парсим params один раз на изменение. parse() безопасен.
    val params = remember(rule.conditionType, rule.conditionParams) {
        ConditionParams.parse(rule.conditionType, rule.conditionParams)
    }

    /** Обновляет params + пересобирает JSON + отправляет наверх. */
    fun updateParams(newParams: ConditionParams) {
        onChange(rule.copy(conditionParams = ConditionParams.toJson(newParams)))
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Заголовок с кнопкой удаления
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Условие", style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить")
                }
            }

            // Выбор типа условия
            TypeSelector(
                current = rule.conditionType,
                onSelect = { newType ->
                    // Смена типа → ставим дефолтные params
                    onChange(
                        rule.copy(
                            conditionType = newType,
                            conditionParams = ConditionParams.toJson(
                                ConditionParams.default(newType)
                            )
                        )
                    )
                }
            )
            // человекочитаемый промежуток
            Text(
                text = describeCondition(rule.conditionType, params),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

            // Диспетчер редактора
            when (params) {
                is ConditionParams.Weekdays -> WeekdayEditor(
                    selected = params.days,
                    onChange = { updateParams(params.copy(days = it)) }
                )
                is ConditionParams.DaysOfMonth -> DayOfMonthEditor(
                    selected = params.days,
                    onChange = { updateParams(params.copy(days = it)) }
                )
                is ConditionParams.EveryNDays -> EveryNDaysEditor(
                    n = params.n, offset = params.offset,
                    onChange = { n, o -> updateParams(params.copy(n = n, offset = o)) }
                )
                is ConditionParams.EveryNWeekday -> EveryNWeekdayEditor(
                    weekday = params.weekday, n = params.n,
                    onChange = { w, n -> updateParams(params.copy(weekday = w, n = n)) }
                )
                is ConditionParams.NthWeekdayOfMonth -> NthWeekdayEditor(
                    weekday = params.weekday, nth = params.nth,
                    onChange = { w, n -> updateParams(params.copy(weekday = w, nth = n)) }
                )
                is ConditionParams.DateRange -> DateRangeEditor(
                    from = params.from, to = params.to,
                    onChange = { f, t -> updateParams(params.copy(from = f, to = t)) }
                )
                ConditionParams.Plugin -> {
                    Text("Плагин (редактор не реализован)",
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            // Время: два числовых поля в минутах от начала суток
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeField(
                    label = "Начало",
                    minutesOfDay = rule.timeStartMinutes,
                    onChange = { onChange(rule.copy(timeStartMinutes = it)) },
                    modifier = Modifier.weight(1f)
                )
                TimeField(
                    label = "Конец",
                    minutesOfDay = rule.timeEndMinutes,
                    onChange = { onChange(rule.copy(timeEndMinutes = it)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}


private fun describeCondition(type: ConditionType, params: ConditionParams): String {
    return when (params) {
        is ConditionParams.Weekdays -> {
            val labels = listOf("Пн","Вт","Ср","Чт","Пт","Сб","Вс")
            val names = params.days.sorted().joinToString(", ") { labels.getOrElse(it - 1) { "?" } }
            "Дни недели: $names"
        }
        is ConditionParams.DaysOfMonth ->
            "Дни месяца: ${params.days.sorted().joinToString(", ")}"
        is ConditionParams.EveryNDays ->
            "Каждый ${params.n}-й день (смещение ${params.offset})"
        is ConditionParams.EveryNWeekday -> {
            val labels = listOf("Пн","Вт","Ср","Чт","Пт","Сб","Вс")
            "Каждый ${params.n}-й ${labels.getOrElse(params.weekday - 1) { "?" }} месяца"
        }
        is ConditionParams.NthWeekdayOfMonth -> {
            val labels = listOf("Пн","Вт","Ср","Чт","Пт","Сб","Вс")
            "${params.nth}-й ${labels.getOrElse(params.weekday - 1) { "?" }} месяца"
        }
        is ConditionParams.DateRange ->
            "С ${params.from} по ${params.to}"
        ConditionParams.Plugin -> "Плагин"
    }
}
/* ---------- Селектор типа условия ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeSelector(current: ConditionType, onSelect: (ConditionType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = current.humanName(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Тип условия") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ConditionType.entries.forEach { t ->
                DropdownMenuItem(
                    text = { Text(t.humanName()) },
                    onClick = {
                        expanded = false
                        if (t != current) onSelect(t)
                    }
                )
            }
        }
    }
}

private fun ConditionType.humanName(): String = when (this) {
    ConditionType.WEEKDAY -> "Дни недели"
    ConditionType.DAY_OF_MONTH -> "Дни месяца"
    ConditionType.EVERY_N_DAYS -> "Каждый N-й день"
    ConditionType.EVERY_N_WEEKDAY -> "Каждый N-й день недели"
    ConditionType.NTH_WEEKDAY_OF_MONTH -> "N-й день недели месяца"
    ConditionType.DATE_RANGE -> "Диапазон дат"
    ConditionType.PLUGIN -> "Плагин"
}

/* ---------- Редакторы ---------- */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeekdayEditor(selected: Set<Int>, onChange: (Set<Int>) -> Unit) {
    val days = listOf(
        1 to "Пн", 2 to "Вт", 3 to "Ср", 4 to "Чт",
        5 to "Пт", 6 to "Сб", 7 to "Вс"
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        days.forEach { (iso, label) ->
            val isSelected = iso in selected
            FilterChip(
                selected = isSelected,
                onClick = {
                    onChange(if (isSelected) selected - iso else selected + iso)
                },
                label = { Text(label) },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayOfMonthEditor(selected: Set<Int>, onChange: (Set<Int>) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..31).forEach { d ->
            val isSelected = d in selected
            FilterChip(
                selected = isSelected,
                onClick = {
                    onChange(if (isSelected) selected - d else selected + d)
                },
                label = { Text(d.toString()) },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            // Чуть меньше стандартной иконки — чтобы не раздувать чип с числом
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}
@Composable
private fun EveryNDaysEditor(n: Int, offset: Int, onChange: (Int, Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField("Каждый N-й", n, { onChange(it, offset) }, Modifier.weight(1f))
        NumberField("Смещение", offset, { onChange(n, it) }, Modifier.weight(1f))
    }
}

@Composable
private fun EveryNWeekdayEditor(weekday: Int, n: Int, onChange: (Int, Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WeekdayDropdown(
            current = weekday,
            onSelect = { onChange(it, n) },
            modifier = Modifier.weight(1f)
        )
        NumberField("N-й", n, { onChange(weekday, it) }, Modifier.weight(1f))
    }
}

@Composable
private fun NthWeekdayEditor(weekday: Int, nth: Int, onChange: (Int, Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NthDropdown(
            current = nth,
            onSelect = { onChange(weekday, it) },
            modifier = Modifier.weight(1f)   // ← здесь weight доступен
        )
        WeekdayDropdown(
            current = weekday,
            onSelect = { onChange(it, nth) },
            modifier = Modifier.weight(1f)
        )
    }
}
@Composable
private fun DateRangeEditor(from: String, to: String, onChange: (String, String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = from,
            onValueChange = { onChange(it, to) },
            label = { Text("С (YYYY-MM-DD)") },
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = to,
            onValueChange = { onChange(from, it) },
            label = { Text("По (YYYY-MM-DD)") },
            modifier = Modifier.weight(1f)
        )
    }
}

/* ---------- Мелкие компоненты ---------- */
/*
@Composable
private fun TimeField(
    label: String, value: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            it.toIntOrNull()?.let(onChange)
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}
*/
@Composable
private fun NumberField(
    label: String, value: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            it.toIntOrNull()?.let(onChange)
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeekdayDropdown(
    current: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val labels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier   // ← используем переданный modifier
    ) {
        OutlinedTextField(
            value = labels.getOrElse(current - 1) { "Пн" },
            onValueChange = {}, readOnly = true,
            label = { Text("День недели") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            labels.forEachIndexed { i, l ->
                DropdownMenuItem(
                    text = { Text(l) },
                    onClick = { expanded = false; onSelect(i + 1) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NthDropdown(
    current: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = "${current}-й",
            onValueChange = {}, readOnly = true,
            label = { Text("Какой по счёту") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            (1..5).forEach { n ->
                DropdownMenuItem(
                    text = { Text("$n-й") },
                    onClick = { expanded = false; onSelect(n) }
                )
            }
        }
    }
}