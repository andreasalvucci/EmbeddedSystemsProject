package com.example.cameraapp2.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.cameraapp2.R
import com.example.cameraapp2.model.BusStop
import com.example.cameraapp2.model.BusStopDao

@Database(entities = [BusStop::class], version = 1, exportSchema = false)
abstract class BusStopDatabase : RoomDatabase() {
    abstract fun busStopDao(): BusStopDao

    companion object {
        private const val DATABASE_NAME = "bus_stop_database"

        @Volatile
        private var INSTANCE: BusStopDatabase? = null

        fun getDatabase(context: Context): BusStopDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BusStopDatabase::class.java,
                    DATABASE_NAME
                ).createFromAsset(context.getString(R.string.bus_stop_database_asset_path)).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}