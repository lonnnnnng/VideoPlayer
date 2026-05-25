package com.zy.player.ui.navigation

import android.net.Uri

object Destinations {
    const val HOME = "home"
    const val LIVE = "live"
    const val SETTINGS = "settings"
    const val SEARCH = "search"
    const val SEARCH_RESULT = "search_result/{keyword}"
    const val DETAIL = "detail/{siteId}/{vodId}"
    const val EPISODES = "episodes/{siteId}/{vodId}"
    const val PLAYER = "player/{siteId}/{vodId}/{episodeUrl}?title={title}&episodeLabel={episodeLabel}"
    const val PLAYER_PREVIEW = "player_preview"
    const val HISTORY = "history"
    const val SITE_MANAGEMENT = "site_management"
    const val LIVE_SOURCE_MANAGEMENT = "live_source_management"

    fun searchResult(keyword: String) = "search_result/${Uri.encode(keyword)}"
    fun detail(siteId: Long, vodId: String) = "detail/$siteId/$vodId"
    fun episodes(siteId: Long, vodId: String) = "episodes/$siteId/$vodId"
    fun player(
        siteId: Long,
        vodId: String,
        episodeUrl: String,
        title: String = "",
        episodeLabel: String = ""
    ): String {
        val encodedUrl = Uri.encode(episodeUrl)
        val encodedTitle = Uri.encode(title)
        val encodedEpisodeLabel = Uri.encode(episodeLabel)
        return "player/$siteId/$vodId/$encodedUrl?title=$encodedTitle&episodeLabel=$encodedEpisodeLabel"
    }
}
