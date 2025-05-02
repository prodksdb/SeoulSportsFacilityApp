package com.seouldata.sport.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.seouldata.sport.data.entity.FacilityEntity
import com.seouldata.sport.data.dao.FacilityDao

@Database(
    entities = [FacilityEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun facilityDao(): FacilityDao
}
