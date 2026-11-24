package com.example.androidgame.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
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
            // Immediately set offline when default network is lost
            _online.value = false
        }

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            _online.value = hasInternet(caps)
        }
    }

    init {
        try {
            cm?.registerDefaultNetworkCallback(callback)
        } catch (e: Exception) {
            Log.e("ConnectivityMonitor", "Failed to register network callback", e)
            _online.value = true
        }
    }

    fun unregister() {
        try {
            cm?.unregisterNetworkCallback(callback)
        } catch (e: Exception) {
            // Ignored if callback was not registered or already unregistered
            Log.d("ConnectivityMonitor", "Unregister callback ignored", e)
        }
    }

    private fun currentlyOnline(): Boolean {
        val manager = cm ?: return true
        return try {
            val network = manager.activeNetwork ?: return false
            val caps = manager.getNetworkCapabilities(network) ?: return false
            hasInternet(caps)
        } catch (e: Exception) {
            Log.e("ConnectivityMonitor", "Error checking network capabilities", e)
            true
        }
    }

    private fun hasInternet(caps: NetworkCapabilities): Boolean =
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
