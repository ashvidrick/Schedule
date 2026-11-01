package com.example.workschedule.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.workschedule.data.*
import com.example.workschedule.domain.ConditionParams

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RuleCard(
    rw: RuleWithIntervals,
    onChange: (RuleWithIntervals) -> Unit,
    onRemove: () -> Unit
) {
    val rule = rw.rule
    val params = remember(rule.conditionType, rule.conditionParams) {
        ConditionParams.parse(rule.conditionType, rule.conditionParams)
    }

    fun updateParams(newParams: ConditionParams) {
        onChange(rw.copy(rule = rule.copy(conditionParams = ConditionParams.toJson(newParams))))
    }

    fun updateIntervals(work: List<IntervalEntity>, breaks: List<IntervalEntity>) {
        onChange(rw.copy(intervals = work + breaks))
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Заголовок + удалить
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Условие", style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить")
                }
            }

            // Название правила
            OutlinedTextField(
                value = rule.label,
                onValueChange = { newLabel ->
                    onChange(rw.copy(rule = rule.copy(label = newLabel)))
                },
                label = { Text("Название (необязательно)") },
                placeholder = { Text(describeCondition(rule.conditionType, params)) },
                modifier = Modifier.fillMaxWidth()
            )

            // Чекбокс "Исключение"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Checkbox(
                    checked = rule.isException,
                    onCheckedChange = { checked ->
                        onChange(rw.copy(rule = rule.copy(isException = checked)))
                    }
                )
                Column {
                    Text("Исключение", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Перекрывает другие правила. Без интервалов = закрыто.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Селектор типа
            TypeSelector(
                current = rule.conditionType,
                onSelect = { newType ->
                    val newParams = ConditionParams.default(newType)
                    val autoLabel = describeCondition(newType, newParams)
                    val shouldUpdateLabel = rule.label.isBlank() ||
                            rule.label == describeCondition(rule.conditionType, params)
                    onChange(rw.copy(rule = rule.copy(
                        conditionType = newType,
                        conditionParams = ConditionParams.toJson(newParams),
                        label = if (shouldUpdateLabel) autoLabel else rule.label
                    )))
                }
            )

            // Редактор параметров
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

            HorizontalDivider()

            val workList = rw.intervals.filter { !it.isBreak }
            val breakList = rw.intervals.filter { it.isBreak }

            // --- Рабочие интервалы ---
            Text("Рабочие интервалы", style = MaterialTheme.typography.titleSmall)
            workList.forEachIndexed { idx, iv ->
                IntervalRow(
                    interval = iv,
                    onChange = { newIv ->
                        val newWork = workList.toMutableList().also { it[idx] = newIv }
                        updateIntervals(newWork, breakList)
                    },
                    onRemove = {
                        val newWork = workList.toMutableList().also { it.removeAt(idx) }
                        updateIntervals(newWork, breakList)
                    }
                )
            }
            TextButton(onClick = {
                val newIv = IntervalEntity(
                    ruleId = rule.id,
                    startMinutes = 9 * 60,
                    durationMinutes = 8 * 60,
                    isBreak = false
                )
                updateIntervals(workList + newIv, breakList)
            }) { Text("+ Рабочий интервал") }

            Spacer(Modifier.height(4.dp))

            // --- Перерывы ---
            Text("Перерывы", style = MaterialTheme.typography.titleSmall)
            breakList.forEachIndexed { idx, iv ->
                BreakRow(
                    interval = iv,
                    onChange = { newIv ->
                        val newBreaks = breakList.toMutableList().also { it[idx] = newIv }
                        updateIntervals(workList, newBreaks)
                    },
                    onRemove = {
                        val newBreaks = breakList.toMutableList().also { it.removeAt(idx) }
                        updateIntervals(workList, newBreaks)
                    }
                )
            }
            TextButton(onClick = {
                val newIv = IntervalEntity(
                    ruleId = rule.id,
                    startMinutes = 14 * 60,
                    durationMinutes = 60,
                    isBreak = true
                )
                updateIntervals(workList, breakList + newIv)
            }) { Text("+ Перерыв") }
        }
    }
}

/* ---------- Рабочий интервал ---------- */

