package com.zy.player.data.repository

import com.zy.player.data.local.dao.LiveSourceDao
import com.zy.player.data.local.DefaultSources
import com.zy.player.data.local.entity.LiveSourceEntity
import com.zy.player.domain.model.LiveChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveRepository @Inject constructor(
    private val liveSourceDao: LiveSourceDao,
    private val okHttpClient: OkHttpClient
) {
    fun observeAllSources(): Flow<List<LiveSourceEntity>> = liveSourceDao.observeAll()

    suspend fun getEnabledSources(): List<LiveSourceEntity> = liveSourceDao.getEnabled()

    suspend fun insertSource(source: LiveSourceEntity): Long = liveSourceDao.insert(source)

    suspend fun updateSource(source: LiveSourceEntity) = liveSourceDao.update(source)

    suspend fun deleteSource(source: LiveSourceEntity) = liveSourceDao.delete(source)

    suspend fun clearAllSources() = liveSourceDao.clearAll()

    suspend fun resetToDefaults() {
        liveSourceDao.clearAll()
        DefaultSources.liveSources.forEach { source ->
            liveSourceDao.insert(source)
        }
    }

    suspend fun moveSourceUp(source: LiveSourceEntity, allSources: List<LiveSourceEntity>) {
        val currentIndex = allSources.indexOfFirst { it.id == source.id }
        if (currentIndex > 0) {
            val prevSource = allSources[currentIndex - 1]
            liveSourceDao.update(source.copy(sortOrder = prevSource.sortOrder))
            liveSourceDao.update(prevSource.copy(sortOrder = source.sortOrder))
        }
    }

    suspend fun moveSourceDown(source: LiveSourceEntity, allSources: List<LiveSourceEntity>) {
        val currentIndex = allSources.indexOfFirst { it.id == source.id }
        if (currentIndex < allSources.size - 1) {
            val nextSource = allSources[currentIndex + 1]
            liveSourceDao.update(source.copy(sortOrder = nextSource.sortOrder))
            liveSourceDao.update(nextSource.copy(sortOrder = source.sortOrder))
        }
    }

    suspend fun fetchAndParseChannels(url: String): Result<List<LiveChannel>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }

            val content = response.body?.string() ?: ""
            val channels = parseM3U(content, url)
            Result.success(channels)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkLiveSource(url: String): Result<LiveSourceCheckResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                val content = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        SourceHttpException(
                            statusCode = response.code,
                            message = "HTTP ${response.code}",
                            rawContent = content
                        )
                    )
                }

                if (content.isBlank()) {
                    return@withContext Result.failure(
                        SourceDataException("接口返回内容为空", rawContent = content)
                    )
                }

                val channels = parseM3U(content, url)
                if (channels.isEmpty()) {
                    return@withContext Result.failure(
                        SourceDataException("接口返回内容无法解析出频道", rawContent = content)
                    )
                }

                Result.success(
                    LiveSourceCheckResponse(
                        httpCode = response.code,
                        contentType = response.header("Content-Type"),
                        rawContent = content,
                        channels = channels
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseM3U(content: String, sourceUrl: String): List<LiveChannel> {
        val channels = mutableListOf<LiveChannel>()
        val lines = content.lines()
        var currentGroup = "默认"
        var currentName: String? = null

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("#EXTINF:") -> {
                    // Parse #EXTINF:-1 tvg-name="CCTV1" tvg-logo="..." group-title="央视",CCTV1综合
                    val nameMatch = """tvg-name="([^"]+)"""".toRegex().find(trimmed)
                    val groupMatch = """group-title="([^"]+)"""".toRegex().find(trimmed)
                    val labelMatch = trimmed.substringAfterLast(",", "")

                    currentName = nameMatch?.groupValues?.get(1) ?: labelMatch.ifBlank { null }
                    currentGroup = groupMatch?.groupValues?.get(1) ?: "默认"
                }
                trimmed.startsWith("http") && currentName != null -> {
                    val format = when {
                        trimmed.contains(".m3u8") -> "m3u8"
                        trimmed.contains(".flv") -> "flv"
                        else -> "unknown"
                    }
                    channels.add(LiveChannel(currentName, trimmed, currentGroup, format))
                    currentName = null
                }
                trimmed.contains(",") && !trimmed.startsWith("#") -> {
                    // Simple format: 频道名,http://url
                    val parts = trimmed.split(",", limit = 2)
                    if (parts.size == 2 && parts[1].startsWith("http")) {
                        val format = when {
                            parts[1].contains(".m3u8") -> "m3u8"
                            parts[1].contains(".flv") -> "flv"
                            else -> "unknown"
                        }
                        channels.add(LiveChannel(parts[0], parts[1], currentGroup, format))
                    }
                }
            }
        }

        if (channels.isEmpty() && isDirectHlsPlaylist(content, sourceUrl)) {
            channels.add(LiveChannel("播放测试", sourceUrl, "测试频道", "m3u8"))
        }

        return channels
    }

    private fun isDirectHlsPlaylist(content: String, sourceUrl: String): Boolean {
        return sourceUrl.contains(".m3u8", ignoreCase = true) &&
            content.contains("#EXTM3U", ignoreCase = true) &&
            (
                content.contains("#EXT-X-STREAM-INF", ignoreCase = true) ||
                    content.contains("#EXT-X-TARGETDURATION", ignoreCase = true)
            )
    }
}
