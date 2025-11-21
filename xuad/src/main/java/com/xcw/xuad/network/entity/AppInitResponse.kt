package com.xcw.xuad.network.entity

import kotlinx.serialization.Serializable

@Serializable
data class AppInitResponse(
    val adEnabled: Int,
    val adStrategy: String = "",
    val pangleAppId: String = "",
    val adSplashId: String = "",
    val adInterstitialId: String = "",
    val adBannerId: String = "",
    val adRewardedId: String = "",
    val downloadUrl: String = "",
    val umengKey: String = "",
    val apihzId: String = "",
    val apihzKey: String = ""
)