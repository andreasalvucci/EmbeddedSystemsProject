package com.example.cameraapp2.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.cameraapp2.database.BusStopDatabase
import com.example.cameraapp2.model.BusStop
import com.example.cameraapp2.repository.BusStopRepository

class BusStopViewModel(application: Application) : AndroidViewModel(application) {
    var readAllData: List<BusStop>
    private var repository: BusStopRepository

    init {
        val busStopDao = BusStopDatabase.getDatabase(application).busStopDao()
        repository = BusStopRepository(busStopDao)
        readAllData = repository.readAllData
    }
}