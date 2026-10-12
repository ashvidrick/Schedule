package com.example.workschedule.data

enum class ConditionType {
    WEEKDAY,             // конкретные дни недели (Mon, Tue...)
    DAY_OF_MONTH,        // конкретные числа месяца (1, 15, 31)
    EVERY_N_DAYS,        // каждый N-й день (от начала месяца)
    EVERY_N_WEEKDAY,     // каждый N-й день недели (напр. каждый 2-й понедельник)
    NTH_WEEKDAY_OF_MONTH,// N-й день недели в месяце (напр. 3-й понедельник)
    DATE_RANGE,          // с даты по дату
    PLUGIN               // зарезервировано под JAR-плагин
}