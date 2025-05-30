//package com.example.beerbank.util
//
//import com.example.beerbank.repository.CardRepository
//import com.example.beerbank.repository.TransactionRepository
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//
//class DataInitializer(
//    private val cardRepository: CardRepository,
//    private val transactionRepository: TransactionRepository
//) {
//    suspend fun initializeDataIfNeeded() = withContext(Dispatchers.IO) {
//        try {
//            // Only clear transactions
//            clearAllTransactions()
//            return@withContext true
//        } catch (e: Exception) {
//            e.printStackTrace()
//            return@withContext false
//        }
//    }
//
//    private suspend fun clearAllTransactions() = withContext(Dispatchers.IO) {
//        // This will delete all transactions from the database
//        val allTransactions = transactionRepository.getAllTransactions()
//        allTransactions.forEach { transaction ->
//            transactionRepository.deleteTransaction(transaction)
//        }
//    }
//}