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
                require(apiKey.isNotBlank()) { "Не указан API-ключ" }

                val messages = JSONArray()
                messages.put(
                    JSONObject()
                        .put("role", "developer")
                        .put("content", "Ты дружелюбный помощник Island Buddy. Всегда отвечай по-русски, естественно и понятно. Старайся отвечать коротко, чтобы текст помещался в маленьком сообщении на экране.")
                )
                history.takeLast(18).forEach { (role, text) ->
                    messages.put(JSONObject().put("role", role).put("content", text))
                }
                val body = JSONObject()
                    .put("model", model.ifBlank { "gpt-5.4-mini" })
                    .put("messages", messages)

                connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 45000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer $apiKey")
                }
                connection.outputStream.use {
                    it.write(body.toString().toByteArray(Charsets.UTF_8))
                }

                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val json = JSONObject(raw.ifBlank { "{}" })
                if (code !in 200..299) {
                    val message = json.optJSONObject("error")?.optString("message")
                    val detail = message?.takeIf { it.isNotBlank() } ?: "Сервер вернул HTTP $code"
                    throw IllegalStateException(detail)
                }

                val answer = json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .optString("content")
                    .trim()
                if (answer.isBlank()) throw IllegalStateException("Нейросеть вернула пустой ответ")
                callback(answer, null)
            } catch (e: Exception) {
                callback(null, e.message ?: "Не удалось связаться с AI-сервисом")
            } finally {
                connection?.disconnect()
            }
        }.start()
    }
}
