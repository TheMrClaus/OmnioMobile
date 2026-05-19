package com.nuvio.app.features.sourcecloud

internal expect object SourceCloudAuthStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
}
