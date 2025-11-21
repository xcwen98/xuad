package com.xcw.xuad.network.entity

import kotlinx.serialization.Serializable

@Serializable
data class AppFeedbackRequest(
    val packageName: String,
    val channelName: String,
    val version: String,
    val subject: String,
    val content: String,
    val contact: String? = null
)