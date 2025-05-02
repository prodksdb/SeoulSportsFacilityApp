package com.seouldata.sport.data.db

import android.content.Context
import androidx.room.Room

object AppDatabaseProvider {

    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "facility_database"
            ).build()
            INSTANCE = instance
            instance
        }
    }
}
