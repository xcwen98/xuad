package com.xcw.xuad.network.entity

import kotlinx.serialization.Serializable

@Serializable
data class AppRecordRequest(
    val oaid: String,
    val ecpm: Double,
    val adType: String,
    val interactionType: String,
    val deviceInfo: String
)