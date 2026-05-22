package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CardRepository(private val customCardDao: CustomCardDao) {

    // Merges static default cards with dynamic custom cards
    val customCardsFlow: Flow<List<Card>> = customCardDao.getAllCustomCards().map { entities ->
        entities.map { it.toCard() }
    }

    suspend fun addCustomCard(text: String, emoji: String) {
        val entity = CustomCardEntity(text = text, emoji = emoji)
        customCardDao.insertCustomCard(entity)
    }

    suspend fun deleteCustomCard(idString: String) {
        val idInt = idString.replace("custom_", "").toIntOrNull()
        if (idInt != null) {
            val entity = CustomCardEntity(id = idInt, text = "", emoji = "")
            customCardDao.deleteCustomCard(entity)
        }
    }
}
