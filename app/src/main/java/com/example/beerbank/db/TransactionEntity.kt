package com.example.beerbank.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.beerbank.model.Transaction
import java.util.UUID

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("cardId")]
)
data class TransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val cardId: String,
    val description: String,
    val date: String,
    val amount: Double,
    val isIncoming: Boolean
) {
    fun toTransaction(): Transaction {
        return Transaction(cardId, description, date, amount, isIncoming)
    }

    companion object {
        fun fromTransaction(transaction: Transaction): TransactionEntity {
            return TransactionEntity(
                id = UUID.randomUUID().toString(),
                cardId = transaction.cardId,
                description = transaction.description,
                date = transaction.date,
                amount = transaction.amount,
                isIncoming = transaction.isIncoming
            )
        }
    }
}