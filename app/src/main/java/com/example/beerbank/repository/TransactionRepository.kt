package com.example.beerbank.repository

import com.example.beerbank.db.TransactionDao
import com.example.beerbank.db.TransactionEntity
import com.example.beerbank.model.Transaction

class TransactionRepository(private val transactionDao: TransactionDao) {
    suspend fun getAllTransactions(): List<Transaction> {
        return transactionDao.getAllTransactions().map { it.toTransaction() }
    }

    suspend fun getTransactionsForCard(cardId: String): List<Transaction> {
        return transactionDao.getTransactionsForCard(cardId).map { it.toTransaction() }
    }

    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(TransactionEntity.fromTransaction(transaction))
    }

    suspend fun insertAllTransactions(transactions: List<Transaction>) {
        transactionDao.insertAllTransactions(transactions.map { TransactionEntity.fromTransaction(it) })
    }
    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(TransactionEntity.fromTransaction(transaction))
    }
}