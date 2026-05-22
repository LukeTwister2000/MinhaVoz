package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_cards")
data class CustomCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val emoji: String
) {
    fun toCard() = Card(
        id = "custom_$id",
        text = text,
        emoji = emoji,
        category = "Meus",
        isCustom = true
    )
}
