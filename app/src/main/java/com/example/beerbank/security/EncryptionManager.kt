package com.example.beerbank.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class EncryptionManager(private val context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val ENCRYPTION_KEY_ALIAS ="BeerBank_MasterKey"
        private const val AES_GCM_NOPADDING = "AES/GCM/NoPadding"
        private const val SHARED_PREFS_NAME = "BeerBank_SecurePrefs"
        private const val DATABASE_PASSWORD_KEY = "db_password"
        private const val IV_KEY = "db_password_iv"
    }

    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    // Create or get master key from Android KeyStore
    private fun getMasterKey(): SecretKey {
        if (!keyStore.containsAlias(ENCRYPTION_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )

            val keyGenSpec = KeyGenParameterSpec.Builder(
                ENCRYPTION_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .build()

            keyGenerator.init(keyGenSpec)
            return keyGenerator.generateKey()
        }

        return keyStore.getKey(ENCRYPTION_KEY_ALIAS, null) as SecretKey
    }

    // Generate and store random database password
    fun getDatabasePassword(): String {
        val sharedPrefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)

        // Check if we already have an encrypted password
        val encryptedData = sharedPrefs.getString(DATABASE_PASSWORD_KEY, null)
        val ivData = sharedPrefs.getString(IV_KEY, null)

        if (encryptedData != null && ivData != null) {
            // Decrypt the existing password
            return decryptDatabasePassword(encryptedData, ivData)
        }

        // Generate a new random 32-byte password
        val random = SecureRandom()
        val passwordBytes = ByteArray(32)
        random.nextBytes(passwordBytes)
        val password = Base64.encodeToString(passwordBytes, Base64.NO_WRAP)

        // Encrypt and store the password
        encryptAndStoreDatabasePassword(password)

        return password
    }

    private fun encryptAndStoreDatabasePassword(password: String) {
        val masterKey = getMasterKey()

        val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
        cipher.init(Cipher.ENCRYPT_MODE, masterKey)

        val iv = cipher.iv
        val encryptedPassword = cipher.doFinal(password.toByteArray())

        val sharedPrefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putString(DATABASE_PASSWORD_KEY, Base64.encodeToString(encryptedPassword, Base64.NO_WRAP))
            .putString(IV_KEY, Base64.encodeToString(iv, Base64.NO_WRAP))
            .apply()
    }

    private fun decryptDatabasePassword(encryptedData: String, ivData: String): String {
        val masterKey = getMasterKey()

        val encryptedBytes = Base64.decode(encryptedData, Base64.NO_WRAP)
        val ivBytes = Base64.decode(ivData, Base64.NO_WRAP)

        val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
        val spec = GCMParameterSpec(128, ivBytes)
        cipher.init(Cipher.DECRYPT_MODE, masterKey, spec)

        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes)
    }


}
