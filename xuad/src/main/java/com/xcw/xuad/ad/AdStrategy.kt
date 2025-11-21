package com.xcw.xuad.ad

import kotlinx.serialization.Serializable

@Serializable
data class AdPageConfig(
    val pageName: String? = null,
    val pageRemark: String? = null,
    val clickAd: Boolean = false,
    val bannerAd: Boolean = false,
    val interstitialMode: String = "cooldown",
    val interstitialCount: Int = 0,
    val concurrentInterstitialCount: Int = 0
)

@Serializable
data class AdStrategy(
    val splashCount: Int = 0,
    val detailConfigs: List<AdPageConfig> = emptyList(),
    val fallbackConfig: AdPageConfig? = null,
    val hotStartSplash: Boolean = false,
    val globalBannerEnabled: Boolean = false,
    val globalSplashEnabled: Boolean = false,
    val interstitialCooldown: Int = 0,
    val globalInterstitialEnabled: Boolean = false
)