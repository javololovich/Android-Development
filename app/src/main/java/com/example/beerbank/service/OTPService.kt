package com.example.beerbank.service

import android.content.Context
import android.content.SharedPreferences
import java.util.Random
import kotlin.math.abs

class OTPService(private val context: Context) {

    companion object {
        private const val PREF_NAME = "BeerBankPrefs"
        private const val KEY_OTP = "otp_code"
        private const val KEY_OTP_TIMESTAMP = "otp_timestamp"
        private const val OTP_VALID_MINUTES = 5
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // Generate a random 6-digit OTP
    fun generateOTP(): String {
        // This ensures the OTP is always 6 digits by using a format string
        val random = Random()
        // Generate a number between 0-999999
        val number = random.nextInt(1000000)
        // Format as a 6-digit string with leading zeros if needed
        val otp = String.format("%06d", number)

        // Save OTP with timestamp
        prefs.edit()
            .putString(KEY_OTP, otp)
            .putLong(KEY_OTP_TIMESTAMP, System.currentTimeMillis())
            .apply()

        return otp
    }

    // Verify the OTP
    fun verifyOTP(enteredOTP: String): Boolean {
        val storedOTP = prefs.getString(KEY_OTP, "") ?: ""
        val timestamp = prefs.getLong(KEY_OTP_TIMESTAMP, 0)
        val currentTime = System.currentTimeMillis()

        // Check if OTP is valid (matches and not expired)
        val valid = enteredOTP == storedOTP &&
                isOTPStillValid(timestamp, currentTime)

        // Clear OTP after verification attempt
        if (valid) {
            clearOTP()
        }

        return valid
    }

    // Check if OTP is still within valid time window
    private fun isOTPStillValid(timestamp: Long, currentTime: Long): Boolean {
        val elapsedMinutes = (currentTime - timestamp) / (1000 * 60)
        return elapsedMinutes < OTP_VALID_MINUTES
    }

    // Clear stored OTP
    private fun clearOTP() {
        prefs.edit()
            .remove(KEY_OTP)
            .remove(KEY_OTP_TIMESTAMP)
            .apply()
    }
}