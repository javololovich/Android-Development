package com.example.beerbank

    import android.app.Application
    import com.example.beerbank.db.AppDatabase
    import com.example.beerbank.db.SecureDatabase  // Add this import
    import com.example.beerbank.repository.CardRepository
    import com.example.beerbank.repository.TransactionRepository
    import com.example.beerbank.service.EmailService
    import com.example.beerbank.service.OTPService
    import com.example.beerbank.service.PaymentService
    import com.example.beerbank.service.TransferService
    import kotlinx.coroutines.CoroutineScope
    import kotlinx.coroutines.SupervisorJob

    class BeerBankApplication : Application() {
        private val applicationScope = CoroutineScope(SupervisorJob())
        val otpService by lazy { OTPService(this) }
        val emailService by lazy { EmailService(this) }

        // Fix the database initialization to use proper Kotlin syntax
        val database by lazy { SecureDatabase.getDatabase(this) }

        val cardRepository by lazy { CardRepository(database.cardDao()) }
        val transactionRepository by lazy { TransactionRepository(database.transactionDao()) }
        val paymentService by lazy { PaymentService(cardRepository, transactionRepository, database) }
        val transferService by lazy {
            TransferService(cardRepository, transactionRepository)
        }

        override fun onCreate() {
            super.onCreate()
            // No additional initialization needed
        }
    }