package com.example.beerbank.db

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import com.example.beerbank.security.EncryptionManager

object SecureDatabase {
    private const val DATABASE_NAME = "beerbank_db"
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val encryptionManager = EncryptionManager(context)
            val passphrase = encryptionManager.getDatabasePassword()

            val factory = SupportFactory(SQLiteDatabase.getBytes(passphrase.toCharArray()))

            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(factory)
                .build()

            INSTANCE = instance
            instance
        }
    }

    // Optional: method to close the database
    fun closeDatabase() {
        INSTANCE?.let {
            if (it.isOpen) {
                it.close()
            }
            INSTANCE = null
        }
    }

    // Optional: method to handle migration to encrypted database
    fun migratePlainTextToEncrypted(context: Context) {
        // Implementation would depend on your specific needs
        // This might involve exporting data from plain DB and importing to encrypted one
    }
}