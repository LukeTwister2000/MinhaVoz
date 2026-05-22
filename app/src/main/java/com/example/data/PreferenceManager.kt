package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("minha_voz_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_GRID_SIZE = "grid_size"
        private const val KEY_LAST_SENTENCE = "last_sentence"
    }

    var isDarkTheme: Boolean
        get() = prefs.getBoolean(KEY_DARK_THEME, false)
        set(value) {
            prefs.edit().putBoolean(KEY_DARK_THEME, value).apply()
        }

    var gridSize: String
        get() = prefs.getString(KEY_GRID_SIZE, "Normal") ?: "Normal"
        set(value) {
            prefs.edit().putString(KEY_GRID_SIZE, value).apply()
        }

    // Encodes sentence list to raw string representation (id|||text|||emoji|||category) separated by ;;;
    fun saveLastSentence(sentence: List<Card>) {
        val raw = sentence.joinToString(";;;") { card ->
            "${card.id}|||${card.text}|||${card.emoji}|||${card.category}"
        }
        prefs.edit().putString(KEY_LAST_SENTENCE, raw).apply()
    }

    fun loadLastSentence(): List<Card> {
        val raw = prefs.getString(KEY_LAST_SENTENCE, "") ?: ""
        if (raw.isEmpty()) return emptyList()
        val cards = mutableListOf<Card>()
        val tokens = raw.split(";;;")
        for (token in tokens) {
            if (token.isEmpty()) continue
            val parts = token.split("|||")
            if (parts.size >= 3) {
                val id = parts[0]
                val text = parts[1]
                val emoji = parts[2]
                val category = if (parts.size >= 4) parts[3] else "Meus"
                cards.add(Card(id, text, emoji, category, id.startsWith("custom_")))
            }
        }
        return cards
    }
}
