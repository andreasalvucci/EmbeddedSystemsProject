package com.example.cameraapp2.model

import androidx.room.Dao
import androidx.room.Query

@Dao
interface BusStopDao {
    @Query("SELECT * FROM bus_stop_table ORDER BY code ASC")
    fun readAllData(): List<BusStop>
}