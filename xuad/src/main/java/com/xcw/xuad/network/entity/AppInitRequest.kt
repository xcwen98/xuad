package com.xcw.xuad.network.entity

import kotlinx.serialization.Serializable

@Serializable
data class AppInitRequest(
    val packageName: String,
    val channelName: String,
    val version: String
)