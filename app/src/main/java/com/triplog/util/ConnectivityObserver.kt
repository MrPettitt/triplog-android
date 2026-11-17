package com.triplog.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.triplog.sync.SyncScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

enum class NetworkStatus { Available, Unavailable }

@Singleton
class ConnectivityObserver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncScheduler: SyncScheduler
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var syncTriggerRegistered = false

    /** Pure status stream for the UI. Collecting it has no side effects. */
    val networkStatus: Flow<NetworkStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(NetworkStatus.Available)
            }

            override fun onLost(network: Network) {
                trySend(NetworkStatus.Unavailable)
            }

            override fun onUnavailable() {
                trySend(NetworkStatus.Unavailable)
            }
        }

        connectivityManager.registerNetworkCallback(validatedInternetRequest(), callback)
        // Emit initial state
        val initial = if (isConnected()) NetworkStatus.Available else NetworkStatus.Unavailable
        trySend(initial)

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    /**
     * Requests a sync whenever the device gets a network. Call once from the Application so it
     * works whether or not any screen is showing. The scheduler de-duplicates the requests.
     */
    fun startSyncOnReconnect() {
        if (syncTriggerRegistered) return
        syncTriggerRegistered = true
        // Only networks the system has verified can reach the internet, so a captive portal or a
        // Wi-Fi network without a connection does not trigger a pointless sync.
        connectivityManager.registerNetworkCallback(
            validatedInternetRequest(),
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    syncScheduler.requestSync()
                }
            }
        )
    }

    fun isConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun validatedInternetRequest(): NetworkRequest = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        .build()
}
