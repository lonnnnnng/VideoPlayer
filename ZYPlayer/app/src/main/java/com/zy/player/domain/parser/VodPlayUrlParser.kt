package com.zy.player.domain.parser

import android.util.Log
import com.zy.player.domain.model.EpisodeGroup
import com.zy.player.domain.model.EpisodeItem

object VodPlayUrlParser {
    private const val TAG = "VodPlayUrlParser"

    fun parse(playFrom: String?, playUrl: String?): List<EpisodeItem> {
        return parseGroups(playFrom, playUrl).firstOrNull()?.episodes.orEmpty()
    }

    fun parseGroups(playFrom: String?, playUrl: String?): List<EpisodeGroup> {
        if (playUrl.isNullOrBlank()) return emptyList()

        Log.d(TAG, "parse - playFrom: $playFrom")
        Log.d(TAG, "parse - playUrl: $playUrl")

        val groupUrls = playUrl.split("\$\$\$")
        val groupNames = parseGroupNames(playFrom, groupUrls.size)
        val groupedEpisodes = mutableListOf<EpisodeGroup>()

        groupUrls.forEachIndexed { groupIndex, groupRaw ->
            val groupName = groupNames.getOrNull(groupIndex) ?: "group_$groupIndex"
            val episodes = groupRaw.split('#').mapIndexedNotNull { idx, entry ->
                val raw = entry.trim()
                if (raw.isBlank()) return@mapIndexedNotNull null
                val splitIndex = raw.indexOf('$')
                val label: String
                val url: String
                if (splitIndex <= 0 || splitIndex >= raw.lastIndex) {
                    label = "第${idx + 1}集"
                    url = raw
                } else {
                    label = raw.substring(0, splitIndex).ifBlank { "第${idx + 1}集" }
                    url = raw.substring(splitIndex + 1).trim()
                }
                if (url.isBlank()) return@mapIndexedNotNull null
                EpisodeItem(
                    groupName = groupName,
                    label = label,
                    url = normalizeUrl(url)
                )
            }
            if (episodes.isNotEmpty()) {
                Log.d(TAG, "parse - Group $groupIndex ($groupName): ${episodes.size} episodes")
                episodes.take(2).forEach { ep ->
                    Log.d(TAG, "parse - Episode: ${ep.label} -> ${ep.url}")
                }
                groupedEpisodes += EpisodeGroup(groupName, episodes)
            }
        }

        if (groupedEpisodes.isEmpty()) {
            Log.w(TAG, "parse - No episodes found")
            return emptyList()
        }

        val preferredIndex = groupedEpisodes.indexOfFirst { group ->
            val name = group.name
            name.contains("m3u8", ignoreCase = true) || name.contains("hls", ignoreCase = true)
        }.takeIf { it >= 0 } ?: groupedEpisodes.indexOfFirst { group ->
            group.episodes.any { it.url.contains(".m3u8", ignoreCase = true) }
        }.takeIf { it >= 0 } ?: groupedEpisodes.indexOfFirst { group ->
            group.episodes.any { isLikelyPlayable(it.url) }
        }.takeIf { it >= 0 } ?: 0

        val preferred = groupedEpisodes[preferredIndex]
        Log.d(TAG, "parse - Selected group: ${preferred.name}, ${preferred.episodes.size} episodes")

        return buildList {
            add(preferred)
            groupedEpisodes.forEachIndexed { index, group ->
                if (index != preferredIndex) add(group)
            }
        }
    }

    private fun parseGroupNames(playFrom: String?, expectedSize: Int): List<String> {
        val raw = playFrom?.trim().orEmpty()
        if (raw.isBlank()) return emptyList()
        val list = when {
            raw.contains("\$\$\$") -> raw.split("\$\$\$")
            raw.contains("|||") -> raw.split("|||")
            raw.contains(",") -> raw.split(",")
            else -> listOf(raw)
        }.map { it.trim() }.filter { it.isNotBlank() }
        if (list.size >= expectedSize) return list
        return list + List(expectedSize - list.size) { "group_${list.size + it}" }
    }

    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("//") -> "https:$trimmed"
            else -> trimmed
        }
    }

    private fun isLikelyPlayable(url: String?): Boolean {
        val raw = url.orEmpty().lowercase()
        return raw.startsWith("http://") || raw.startsWith("https://")
    }
}
