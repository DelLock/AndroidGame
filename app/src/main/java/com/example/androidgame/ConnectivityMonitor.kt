package com.example.androidgame

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Следит за интернетом телефона. Нужен для индикатора "Онлайн / Нет сети". */
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
        } catch (_: SecurityException) {
            // нет разрешения ACCESS_NETWORK_STATE — индикатор просто останется "онлайн"
            _online.value = true
        }
    }

    private fun currentlyOnline(): Boolean {
        val manager = cm ?: return true
        return try {
            val caps = manager.getNetworkCapabilities(manager.activeNetwork)
            (caps != null) && hasInternet(caps)
        } catch (_: SecurityException) {
            true
        }
    }

    private fun hasInternet(caps: NetworkCapabilities): Boolean =
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
