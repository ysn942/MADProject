package com.example.myapplication.room

import android.content.Context
import androidx.room.*

@Database(entities = [CoordinatesEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun coordinatesDao(): ICoordinatesDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "coordinates_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
