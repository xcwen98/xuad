package com.xcw.xuad.network.entity

import kotlinx.serialization.Serializable

@Serializable
data class AppUserInitRequest(
    val packageName: String,
    val channelName: String,
    val version: String,
    val oaid: String,
    val deviceInfo: String
)