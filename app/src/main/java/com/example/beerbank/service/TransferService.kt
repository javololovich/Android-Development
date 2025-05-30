package com.example.beerbank.service

import com.example.beerbank.model.Card
import com.example.beerbank.model.Transaction
import com.example.beerbank.repository.CardRepository
import com.example.beerbank.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransferService(
    private val cardRepository: CardRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend fun transferBetweenCards(
        sourceCardId: String,
        targetCardId: String,
        amount: Double
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Get both cards
            val sourceCard = cardRepository.getCardById(sourceCardId) ?: return@withContext Result.failure(
                Exception("Source card not found")
            )

            val targetCard = cardRepository.getCardById(targetCardId) ?: return@withContext Result.failure(
                Exception("Target card not found")
            )

            // Check if source card has enough balance
            if (sourceCard.balance < amount) {
                return@withContext Result.failure(Exception("Insufficient funds"))
            }

            // Update balances
            val newSourceBalance = sourceCard.balance - amount
            val newTargetBalance = targetCard.balance + amount

            // Create transaction timestamp
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            // Update the cards in database
            cardRepository.updateCard(sourceCard.copy(balance = newSourceBalance))
            cardRepository.updateCard(targetCard.copy(balance = newTargetBalance))

            // Create and save withdrawal transaction
            val withdrawTransaction = Transaction(
                cardId = sourceCardId,
                description = "Transfer to **** ${targetCard.number.takeLast(4)}",
                date = timestamp,
                amount = amount,
                isIncoming = false
            )
            transactionRepository.insertTransaction(withdrawTransaction)

            // Create and save deposit transaction
            val depositTransaction = Transaction(
                cardId = targetCardId,
                description = "Transfer from **** ${sourceCard.number.takeLast(4)}",
                date = timestamp,
                amount = amount,
                isIncoming = true
            )
            transactionRepository.insertTransaction(depositTransaction)

            return@withContext Result.success(Unit)

        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
}