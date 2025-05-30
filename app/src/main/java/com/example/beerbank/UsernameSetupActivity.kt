package com.example.beerbank

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.example.beerbank.service.EmailService
import com.example.beerbank.service.OTPService
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class UsernameSetupActivity : AppCompatActivity() {

    companion object {
        private const val PREF_NAME = "BeerBankPrefs"
        private const val KEY_PIN = "user_pin"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "user_email"
    }

    private lateinit var pin: String
    private lateinit var editUsername: TextInputEditText
    private lateinit var editEmail: TextInputEditText
    private lateinit var btnConfirm: Button

    // Services for email verification
    private lateinit var otpService: OTPService
    private lateinit var emailService: EmailService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_username_setup)

        // Initialize services
        otpService = (application as BeerBankApplication).otpService
        emailService = (application as BeerBankApplication).emailService

        // Get PIN from intent
        pin = intent.getStringExtra("pin") ?: ""
        if (pin.isEmpty()) {
            // Something went wrong, go back to login
            finish()
            return
        }

        // Get UI references
        val usernameInputLayout = findViewById<TextInputLayout>(R.id.input_layout)
        val emailInputLayout = findViewById<TextInputLayout>(R.id.email_input_layout)

        editUsername = usernameInputLayout.findViewById(R.id.edit_username)
        editEmail = emailInputLayout.findViewById(R.id.edit_email)
        btnConfirm = findViewById(R.id.btn_confirm)

        btnConfirm.setOnClickListener {
            val username = editUsername.text.toString().trim()
            val email = editEmail.text.toString().trim()

            if (username.isEmpty()) {
                showError("Please enter your name")
                return@setOnClickListener
            }

            // Validate email format
            if (!isValidEmail(email)) {
                showError("Please enter a valid email address")
                return@setOnClickListener
            }

            // Start the email verification process
            sendVerificationEmail(username, email, pin)
        }
    }

    private fun sendVerificationEmail(username: String, email: String, pin: String) {
        val otp = otpService.generateOTP()

        // Show loading indicator
        Toast.makeText(this, "Sending verification code to your email...", Toast.LENGTH_SHORT).show()

        emailService.sendOTPEmail(email, otp, object : EmailService.EmailCallback {
            override fun onSuccess() {
                showOTPVerificationDialog(username, email, pin)
            }

            override fun onError(error: String) {
                // Even on error, we should show the OTP dialog since we're in debug mode
                showOTPVerificationDialog(username, email, pin)
            }
        })
    }

    private fun showOTPVerificationDialog(username: String, email: String, pin: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_otp_input, null)
        val otpInput = dialogView.findViewById<EditText>(R.id.input_otp)
        val resendText = dialogView.findViewById<TextView>(R.id.text_resend)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Verify Email Address")
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("Verify", null)
            .create()

        dialog.show()

        // Set the positive button listener after showing dialog
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val enteredOTP = otpInput.text.toString()
            if (enteredOTP.length != 6) {
                Toast.makeText(this, "Please enter the 6-digit code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (otpService.verifyOTP(enteredOTP)) {
                // OTP is verified, save user info and proceed
                saveUserInfo(username, email, pin)
                dialog.dismiss()

                // Proceed to main activity
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity() // Close all previous activities
            } else {
                Toast.makeText(this, "Invalid or expired code", Toast.LENGTH_SHORT).show()
            }
        }

        // Setup resend option
        resendText.setOnClickListener {
            dialog.dismiss()
            sendVerificationEmail(username, email, pin)
        }
    }

    private fun saveUserInfo(username: String, email: String, pin: String) {
        getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_USERNAME, username)
            putString(KEY_EMAIL, email)
            putString(KEY_PIN, pin)
            apply()
        }

        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Email validation helper
    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}