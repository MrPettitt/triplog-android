package com.triplog.sync

/**
 * Whether a real backend has been configured. The project ships with a placeholder API URL,
 * and syncing against it can never succeed, so sync is skipped and the UI says so.
 */
data class SyncConfig(val apiBaseUrl: String) {
    val isConfigured: Boolean
        get() = apiBaseUrl.isNotBlank() && !apiBaseUrl.contains(PLACEHOLDER_HOST, ignoreCase = true)

    companion object {
        const val PLACEHOLDER_HOST = "your-api-url.com"
    }
}
