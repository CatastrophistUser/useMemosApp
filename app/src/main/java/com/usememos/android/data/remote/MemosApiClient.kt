package com.usememos.android.data.remote

import android.util.Log
import com.usememos.android.data.local.MemoEntity
import com.usememos.android.data.local.SyncState
import com.usememos.android.data.settings.SecureSettingsStore
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Instant
import java.util.concurrent.TimeUnit

class MemosApiClient(
    private val settingsStore: SecureSettingsStore,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun syncPendingMemo(memo: MemoEntity): Result<MemoEntity> = runCatching {
        val baseUrl = settingsStore.getBaseUrl()
        val token = settingsStore.getAccessToken()
        require(baseUrl.isNotBlank()) { "Server URL is required." }
        require(token.isNotBlank()) { "Access token is required." }

        val payload = JSONObject()
            .put("content", memo.content)
            .put("visibility", memo.visibility)

        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/api/v1/memos")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Sync failed with HTTP ${response.code}")
            }
            val body = response.body?.string().orEmpty()
            Log.d(TAG, "syncPendingMemo response=${body.take(500)}")
            parseSingleMemo(body, fallback = memo)
        }
    }.recoverCatching { throwable ->
        when (throwable) {
            is SocketTimeoutException -> throw IOException("Request timed out. Tailscale may still be negotiating.", throwable)
            is UnknownHostException -> throw IOException("Server not reachable. Verify Tailscale and server URL.", throwable)
            else -> throw throwable
        }
    }

    suspend fun fetchMemos(): Result<List<MemoEntity>> = runCatching {
        val baseUrl = settingsStore.getBaseUrl()
        val token = settingsStore.getAccessToken()
        if (baseUrl.isBlank() || token.isBlank()) {
            return@runCatching emptyList()
        }

        val allMemos = mutableListOf<MemoEntity>()
        var nextPageToken: String? = null

        do {
            val urlBuilder = "${baseUrl.trimEnd('/')}/api/v1/memos"
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("pageSize", "100")
            if (!nextPageToken.isNullOrBlank()) {
                urlBuilder.addQueryParameter("pageToken", nextPageToken)
            }

            val request = Request.Builder()
                .url(urlBuilder.build())
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Refresh failed with HTTP ${response.code}")
                }
                val body = response.body?.string().orEmpty()
                Log.d(TAG, "fetchMemos response=${body.take(1000)}")
                val page = parseMemoPage(body)
                allMemos += page.memos
                nextPageToken = page.nextPageToken
                Log.d(TAG, "fetchMemos pageCount=${page.memos.size} nextPageToken=${nextPageToken.orEmpty()}")
            }
        } while (!nextPageToken.isNullOrBlank())

        allMemos.also { parsed ->
            Log.d(TAG, "fetchMemos parsedCount=${parsed.size}")
        }
    }.recoverCatching { throwable ->
        when (throwable) {
            is SocketTimeoutException -> throw IOException("Refresh timed out. Tailscale may still be negotiating.", throwable)
            is UnknownHostException -> throw IOException("Server not reachable. Verify Tailscale and server URL.", throwable)
            else -> throw throwable
        }
    }

    private fun parseMemoPage(body: String): MemoPage {
        if (body.isBlank()) {
            return MemoPage(
                memos = emptyList(),
                nextPageToken = null,
            )
        }
        return if (body.trimStart().startsWith("[")) {
            val jsonArray = JSONArray(body)
            MemoPage(
                memos = List(jsonArray.length()) { index ->
                    parseMemoObject(jsonArray.getJSONObject(index))
                },
                nextPageToken = null,
            )
        } else {
            val root = JSONObject(body)
            val memosArray = root.optJSONArray("memos") ?: JSONArray()
            MemoPage(
                memos = List(memosArray.length()) { index ->
                    parseMemoObject(memosArray.getJSONObject(index))
                },
                nextPageToken = root.optString("nextPageToken").ifBlank { null },
            )
        }
    }

    private fun parseSingleMemo(body: String, fallback: MemoEntity): MemoEntity {
        if (body.isBlank()) return fallback.copy(syncState = SyncState.SYNCED, errorMessage = null)
        val root = JSONObject(body)
        return parseMemoObject(root, fallback)
    }

    private fun parseMemoObject(json: JSONObject, fallback: MemoEntity? = null): MemoEntity {
        val name = json.optString("name").ifBlank { fallback?.remoteName.orEmpty() }
        val uid = json.optString("uid")
        val content = json.optString("content").ifBlank { fallback?.content.orEmpty() }
        val visibility = json.optString("visibility").ifBlank { fallback?.visibility ?: "PRIVATE" }
        val createdAt = parseInstantMillis(json.optString("createTime"), fallback?.createdAt)
        val updatedAt = parseInstantMillis(json.optString("updateTime"), fallback?.updatedAt ?: createdAt)
        val displayTime = parseInstantMillis(json.optString("displayTime"), fallback?.displayTime ?: updatedAt)

        return MemoEntity(
            id = name.ifBlank { uid.ifBlank { fallback?.id ?: System.currentTimeMillis().toString() } },
            remoteName = name.ifBlank { uid.ifBlank { null } },
            content = content,
            visibility = visibility,
            createdAt = createdAt,
            updatedAt = updatedAt,
            displayTime = displayTime,
            syncState = SyncState.SYNCED,
            errorMessage = null,
        )
    }

    private fun parseInstantMillis(value: String?, fallback: Long? = null): Long {
        return runCatching {
            if (value.isNullOrBlank()) {
                fallback ?: System.currentTimeMillis()
            } else {
                Instant.parse(value).toEpochMilli()
            }
        }.getOrElse { fallback ?: System.currentTimeMillis() }
    }

    companion object {
        private const val TAG = "MemosApiClient"
    }
}

private data class MemoPage(
    val memos: List<MemoEntity>,
    val nextPageToken: String?,
)
