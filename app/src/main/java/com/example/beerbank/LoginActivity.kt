package com.example.beerbank

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.beerbank.security.BiometricAuthManager
import com.example.beerbank.security.EncryptionManager
import com.example.beerbank.service.EmailService
import com.example.beerbank.service.OTPService
import java.util.concurrent.Executor

class LoginActivity : AppCompatActivity() {

    companion object {
        private const val PREF_NAME = "BeerBankPrefs"
        private const val KEY_PIN = "user_pin"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "user_email"
        private const val PIN_LENGTH = 4
        private const val DB_INITIALIZED = "db_initialized"
    }

    private lateinit var pinDots: List<View>
    private lateinit var btnFingerprint: Button
    private lateinit var forgotPinText: TextView
    private val enteredPin = StringBuilder()
    private var isFirstLogin = false

    // Services
    private lateinit var otpService: OTPService
    private lateinit var emailService: EmailService
    private lateinit var biometricAuthManager: BiometricAuthManager
    private lateinit var encryptionManager: EncryptionManager

    // Biometric authentication properties
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize services
        otpService = (application as BeerBankApplication).otpService
        emailService = (application as BeerBankApplication).emailService
        biometricAuthManager = BiometricAuthManager(this)
        encryptionManager = EncryptionManager(this)

