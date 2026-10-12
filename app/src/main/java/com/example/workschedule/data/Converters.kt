package com.example.workschedule.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun fromType(t: ConditionType): String = t.name
    @TypeConverter fun toType(s: String): ConditionType = ConditionType.valueOf(s)
}