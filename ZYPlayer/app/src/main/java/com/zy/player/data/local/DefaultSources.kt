package com.zy.player.data.local

import com.zy.player.data.local.entity.LiveSourceEntity
import com.zy.player.data.local.entity.VideoSiteEntity

object DefaultSources {
    const val DEFAULT_LIVE_SOURCE_URL = "https://raw.githubusercontent.com/fanmingming/live/main/tv/m3u/index.m3u"
    const val PLAYBACK_TEST_LIVE_SOURCE_URL = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
    const val LEGACY_IPV6_LIVE_SOURCE_URL = "https://live.fanmingming.com/tv/m3u/ipv6.m3u"

    val videoSites = listOf(
        VideoSiteEntity(
            name = "无尽资源",
            apiUrl = "https://api.wujinapi.me/api.php/provide/vod/",
            enabled = true,
            sortOrder = 1,
            lastCheckStatus = "可播放"
        ),
        VideoSiteEntity(
            name = "量子资源",
            apiUrl = "https://cj.lziapi.com/api.php/provide/vod/",
            enabled = true,
            sortOrder = 2
        ),
        VideoSiteEntity(
            name = "非凡资源",
            apiUrl = "https://cj.ffzyapi.com/api.php/provide/vod/",
            enabled = true,
            sortOrder = 3
        )
    )

    val liveSources = listOf(
        LiveSourceEntity(
            name = "IPTV直播源",
            url = DEFAULT_LIVE_SOURCE_URL,
            enabled = true,
            sortOrder = 1
        ),
        LiveSourceEntity(
            name = "播放测试源",
            url = PLAYBACK_TEST_LIVE_SOURCE_URL,
            enabled = true,
            sortOrder = 2,
            lastCheckStatus = "可播放"
        )
    )
}
