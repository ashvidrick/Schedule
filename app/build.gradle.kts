import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    // id("org.jetbrains.kotlin.android") — НЕ НУЖЕН, AGP 9 включает Kotlin сам [citation:7]
    alias(libs.plugins.compose.compiler) // добавляем эту строку
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.workschedule"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.workschedule"
        minSdk = 35
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

   /* kotlinOptions {
        jvmTarget = "17"
    }
    */


    buildFeatures {
        compose = true
    }

    // В AGP 9 + Kotlin 2.2+ блок composeOptions УДАЛЁН.
    // Compose-компилятор теперь плагин: org.jetbrains.kotlin.plugin.compose
    // Добавьте его в корневой файл, если он не тянется автоматически.
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.compose.foundation.layout)
    val roomVersion = "2.7.0"
    implementation(libs.androidx.room.runtime)
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.navigation:navigation-compose:2.10.2")

    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("org.json:json:20240303")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
}