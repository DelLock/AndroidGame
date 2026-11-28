package com.example.androidgame.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.util.Log

class ConnectivityMonitor(
    context: Context,
    private val onChange: (Boolean) -> Unit,
) {
    private val cm = context.getSystemService(ConnectivityManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var registered = false

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) {
            onChange(false)
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            onChange(hasInternet(networkCapabilities))
        }
    }

    fun register() {
        if (registered) return
        onChange(currentlyOnline())
        try {
            // Handler главного потока: состояние меняется из того же потока, что и действия пользователя.
            cm?.registerDefaultNetworkCallback(callback, mainHandler)
            registered = cm != null
        } catch (e: RuntimeException) {
            Log.e("ConnectivityMonitor", "Failed to register network callback", e)
            onChange(true)
        }
    }

    fun unregister() {
        if (!registered) return
        cm?.unregisterNetworkCallback(callback)
        registered = false
    }

    private fun currentlyOnline(): Boolean {
        val manager = cm ?: return true
        val network = manager.activeNetwork ?: return false
        val caps = manager.getNetworkCapabilities(network) ?: return false
        return hasInternet(caps)
    }

    private fun hasInternet(caps: NetworkCapabilities): Boolean =
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
