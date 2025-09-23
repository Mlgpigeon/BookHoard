package com.example.mybookhoard.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Configuration and utilities for session management
 */
object SessionConfig {
    private const val TAG = "SessionConfig"

    // Session validation intervals
    const val SESSION_VERIFICATION_INTERVAL = 24 * 60 * 60 * 1000L // 24 hours
    const val BACKGROUND_SYNC_INTERVAL = 6 * 60 * 60 * 1000L // 6 hours
    const val SESSION_RETRY_DELAY = 30 * 1000L // 30 seconds

    // Network timeouts
    const val CONNECT_TIMEOUT = 10_000 // 10 seconds
    const val READ_TIMEOUT = 15_000 // 15 seconds
    const val REQUEST_TIMEOUT = 20_000L // 20 seconds

    // Retry configuration
    val RETRY_DELAYS = listOf(1000L, 2000L, 5000L, 10000L) // Exponential backoff
    const val MAX_RETRY_ATTEMPTS = 3

    // App preferences
    private const val APP_PREFS_NAME = "bookhoard_app_prefs"
    private const val PREF_FIRST_LAUNCH = "first_launch"
    private const val PREF_LAST_BACKGROUND_SYNC = "last_background_sync"
    private const val PREF_AUTO_SYNC_ENABLED = "auto_sync_enabled"
    private const val PREF_OFFLINE_MODE = "offline_mode"

    fun getAppPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(APP_PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isFirstLaunch(context: Context): Boolean {
        val prefs = getAppPreferences(context)
        return prefs.getBoolean(PREF_FIRST_LAUNCH, true)
    }

    fun setFirstLaunchCompleted(context: Context) {
        getAppPreferences(context).edit()
            .putBoolean(PREF_FIRST_LAUNCH, false)
            .apply()
        Log.d(TAG, "First launch completed")
    }

    fun getLastBackgroundSync(context: Context): Long {
        return getAppPreferences(context).getLong(PREF_LAST_BACKGROUND_SYNC, 0)
    }

    fun setLastBackgroundSync(context: Context, timestamp: Long = System.currentTimeMillis()) {
        getAppPreferences(context).edit()
            .putLong(PREF_LAST_BACKGROUND_SYNC, timestamp)
            .apply()
    }

    fun isAutoSyncEnabled(context: Context): Boolean {
        return getAppPreferences(context).getBoolean(PREF_AUTO_SYNC_ENABLED, true)
    }

    fun setAutoSyncEnabled(context: Context, enabled: Boolean) {
        getAppPreferences(context).edit()
            .putBoolean(PREF_AUTO_SYNC_ENABLED, enabled)
            .apply()
        Log.d(TAG, "Auto sync ${if (enabled) "enabled" else "disabled"}")
    }

    fun isOfflineMode(context: Context): Boolean {
        return getAppPreferences(context).getBoolean(PREF_OFFLINE_MODE, false)
    }

    fun setOfflineMode(context: Context, offline: Boolean) {
        getAppPreferences(context).edit()
            .putBoolean(PREF_OFFLINE_MODE, offline)
            .apply()
        Log.d(TAG, "Offline mode ${if (offline) "enabled" else "disabled"}")
    }

    fun shouldPerformBackgroundSync(context: Context): Boolean {
        if (!isAutoSyncEnabled(context) || isOfflineMode(context)) {
            return false
        }

        val lastSync = getLastBackgroundSync(context)
        val now = System.currentTimeMillis()
        return (now - lastSync) > BACKGROUND_SYNC_INTERVAL
    }

    fun shouldVerifySession(lastVerification: Long): Boolean {
        val now = System.currentTimeMillis()
        return (now - lastVerification) > SESSION_VERIFICATION_INTERVAL
    }

    fun clearAllPreferences(context: Context) {
        getAppPreferences(context).edit().clear().apply()
        Log.d(TAG, "All app preferences cleared")
    }
}