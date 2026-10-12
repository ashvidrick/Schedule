plugins {
    id("com.android.application") version "9.2.1" apply false
    alias(libs.plugins.compose.compiler) apply false
    // Плагин kotlin-android НЕ нужен — AGP 9 использует встроенный Kotlin [citation:7]

    // KSP нужен для Room. Версия должна соответствовать Kotlin, который тянет AGP.
    // AGP 9.0+ по умолчанию требует KGP 2.2.10 или выше [citation:5][citation:9]
    id("com.google.devtools.ksp") version "2.3.6" apply false
}