package com.example.attenbot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.attenbot.data.local.DataStoreManager
import com.example.attenbot.data.local.db.AppDatabase
import com.example.attenbot.data.repository.AttendanceRepository
import com.example.attenbot.ui.navigation.AppNavigation
import com.example.attenbot.ui.theme.AttenBotTheme
import com.example.attenbot.ui.viewmodel.AttendanceViewModel
import com.example.attenbot.ui.viewmodel.AttendanceViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: AttendanceViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val dataStoreManager = DataStoreManager(applicationContext)
        val repository = AttendanceRepository(database.attendanceDao())
        AttendanceViewModelFactory(repository, dataStoreManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AttenBotTheme {
                AppNavigation(viewModel = viewModel)
            }
        }
    }
}
