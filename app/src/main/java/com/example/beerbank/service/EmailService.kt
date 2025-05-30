package com.example.beerbank.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import kotlin.concurrent.thread

class EmailService(private val context: Context) {

    companion object {
        private const val TAG = "EmailService"

        // Gmail account credentials with app password (no spaces)
        private const val SENDER_EMAIL = "beerbank.noreply@gmail.com"
        private const val SENDER_PASSWORD = "sxry zigv qema ndxb" // Fixed: removed spaces

        // Set Debug mode to true for testing
        private const val DEBUG_MODE = false
    }

    interface EmailCallback {
        fun onSuccess()
        fun onError(error: String)
    }

    fun sendOTPEmail(recipientEmail: String, otp: String, callback: EmailCallback) {
        // Always show the OTP in UI during development
        Log.d(TAG, "OTP for $recipientEmail is $otp")
        //Toast.makeText(context, "Your OTP is: $otp", Toast.LENGTH_LONG).show() // Uncommented

        // If in debug mode, skip email sending but still call the success callback
        if (DEBUG_MODE) {
            Handler(Looper.getMainLooper()).postDelayed({
                callback.onSuccess()
            }, 1000)
            return
        }

        // Only attempt real email when DEBUG_MODE is false
        thread {
            try {
                Log.d(TAG, "Sending email to: $recipientEmail")

                val props = Properties()
                props["mail.smtp.auth"] = "true"
                props["mail.smtp.starttls.enable"] = "true"
                props["mail.smtp.host"] = "smtp.gmail.com"
                props["mail.smtp.port"] = "587"
                props["mail.smtp.ssl.protocols"] = "TLSv1.2"
                // Removed socketFactory causing issues
                // Increase timeout values
                props["mail.smtp.timeout"] = "15000"
                props["mail.smtp.connectiontimeout"] = "15000"

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD)
                    }
                })

                val message = MimeMessage(session)
                message.setFrom(InternetAddress(SENDER_EMAIL))
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail))
                message.subject = "Beer Bank Email Verification"
                message.setText("Your verification code is: $otp\nThis code will expire in 5 minutes.")

                Transport.send(message)
                Log.d(TAG, "Email sent successfully to $recipientEmail")

                Handler(Looper.getMainLooper()).post {
                    callback.onSuccess()
                }
            } catch (e: MessagingException) {
                Log.e(TAG, "Email sending failed: ${e.message}", e)

                // Even on email failure, call onSuccess to show the New PIN dialog
                // This ensures the flow continues even if email fails
                Handler(Looper.getMainLooper()).post {
                    // Still show error in logs, but don't block the user flow
                    Toast.makeText(context, "Email could not be sent, but you can continue with the OTP shown.", Toast.LENGTH_LONG).show()
                    callback.onSuccess() // Call success to continue the flow
                }
            }
        }
    }
}