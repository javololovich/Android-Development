package com.example.beerbank.service

import com.example.beerbank.db.AppDatabase
import com.example.beerbank.model.Transaction
import com.example.beerbank.repository.CardRepository
import com.example.beerbank.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PaymentService(
    private val cardRepository: CardRepository,
    private val transactionRepository: TransactionRepository,
    private val db: AppDatabase
) {
    suspend fun processPayment(cardId: String, recipient: String, amount: Double): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                // Get the card
                val card = cardRepository.getCardById(cardId) ?: throw Exception("Card not found")

                // Check if enough balance
                if (card.balance < amount) throw Exception("Insufficient funds")

                // Update card balance
                val newBalance = card.balance - amount
                cardRepository.updateCard(card.copy(balance = newBalance))

                // In PaymentService.kt
                val transaction = Transaction(
                    cardId = cardId,
                    description = recipient,
                    date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                    amount = amount,
                    isIncoming = false
                )
                transactionRepository.insertTransaction(transaction)
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}