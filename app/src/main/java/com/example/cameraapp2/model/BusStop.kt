package com.example.cameraapp2.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bus_stop_table")
data class BusStop(
    val name: String,
    @PrimaryKey
    val code: Int,
    val municipality: String?,
    val latitude: Double,
    val location: String?,
    val longitude: Double,
)