@Composable
private fun IntervalRow(
    interval: IntervalEntity,
    onChange: (IntervalEntity) -> Unit,
    onRemove: () -> Unit
) {
    val start = interval.startMinutes
    val duration = interval.durationMinutes
    val end = start + duration

    var durationText by rememberSaveable(interval.id) {
        mutableStateOf(duration.toString())
    }

    LaunchedEffect(duration) {
        if (durationText != duration.toString()) {
            durationText = duration.toString()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Строка 1: Название (опционально) - можно убрать, если не нужно
        OutlinedTextField(
            value = interval.label,
            onValueChange = { newLabel ->
                onChange(interval.copy(label = newLabel))
            },
            label = { Text("Название интервала") },
            modifier = Modifier.fillMaxWidth()
        )

        // Строка 2: Начало + Конец
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimeField(
                label = "Начало",
                minutesOfDay = start,
                onChange = { newStart ->
                    onChange(interval.copy(startMinutes = newStart))
                },
                modifier = Modifier.weight(1f)
            )
            TimeField(
                label = "Конец",
                minutesOfDay = end,
                onChange = { newEnd ->
                    val newDuration = newEnd - start
                    if (newDuration > 0) {
                        onChange(interval.copy(durationMinutes = newDuration))
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }

        // Строка 3: Интервал (текст) + удаление
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${formatMinutes(start)} – ${formatMinutes(end)} (${formatDurationCard(duration)})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить")
            }
        }
    }
}

/* ---------- Перерыв ---------- */

@Composable
private fun BreakRow(
    interval: IntervalEntity,
    onChange: (IntervalEntity) -> Unit,
    onRemove: () -> Unit
) {
    val start = interval.startMinutes
    val duration = interval.durationMinutes
    val end = start + duration

    var durationText by rememberSaveable(interval.id) {
        mutableStateOf(duration.toString())
    }

    LaunchedEffect(duration) {
        if (durationText != duration.toString()) {
            durationText = duration.toString()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Строка 0: Название
        OutlinedTextField(
            value = interval.label,
            onValueChange = { newLabel ->
                onChange(interval.copy(label = newLabel))
            },
            label = { Text("Название перерыва (Обед, Тех. перерыв)") },
            modifier = Modifier.fillMaxWidth()
        )

        // Строка 1: Начало
        TimeField(
            label = "Начало перерыва",
            minutesOfDay = start,
            onChange = { newStart ->
                onChange(interval.copy(startMinutes = newStart))
            },
            modifier = Modifier.fillMaxWidth(0.65f)
        )

        // Строка 2: Конец + Минуты
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            TimeField(
                label = "Конец перерыва",
                minutesOfDay = end,
                onChange = { newEnd ->
                    val newDuration = newEnd - start
                    if (newDuration > 0) {
                        onChange(interval.copy(durationMinutes = newDuration))
                    }
                },
                modifier = Modifier.weight(0.65f)
            )
            OutlinedTextField(
                value = durationText,
                onValueChange = { txt ->
                    durationText = txt
                    txt.toIntOrNull()?.let { d ->
                        if (d > 0) {
                            onChange(interval.copy(durationMinutes = d))
                        }
                    }
                },
                label = { Text("Минут") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(0.35f)
            )
        }

        // Строка 3: Интервал + удаление
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Интервал: ${formatMinutes(start)} – ${formatMinutes(end)} (${formatDurationCard(duration)})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить перерыв")
            }
        }
    }
}

/* ---------- Описания и селекторы ---------- */

internal fun describeCondition(type: ConditionType, params: ConditionParams): String {
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
        is ConditionParams.DateRange -> "С ${params.from} по ${params.to}"
        ConditionParams.Plugin -> "Плагин"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeSelector(current: ConditionType, onSelect: (ConditionType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeekdayEditor(selected: Set<Int>, onChange: (Set<Int>) -> Unit) {
    val days = listOf(1 to "Пн", 2 to "Вт", 3 to "Ср", 4 to "Чт", 5 to "Пт", 6 to "Сб", 7 to "Вс")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        days.forEach { (iso, label) ->
            val isSelected = iso in selected
            FilterChip(
                selected = isSelected,
                onClick = { onChange(if (isSelected) selected - iso else selected + iso) },
                label = { Text(label) },
                leadingIcon = if (isSelected) {
                    {
                        Icon(Icons.Default.Check, contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize))
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
                onClick = { onChange(if (isSelected) selected - d else selected + d) },
                label = { Text(d.toString()) },
                leadingIcon = if (isSelected) {
                    {
                        Icon(Icons.Default.Check, contentDescription = null,
                            modifier = Modifier.size(14.dp))
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
        WeekdayDropdown(weekday, { onChange(it, n) }, Modifier.weight(1f))
        NumberField("N-й", n, { onChange(weekday, it) }, Modifier.weight(1f))
    }
}

@Composable
private fun NthWeekdayEditor(weekday: Int, nth: Int, onChange: (Int, Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NthDropdown(nth, { onChange(weekday, it) }, Modifier.weight(1f))
        WeekdayDropdown(weekday, { onChange(it, nth) }, Modifier.weight(1f))
    }
}

@Composable
private fun DateRangeEditor(from: String, to: String, onChange: (String, String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = from, onValueChange = { onChange(it, to) },
            label = { Text("С (YYYY-MM-DD)") }, modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = to, onValueChange = { onChange(from, it) },
            label = { Text("По (YYYY-MM-DD)") }, modifier = Modifier.weight(1f)
        )
    }
}

/* ---------- Мелкие компоненты ---------- */

@Composable
private fun NumberField(
    label: String, value: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; it.toIntOrNull()?.let(onChange) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeekdayDropdown(
    current: Int, onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val labels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = it }, modifier = modifier) {
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
    current: Int, onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = it }, modifier = modifier) {
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

/**
 * 60 → "01:00", 5 → "00:05", 90 → "01:30".
 * В отличие от formatMinutes, всегда 2 знака для часов.
 */
internal fun formatDurationCard(min: Int): String {
    val h = min / 60
    val m = min % 60
    return "%02d:%02d".format(h, m)
}