package com.triplog.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.triplog.data.local.dao.FuelExpenseDao
import com.triplog.data.local.dao.TripDao
import com.triplog.data.local.entity.FuelExpenseEntity
import com.triplog.data.local.entity.TripEntity

@Database(
    entities = [TripEntity::class, FuelExpenseEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun fuelExpenseDao(): FuelExpenseDao

    companion object {
        const val DATABASE_NAME = "triplog.db"
    }
}
