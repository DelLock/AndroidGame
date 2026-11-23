package com.example.androidgame.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ConnectivityMonitor(context: Context) {
    private val cm = context.getSystemService(ConnectivityManager::class.java)

    private val _online = MutableStateFlow(currentlyOnline())
    val online: StateFlow<Boolean> = _online

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _online.value = currentlyOnline()
        }

        override fun onLost(network: Network) {
            _online.value = currentlyOnline()
        }

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            _online.value = hasInternet(caps)
        }
    }

    init {
        try {
            cm?.registerDefaultNetworkCallback(callback)
        } catch (_: Exception) {
            _online.value = true
        }
    }

    fun unregister() {
        try {
            cm?.unregisterNetworkCallback(callback)
        } catch (_: Exception) {
        }
    }

    private fun currentlyOnline(): Boolean {
        val manager = cm ?: return true
        return try {
            val caps = manager.getNetworkCapabilities(manager.activeNetwork)
            (caps != null) && hasInternet(caps)
        } catch (_: Exception) {
            true
        }
    }

    private fun hasInternet(caps: NetworkCapabilities): Boolean =
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
