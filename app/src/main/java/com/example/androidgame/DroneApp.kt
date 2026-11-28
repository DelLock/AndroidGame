package com.example.androidgame

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.example.androidgame.data.DroneRepository
import com.example.androidgame.data.api.OpenMeteoApi
import com.example.androidgame.util.ConnectivityMonitor
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

class DroneApp : Application() {

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .callTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                },
            )
            .build()
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    val openMeteoApi: OpenMeteoApi by lazy {
        retrofit.create(OpenMeteoApi::class.java)
    }

    val repository by lazy {
        DroneRepository(
            openMeteoApi = openMeteoApi,
            context = this,
        )
    }

    override fun onCreate() {
        super.onCreate()
        val monitor = ConnectivityMonitor(this, repository::onSystemNetworkChanged)
        // Подписка на уровне приложения: пока виден хотя бы один экран.
        registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            private var startedActivities = 0

            override fun onActivityStarted(activity: Activity) {
                if (startedActivities++ == 0) monitor.register()
            }

            override fun onActivityStopped(activity: Activity) {
                if (--startedActivities == 0) monitor.unregister()
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
