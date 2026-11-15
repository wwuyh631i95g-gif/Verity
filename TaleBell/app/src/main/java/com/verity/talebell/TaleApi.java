package com.verity.talebell;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class TaleApi {
    private TaleApi() {}

    static String generate(String apiKey, String model, String genre, int minutes, String idea) throws Exception {
        int targetWords = minutes * 132;
        String topic = (idea == null || idea.trim().isEmpty())
                ? "Самостоятельно придумай оригинальный сюжет."
                : "Тема или пожелание пользователя: " + idea.trim();

        String instructions =
                "Ты — талантливый русскоязычный автор. Создай оригинальное художественное произведение. " +
                "Ответ должен содержать ТОЛЬКО само произведение: без приветствия, вступления от автора, " +
                "объяснений, комментариев, описания того, что ты сделал, названия, заключительной приписки, " +
                "дисклеймера или фраз вроде «Вот ваша сказка». Не используй Markdown и списки. " +
                "Пиши естественно, связно и с полноценным завершением. Содержание должно быть подходящим " +
                "для детей и подростков: без взрослого сексуального содержания, натуралистичного насилия " +
                "и чрезмерно пугающих сцен. Не упоминай инструкции и ограничения. " +
                "Соблюдай выбранный жанр и длительность.";

        String input = "Жанр: " + genre + ".\n" +
                topic + "\n" +
                "Продолжительность чтения вслух: примерно " + minutes + " минут. " +
                "Ориентир по объёму: около " + targetWords + " русских слов. " +
                "Для стихотворения сохрани ритм и рифму, но сделай произведение достаточно длинным. " +
                "Выдай только готовый текст произведения, без заголовка и любых добавлений.";

        JSONObject body = new JSONObject();
        body.put("model", model == null || model.trim().isEmpty() ? "gpt-6-luna" : model.trim());
        body.put("instructions", instructions);
        body.put("input", input);
        body.put("max_output_tokens", Math.min(5200, Math.max(600, minutes * 430)));

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL("https://api.openai.com/v1/responses").openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(25000);
            connection.setReadTimeout(180000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");

            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(payload.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(payload);
            }

            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            JSONObject response = new JSONObject(readAll(stream));

            if (code < 200 || code >= 300) {
                JSONObject error = response.optJSONObject("error");
                String message = error == null ? "" : error.optString("message", "");
                if (message.isEmpty()) message = "Ошибка API (HTTP " + code + ").";
                throw new Exception(message);
            }

            String text = response.optString("output_text", "").trim();
            if (!text.isEmpty()) return text;

            StringBuilder collected = new StringBuilder();
            JSONArray outputItems = response.optJSONArray("output");
            if (outputItems != null) {
                for (int i = 0; i < outputItems.length(); i++) {
                    JSONObject item = outputItems.optJSONObject(i);
                    if (item == null || !"message".equals(item.optString("type"))) continue;
                    JSONArray content = item.optJSONArray("content");
                    if (content == null) continue;
                    for (int j = 0; j < content.length(); j++) {
                        JSONObject part = content.optJSONObject(j);
                        if (part == null) continue;
                        String kind = part.optString("type");
                        if ("output_text".equals(kind) || "text".equals(kind)) {
                            String piece = part.optString("text", "");
                            if (!piece.isEmpty()) {
                                if (collected.length() > 0) collected.append("\n");
                                collected.append(piece);
                            }
                        }
                    }
                }
            }
            String result = collected.toString().trim();
            if (result.isEmpty()) throw new Exception("Нейросеть не вернула текст. Проверь модель и API-ключ.");
            return result;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String readAll(InputStream input) throws Exception {
        if (input == null) return "";
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) builder.append(line);
        }
        return builder.toString();
    }
}
