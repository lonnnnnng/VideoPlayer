package com.zy.player.data.repository

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

data class ImportedVideoSite(
    val name: String,
    val apiUrl: String
)

data class VideoSiteImportResult(
    val sites: List<ImportedVideoSite>,
    val format: String
)

@Singleton
class VideoSiteImportRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    suspend fun importFromUrl(url: String): Result<VideoSiteImportResult> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = url.trim().replace("&amp;", "&")
            val request = Request.Builder()
                .url(normalizedUrl)
                .header("User-Agent", "Mozilla/5.0 ZYPlayer")
                .build()
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
                parseSourcePayload(content)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseSourcePayload(content: String): Result<VideoSiteImportResult> {
        val raw = content.trim()
        if (raw.isBlank()) {
            return Result.failure(SourceDataException("导入地址返回内容为空", rawContent = content))
        }

        parseJson(raw, format = "JSON")?.let { return Result.success(it) }

        val decoded = decodeBase58(raw)
            ?: return Result.failure(SourceDataException("返回内容既不是 JSON，也不是有效 Base58 编码", rawContent = content))

        return parseJson(decoded, format = "Base58")
            ?.let { Result.success(it) }
            ?: Result.failure(SourceDataException("Base58 解码后不是有效 JSON 源配置", rawContent = decoded))
    }

    private fun parseJson(content: String, format: String): VideoSiteImportResult? {
        val root = runCatching {
            gson.fromJson(content, JsonObject::class.java)
        }.getOrNull() ?: return null
        val apiSite = root.getAsJsonObject("api_site") ?: return null
        val sites = apiSite.entrySet()
            .mapNotNull { entry ->
                val item = entry.value.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
                val apiUrl = item.get("api")?.asString?.trim().orEmpty()
                if (apiUrl.isBlank()) return@mapNotNull null
                val name = item.get("name")?.asString?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: entry.key
                ImportedVideoSite(
                    name = name,
                    apiUrl = apiUrl
                )
            }
            .distinctBy { it.apiUrl.trimEnd('/') }

        return if (sites.isEmpty()) {
            null
        } else {
            VideoSiteImportResult(sites = sites, format = format)
        }
    }

    private fun decodeBase58(value: String): String? {
        var bytes = byteArrayOf(0)
        value.forEach { char ->
            val digit = BASE58_ALPHABET.indexOf(char)
            if (digit < 0) return null
            bytes = multiplyAndAdd(bytes, 58, digit)
        }

        val leadingZeroCount = value.takeWhile { it == BASE58_ALPHABET.first() }.length
        val decoded = ByteArray(leadingZeroCount) + bytes.dropWhile { it == 0.toByte() }.toByteArray()
        return decoded.toString(Charsets.UTF_8)
    }

    private fun multiplyAndAdd(input: ByteArray, multiplier: Int, addend: Int): ByteArray {
        val result = input.copyOf()
        var carry = addend
        for (index in result.indices.reversed()) {
            val value = (result[index].toInt() and 0xFF) * multiplier + carry
            result[index] = value.toByte()
            carry = value ushr 8
        }

        var prefix = byteArrayOf()
        while (carry > 0) {
            prefix = byteArrayOf(carry.toByte()) + prefix
            carry = carry ushr 8
        }
        return prefix + result
    }

    private companion object {
        const val BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    }
}