        setupViews()
        setupBiometric()
    }

    private fun setupViews() {
        // Setup PIN dots
        pinDots = listOf(
            findViewById(R.id.pin_dot_1),
            findViewById(R.id.pin_dot_2),
            findViewById(R.id.pin_dot_3),
            findViewById(R.id.pin_dot_4)
        )

        // Setup numpad
        setupNumpadButton(findViewById(R.id.btn_0), "0")
        setupNumpadButton(findViewById(R.id.btn_1), "1")
        setupNumpadButton(findViewById(R.id.btn_2), "2")
        setupNumpadButton(findViewById(R.id.btn_3), "3")
        setupNumpadButton(findViewById(R.id.btn_4), "4")
        setupNumpadButton(findViewById(R.id.btn_5), "5")
        setupNumpadButton(findViewById(R.id.btn_6), "6")
        setupNumpadButton(findViewById(R.id.btn_7), "7")
        setupNumpadButton(findViewById(R.id.btn_8), "8")
        setupNumpadButton(findViewById(R.id.btn_9), "9")

        // Setup delete button
        findViewById<ImageButton>(R.id.btn_delete).setOnClickListener {
            deleteLastDigit()
        }

        // Setup biometric button
        btnFingerprint = findViewById(R.id.btn_fingerprint)
        btnFingerprint.setOnClickListener {
            authenticateWithBiometrics()
        }

        // Setup forgot PIN text
        forgotPinText = findViewById(R.id.text_forgot_password)
        forgotPinText.setOnClickListener {
            handleForgotPin()
        }

        // Check if this is the first login
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        isFirstLogin = prefs.getString(KEY_PIN, null) == null

        // Only show biometric option if not first login and biometrics are available
        btnFingerprint.isVisible = !isFirstLogin && biometricAuthManager.canAuthenticate()
    }

    private fun setupNumpadButton(button: Button, digit: String) {
        button.setOnClickListener {
            if (enteredPin.length < PIN_LENGTH) {
                enteredPin.append(digit)
                updatePinDots()

                // If PIN is complete, validate it
                if (enteredPin.length == PIN_LENGTH) {
                    validatePin()
                }
            }
        }
    }

    private fun deleteLastDigit() {
        if (enteredPin.isNotEmpty()) {
            enteredPin.deleteCharAt(enteredPin.length - 1)
            updatePinDots()
        }
    }

    private fun updatePinDots() {
        for (i in pinDots.indices) {
            if (i < enteredPin.length) {
                pinDots[i].setBackgroundResource(R.drawable.pin_dot_filled)
            } else {
                pinDots[i].setBackgroundResource(R.drawable.pin_dot_empty)
            }
        }
    }

    private fun validatePin() {
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val savedPin = prefs.getString(KEY_PIN, null)

        if (isFirstLogin || savedPin == null) {
            // First time login, proceed to username setup
            val intent = Intent(this, com.example.beerbank.UsernameSetupActivity::class.java).apply {
                putExtra("pin", enteredPin.toString())
            }
            startActivity(intent)
            finish()
        } else if (enteredPin.toString() == savedPin) {
            // PIN is correct, initialize the database and proceed
            proceedToMainActivity()
        } else {
            // PIN is incorrect, clear and try again
            Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
            enteredPin.clear()
            updatePinDots()
        }
    }

    private fun proceedToMainActivity() {
        // Ensure the encrypted database is ready before proceeding
        try {
            // This will initialize the encrypted database using the stored key
            val app = application as BeerBankApplication
            val db = app.database // This triggers the secure database initialization

            // If we reach here, database initialization was successful
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } catch (e: Exception) {
            // Handle database initialization error
            Toast.makeText(this, "Error initializing secure database: ${e.message}", Toast.LENGTH_LONG).show()
            enteredPin.clear()
            updatePinDots()
        }
    }

    private fun setupBiometric() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // Authentication succeeded, proceed to main activity
                    proceedToMainActivity()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@LoginActivity, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@LoginActivity, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Authentication")
            .setSubtitle("Log in using your biometric")
            .setDescription("Authenticate to access your secure BeerBank data")
            .setNegativeButtonText("Cancel")
            .build()
    }

    private fun authenticateWithBiometrics() {
        if (biometricAuthManager.canAuthenticate()) {
            biometricPrompt.authenticate(promptInfo)
        } else {
            showBiometricSetupDialog()
        }
    }

    private fun showBiometricSetupDialog() {
        AlertDialog.Builder(this)
            .setTitle("Biometric Authentication Required")
            .setMessage("This app requires biometric authentication for enhanced security. Would you like to set it up now?")
            .setPositiveButton("Yes") { _, _ ->
                biometricAuthManager.openBiometricSettings()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "You can still use PIN authentication", Toast.LENGTH_LONG).show()
            }
            .show()
    }

    // Forgot PIN functionality methods
    private fun handleForgotPin() {
        // Get the stored email
        val email = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_EMAIL, null)

        if (email.isNullOrEmpty()) {
            Toast.makeText(this, "No email address found", Toast.LENGTH_SHORT).show()
            return
        }

        // Show confirmation dialog with email
        showEmailConfirmDialog(email)
    }

    private fun showEmailConfirmDialog(email: String) {
        val view = layoutInflater.inflate(R.layout.dialog_email_confirm, null)
        val emailDisplay = view.findViewById<TextView>(R.id.text_email_display)
        emailDisplay.text = email

        val dialog = AlertDialog.Builder(this)
            .setTitle("Confirm Your Email")
            .setView(view)
            .setPositiveButton("Send Code") { _, _ ->
                sendOTPEmail(email)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun sendOTPEmail(email: String) {
        val otp = otpService.generateOTP()

        // Show loading indicator
        Toast.makeText(this, "Sending code to your email...", Toast.LENGTH_SHORT).show()

        emailService.sendOTPEmail(email, otp, object : EmailService.EmailCallback {
            override fun onSuccess() {
                // Show OTP input dialog
                runOnUiThread {
                    showOTPInputDialog(email)
                }
            }

            override fun onError(error: String) {
                // Even on error, we should show the OTP dialog since we're in debug mode
                // and the OTP is shown in a Toast in the EmailService
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "Email error: $error", Toast.LENGTH_SHORT).show()
                    showOTPInputDialog(email)
                }
            }
        })
    }

    private fun showOTPInputDialog(email: String) {
        val view = layoutInflater.inflate(R.layout.dialog_otp_input, null)
        val otpInput = view.findViewById<EditText>(R.id.input_otp)
        val resendText = view.findViewById<TextView>(R.id.text_resend)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Verify OTP")
            .setView(view)
            .setPositiveButton("Verify", null) // Set to null initially, will set listener later
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        // Set the positive button listener after showing the dialog
        // This prevents the dialog from dismissing when validation fails
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val enteredOTP = otpInput.text.toString()
            if (enteredOTP.length != 6) {
                Toast.makeText(this, "Please enter the 6-digit code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (otpService.verifyOTP(enteredOTP)) {
                // OTP is verified, proceed to PIN reset
                dialog.dismiss()
                showNewPINDialog()
            } else {
                Toast.makeText(this, "Invalid or expired code", Toast.LENGTH_SHORT).show()
            }
        }

        // Setup resend option
        resendText.setOnClickListener {
            dialog.dismiss()
            sendOTPEmail(email)
        }
    }

    private fun showNewPINDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_new_pin, null)
        val newPinInput = view.findViewById<EditText>(R.id.input_new_pin)
        val confirmPinInput = view.findViewById<EditText>(R.id.input_confirm_pin)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Reset PIN")
            .setView(view)
            .setPositiveButton("Save", null) // Set to null initially, will set listener later
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        // Override the positive button to prevent dialog from dismissing on invalid PIN
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val newPin = newPinInput.text.toString()
            val confirmPin = confirmPinInput.text.toString()

            // Validate PIN
            if (newPin.length != PIN_LENGTH) {
                Toast.makeText(this, "PIN must be $PIN_LENGTH digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPin != confirmPin) {
                Toast.makeText(this, "PINs don't match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save new PIN
            saveNewPin(newPin)
            dialog.dismiss()
        }
    }

    private fun saveNewPin(newPin: String) {
        // Save the new PIN
        getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PIN, newPin)
            .apply()

        // Clear the entered PIN and update UI
        enteredPin.clear()
        updatePinDots()

        Toast.makeText(this, "PIN reset successfully", Toast.LENGTH_SHORT).show()
    }
}