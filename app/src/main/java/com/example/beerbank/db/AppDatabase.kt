package com.example.beerbank.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.beerbank.db.CardDao
import com.example.beerbank.db.TransactionDao
import com.example.beerbank.model.Card
import com.example.beerbank.model.Transaction

@Database(
    entities = [CardEntity::class, TransactionEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        // For backward compatibility - redirect to SecureDatabase
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return SecureDatabase.getDatabase(context)
        }
    }
}