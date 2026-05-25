package com.zy.player.data.repository

import android.util.Log
import com.zy.player.data.remote.VodApiResponse
import com.zy.player.data.remote.VodApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VodRepository @Inject constructor(
    private val apiService: VodApiService
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
}
