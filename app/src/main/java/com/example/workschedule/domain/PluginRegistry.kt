package com.example.workschedule.domain

import java.util.Calendar

/**
 * Заглушка под будущие JAR-плагины.
 * Позже: DexClassLoader + интерфейс ConditionPlugin.
 */
object PluginRegistry {

    fun evaluate(paramsJson: String, date: Calendar): Boolean {
        // TODO: загрузка JAR и вызов isMatch(date, paramsJson)
        return false
    }
}