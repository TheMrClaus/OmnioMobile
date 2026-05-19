package com.nuvio.app.features.watched

expect object WatchedStorage {
    fun loadPayload(profileId: Int): String?
    fun savePayload(profileId: Int, payload: String)
    fun loadLastSuccessfulPushMs(profileId: Int): Long
    fun saveLastSuccessfulPushMs(profileId: Int, value: Long)
}

