package com.example.workschedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.workschedule.ui.ScheduleEditScreen
import com.example.workschedule.ui.ScheduleListScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    val nav = rememberNavController()
                    NavHost(nav, startDestination = "list") {
                        composable("list") {
                            ScheduleListScreen(
                                onOpen = { id -> nav.navigate("edit/$id") },
                                onNew  = { nav.navigate("edit/0") }
                            )
                        }
                        composable("edit/{id}") { back ->
                            val id = back.arguments?.getString("id")?.toLongOrNull() ?: 0L
                            ScheduleEditScreen(
                                scheduleId = id,
                                onBack = { nav.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}