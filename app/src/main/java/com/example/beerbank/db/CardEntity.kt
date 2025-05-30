package com.example.beerbank.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.beerbank.model.Card

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val id: String,
    val number: String,
    val holderName: String,
    val expiryDate: String,
    val balance: Double,
    val cardType: String,
    val cvv: String
) {
    fun toCard(): Card {
        return Card(id, number, holderName, expiryDate, balance, cardType, cvv)
    }

    companion object {
        fun fromCard(card: Card): CardEntity {
            return CardEntity(
                id = card.id,
                number = card.number,
                holderName = card.holderName,
                expiryDate = card.expiryDate,
                balance = card.balance,
                cardType = card.cardType,
                cvv = card.cvv
            )
        }
    }
}