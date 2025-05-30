package com.example.beerbank.repository

import com.example.beerbank.db.CardDao
import com.example.beerbank.db.CardEntity
import com.example.beerbank.model.Card

class CardRepository(private val cardDao: CardDao) {
    suspend fun getAllCards(): List<Card> {
        return cardDao.getAllCards().map { it.toCard() }
    }

    suspend fun getCardById(id: String): Card? {
        return cardDao.getCardById(id)?.toCard()
    }

    suspend fun insertCard(card: Card) {
        cardDao.insertCard(CardEntity.fromCard(card))
    }

    suspend fun updateCard(card: Card) {
        cardDao.updateCard(CardEntity.fromCard(card))
    }
    suspend fun getCardCount(): Int {
        return cardDao.getCardCount()
    }
    suspend fun updateCardBalance(cardId: String, newBalance: Double) {
        val card = cardDao.getCardById(cardId)
        card?.let {
            cardDao.updateCard(it.copy(balance = newBalance))
        }
    }

    suspend fun deleteCard(card: Card) {
        cardDao.deleteCard(CardEntity.fromCard(card))
    }
}