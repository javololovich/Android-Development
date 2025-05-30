// app/src/main/java/com/example/beerbank/model/Card.kt
package com.example.beerbank.model

data class Card(
    val id: String,
    val number: String,
    val holderName: String,
    val expiryDate: String,
    val balance: Double,
    val cardType: String,
    val cvv: String = "123"  // Default value for backward compatibility
)
