package com.zy.player.data.repository

import android.util.Log
import com.google.gson.Gson
import com.zy.player.data.remote.VodApiResponse
import com.zy.player.data.remote.VodApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VodRepository @Inject constructor(
    private val apiService: VodApiService,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "VodRepository"
    }

    suspend fun getVodList(
        baseUrl: String,
        page: Int? = null,
        typeId: Int? = null,
        keyword: String? = null
    ): Result<VodApiResponse> = withContext(Dispatchers.IO) {
        try {
            val url = buildString {
                append(baseUrl)
                append("?ac=videolist")
                page?.let { append("&pg=$it") }
                typeId?.let { append("&t=$it") }
                keyword?.let { append("&wd=$it") }
            }
            Log.d(TAG, "getVodList - URL: $url")
            Log.d(TAG, "getVodList - Params: page=$page, typeId=$typeId, keyword=$keyword")

            val response = apiService.getVodList(
                url = baseUrl,
                page = page,
                typeId = typeId,
                keyword = keyword
            )
            Log.d(TAG, "getVodList - Response: code=${response.code}, total=${response.total}, listSize=${response.list?.size}")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "getVodList - Error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getVodDetail(
        baseUrl: String,
        vodId: String
    ): Result<VodApiResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl?ac=detail&ids=$vodId"
            Log.d(TAG, "getVodDetail - URL: $url")
            Log.d(TAG, "getVodDetail - Params: vodId=$vodId")

            val response = apiService.getVodDetail(
                url = baseUrl,
                ids = vodId
            )
            Log.d(TAG, "getVodDetail - Response: code=${response.code}, vodName=${response.list?.firstOrNull()?.vod_name}")
            response.list?.firstOrNull()?.let { vod ->
                Log.d(TAG, "getVodDetail - vod_play_from: ${vod.vod_play_from}")
                Log.d(TAG, "getVodDetail - vod_play_url: ${vod.vod_play_url}")
                Log.d(TAG, "getVodDetail - vod_id: ${vod.vod_id}")
                Log.d(TAG, "getVodDetail - vod_remarks: ${vod.vod_remarks}")
            }
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "getVodDetail - Error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getCategories(baseUrl: String): Result<VodApiResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl?ac=videolist&pg=1"
            Log.d(TAG, "getCategories - URL: $url")

            val response = apiService.getVodList(
                url = baseUrl,
                page = 1
            )
            Log.d(TAG, "getCategories - Response: code=${response.code}, classSize=${response.`class`?.size}")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "getCategories - Error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun checkVideoSite(baseUrl: String): Result<VideoSiteCheckResponse> = withContext(Dispatchers.IO) {
        try {
            val checkUrl = buildString {
                append(baseUrl)
                append(if (baseUrl.contains("?")) "&" else "?")
                append("ac=videolist&pg=1")
            }
            val request = Request.Builder().url(checkUrl).build()
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

                val apiResponse = try {
                    gson.fromJson(content, VodApiResponse::class.java)
                } catch (e: Exception) {
                    return@withContext Result.failure(
                        SourceDataException(
                            message = "接口返回内容不是有效 JSON",
                            rawContent = content,
                            cause = e
                        )
                    )
                } ?: return@withContext Result.failure(
                    SourceDataException("接口返回内容不是有效 JSON", rawContent = content)
                )
                val hasList = !apiResponse.list.isNullOrEmpty()
                val hasClass = !apiResponse.`class`.isNullOrEmpty()
                val hasMeta = apiResponse.code != null || apiResponse.total != null || apiResponse.page != null
                if (!hasList && !hasClass && !hasMeta) {
                    return@withContext Result.failure(
                        SourceDataException(
                            message = "接口返回 JSON 不包含影视列表、分类或分页字段",
                            rawContent = content
                        )
                    )
                }

                Result.success(
                    VideoSiteCheckResponse(
                        httpCode = response.code,
                        contentType = response.header("Content-Type"),
                        rawContent = content,
                        response = apiResponse
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
