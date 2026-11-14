package com.verity.islandbuddy

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AiClient {
    fun ask(
        endpoint: String,
        model: String,
        apiKey: String,
        history: List<Pair<String, String>>,
        callback: (String?, String?) -> Unit
    ) {
        Thread {
            var connection: HttpURLConnection? = null
            try {
                require(endpoint.startsWith("https://", ignoreCase = true)) {
                    "Для безопасности адрес API должен начинаться с https://"
                }
                val messages = JSONArray()
                messages.put(
                    JSONObject()
                        .put("role", "system")
                        .put("content", "Ты дружелюбный голосовой помощник в приложении Island Buddy. Отвечай по-русски, понятно и коротко.")
                )
                history.takeLast(16).forEach { (role, text) ->
                    messages.put(JSONObject().put("role", role).put("content", text))
                }
                val body = JSONObject()
                    .put("model", model.ifBlank { "gpt-4o-mini" })
                    .put("messages", messages)
                    .put("temperature", 0.7)
                connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 25000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Authorization", "Bearer $apiKey")
                }
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val json = JSONObject(raw.ifBlank { "{}" })
                if (code !in 200..299) {
                    val message = json.optJSONObject("error")?.optString("message")
                    throw IllegalStateException(message?.takeIf { it.isNotBlank() } ?: "Сервер вернул HTTP $code")
                }
                val answer = json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .optString("content")
                if (answer.isBlank()) throw IllegalStateException("Нейросеть вернула пустой ответ")
                callback(answer.trim(), null)
            } catch (e: Exception) {
                callback(null, e.message ?: "Не удалось связаться с AI-сервисом")
            } finally {
                connection?.disconnect()
            }
        }.start()
    }
}
