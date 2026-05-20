package com.example.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
    private const val MODEL_IMAGE = "gemini-2.5-flash-image"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Verifies if the API key is set and not a placeholder.
     */
    fun isApiKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return !key.isNullOrBlank() && key != "MY_GEMINI_API_KEY" && key != "GEMINI_API_KEY"
    }

    /**
     * Generates a keyframe image from a text prompt.
     * Appends consistency guidelines and technical specifications automatically.
     * Returns a local file path to the saved image, or null if it fails or if the API key is not set.
     */
    suspend fun generateVideoKeyframe(
        context: Context,
        prompt: String,
        characterName: String?,
        characterDesc: String?,
        resolution: String = "1080p",
        duration: Int = 10,
        isReferenceEdit: Boolean = false,
        referencePrompt: String? = null
    ): String? {
        if (!isApiKeyConfigured()) {
            Log.w(TAG, "Gemini API key is not configured. Falling back to procedural/mock generation.")
            return null
        }

        val finalPrompt = buildString {
            append("Cinematic video keyframe, 16:9 aspect ratio, 1080p resolution.")
            if (!characterName.isNullOrBlank()) {
                append(" Character named '$characterName'")
                if (!characterDesc.isNullOrBlank()) {
                    append(" with description: $characterDesc. Maintain absolute character consistency.")
                }
                append(".")
            }
            if (isReferenceEdit && !referencePrompt.isNullOrBlank()) {
                append(" Edits based on reference video: $referencePrompt.")
            }
            append(" Duration token: ${duration}s. Style: Ultra-detailed digital art. Main Prompt: $prompt")
        }

        try {
            val url = "$BASE_URL$MODEL_IMAGE:generateContent?key=${BuildConfig.GEMINI_API_KEY}"

            val requestBodyJson = JSONObject().apply {
                // contents
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", finalPrompt)
                            })
                        })
                    })
                })

                // generationConfig
                put("generationConfig", JSONObject().apply {
                    put("imageConfig", JSONObject().apply {
                        put("aspectRatio", "16:9")
                        put("imageSize", "1K")
                    })
                    put("responseModalities", JSONArray().apply {
                        put("TEXT")
                        put("IMAGE")
                    })
                })
            }

            val requestBody = requestBodyJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "API Call failed with code: ${response.code}, message: ${response.message}")
                return null
            }

            val responseBodyString = response.body?.string() ?: return null
            val jsonResponse = JSONObject(responseBodyString)
            val candidates = jsonResponse.optJSONArray("candidates") ?: return null
            val firstCandidate = candidates.optJSONObject(0) ?: return null
            val content = firstCandidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null

            for (i in 0 until parts.length()) {
                val part = parts.optJSONObject(i) ?: continue
                val inlineData = part.optJSONObject("inlineData") ?: continue
                val mimeType = inlineData.optString("mimeType", "")
                val base64Data = inlineData.optString("data", "")

                if (mimeType.startsWith("image/") && base64Data.isNotEmpty()) {
                    val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                    return saveImageBytesToLocalStorage(context, bytes)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in generateVideoKeyframe: ${e.message}", e)
        }

        return null
    }

    private fun saveImageBytesToLocalStorage(context: Context, bytes: ByteArray): String? {
        return try {
            val directory = File(context.filesDir, "generated_keyframes")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val fileName = "video_frame_${System.currentTimeMillis()}.png"
            val file = File(directory, fileName)
            FileOutputStream(file).use { out ->
                out.write(bytes)
            }
            file.absolutePath
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save generated image bytes", e)
            null
        }
    }
}
