package com.example.cameraapp2.repository

import com.example.cameraapp2.model.BusStop
import com.example.cameraapp2.model.BusStopDao

class BusStopRepository(busStopDao: BusStopDao) {
    val readAllData: List<BusStop> = busStopDao.readAllData()
}