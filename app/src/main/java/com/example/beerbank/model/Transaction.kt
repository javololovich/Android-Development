
package com.example.beerbank.model

data class Transaction(
    val cardId: String,  // Link to the card
    val description: String,
    val date: String,
    val amount: Double,
    val isIncoming: Boolean
)