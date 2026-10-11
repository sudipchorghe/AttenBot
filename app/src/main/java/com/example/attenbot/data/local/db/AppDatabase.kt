package com.example.attenbot.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.attenbot.data.local.dao.AttendanceDao
import com.example.attenbot.data.local.entity.AttendanceRecord

@Database(
    entities = [AttendanceRecord::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "attenbot_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
