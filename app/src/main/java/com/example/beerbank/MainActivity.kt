package com.example.beerbank

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.beerbank.databinding.ActivityMainBinding
import com.example.beerbank.frag.CardsFragment
import com.example.beerbank.frag.PaymentsFragment
import com.example.beerbank.frag.TransfersFragment  // Fix this import
import com.example.beerbank.security.EncryptionManager

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Set default fragment
            if (savedInstanceState == null) {
                replaceFragment(CardsFragment())
            }

            setupBottomNavigation()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onCreate: ${e.message}")
            Toast.makeText(this, "Error initializing app: ${e.message}", Toast.LENGTH_LONG).show()
        }
        // TEMPORARY DEBUG CODE - REMOVE BEFORE PRODUCTION
        val encryptionManager = EncryptionManager(this)
        val dbKey = encryptionManager.getEncryptionKeyForDebug()
        Log.d("DatabaseDebug", "Key for SQLCipher: $dbKey")
    }

    private fun setupBottomNavigation() {
        try {
            binding.bottomNavigation.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.navigation_cards -> replaceFragment(CardsFragment())
                    R.id.navigation_transfers -> replaceFragment(TransfersFragment())  // Make sure this uses TransfersFragment
                    R.id.navigation_payments -> replaceFragment(PaymentsFragment())
                    else -> false
                }
                true
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up navigation: ${e.message}")
            Toast.makeText(this, "Error initializing navigation", Toast.LENGTH_LONG).show()
        }
    }

    private fun replaceFragment(fragment: Fragment): Boolean {
        try {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
            return true
        } catch (e: Exception) {
            Log.e("MainActivity", "Error replacing fragment: ${e.message}")
            return false
        }
    }
}