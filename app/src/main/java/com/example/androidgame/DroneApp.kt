package com.example.androidgame

import android.app.Application

class DroneApp : Application() {
    val repository by lazy { DroneRepository(context = this) }
}
