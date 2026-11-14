package com.verity.islandbuddy

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AssistantLogic {
    fun localReply(question: String): String {
        val q = question.lowercase(Locale.ROOT).trim()
        return when {
            q.contains("привет") || q.contains("здравств") -> "Привет! Я Island Buddy. Можешь спросить меня о чём-нибудь."
            q.contains("как тебя зовут") || q.contains("кто ты") -> "Я Island Buddy — маленький помощник, который живёт рядом с камерой."
            q.contains("который час") || q == "время" -> "Сейчас ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}."
            q.contains("какое сегодня число") || q.contains("какая сегодня дата") -> "Сегодня ${SimpleDateFormat("d MMMM yyyy", Locale("ru")).format(Date())}."
            q.contains("спасибо") -> "Пожалуйста! Рад помочь."
            q.contains("что ты умеешь") -> "Я могу отвечать голосом и текстом. Для полноценной работы нейросети подключи совместимый AI API в настройках приложения."
            else -> "Я сейчас работаю в локальном режиме и пока не знаю ответа на это. Подключи AI API в настройках Island Buddy, чтобы получать ответы нейросети."
        }
    }
}
