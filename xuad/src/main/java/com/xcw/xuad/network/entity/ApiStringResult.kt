package com.xcw.xuad.network.entity

import kotlinx.serialization.Serializable

@Serializable
data class ApiStringResult(
    val code: Int,
    val message: String,
    val success: Boolean,
    val data: String? = null
)