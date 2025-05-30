package com.example.beerbank.frag

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.beerbank.BeerBankApplication
import com.example.beerbank.adapter.CardAdapter
import com.example.beerbank.adapter.TransactionAdapter
import com.example.beerbank.databinding.DialogAddCardBinding
import com.example.beerbank.databinding.FragmentCardsBinding
import com.example.beerbank.model.Card
import com.example.beerbank.model.Transaction
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.UUID

class CardsFragment : Fragment() {
    private var _binding: FragmentCardsBinding? = null
    private val binding get() = _binding!!

    private lateinit var app: BeerBankApplication
    private lateinit var cardAdapter: CardAdapter
    private lateinit var transactionAdapter: TransactionAdapter

    private var cards = listOf<Card>()
    private var cardTransactions = listOf<Transaction>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCardsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        app = requireActivity().application as BeerBankApplication

        setupTransactionsRecyclerView()
        loadData()

        // Setup add card button
        binding.btnAddCard.setOnClickListener {
            showAddCardDialog()
        }
    }

    private fun showAddCardDialog() {
        val dialogBinding = DialogAddCardBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Add New Card")
            .setView(dialogBinding.root)
            .setPositiveButton("Add") { _, _ ->
                // This will be overridden below
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()

        // Override the button to prevent automatic dismissal when there's validation errors
        // Inside showAddCardDialog() method
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val cardNumber = dialogBinding.editCardNumber.text.toString()
            val cardHolderName = dialogBinding.editCardHolderName.text.toString()
            val expiryDate = dialogBinding.editExpiryDate.text.toString()
            val cvv = dialogBinding.editCvv.text.toString()
            val initialBalanceString = dialogBinding.editInitialBalance.text.toString()
            val cardType = if (dialogBinding.radioVisa.isChecked) "VISA" else "MASTERCARD"

            // Updated validation for card number - exactly 16 digits
            val digitCount = cardNumber.count { it.isDigit() }
            if (digitCount != 16) {
                dialogBinding.editCardNumber.error = "Card number must be exactly 16 digits"
                return@setOnClickListener
            }

            if (cardHolderName.isBlank()) {
                dialogBinding.editCardHolderName.error = "Please enter cardholder name"
                return@setOnClickListener
            }

            if (!expiryDate.matches(Regex("^\\d{2}/\\d{2}$"))) {
                dialogBinding.editExpiryDate.error = "Please enter a valid expiry date (MM/YY)"
                return@setOnClickListener
            }

            if (cvv.isBlank() || cvv.length < 3) {
                dialogBinding.editCvv.error = "Please enter a valid CVV"
                return@setOnClickListener
            }

            if (initialBalanceString.isBlank()) {
                dialogBinding.editInitialBalance.error = "Please enter initial balance"
                return@setOnClickListener
            }

            val initialBalance = try {
                initialBalanceString.toDouble()
            } catch (e: NumberFormatException) {
                dialogBinding.editInitialBalance.error = "Please enter a valid number"
                return@setOnClickListener
            }

            // All validation passed, create the card
            val formattedCardNumber = formatCardNumber(cardNumber)

            val newCard = Card(
                id = UUID.randomUUID().toString(),
                number = formattedCardNumber,
                holderName = cardHolderName,
                expiryDate = expiryDate,
                balance = initialBalance,
                cardType = cardType,
                cvv = cvv
            )

            // Add the new card to database
            addCardToDatabase(newCard)
            dialog.dismiss()
        }
    }

    private fun formatCardNumber(cardNumber: String): String {
        // Extract only the digits
        val digits = cardNumber.filter { it.isDigit() }

        // Ensure we have exactly 16 digits
        val validDigits = digits.take(16).padEnd(16, '0')

        // Format with spaces every 4 digits
        return buildString {
            validDigits.forEachIndexed { index, c ->
                append(c)
                if ((index + 1) % 4 == 0 && index < validDigits.length - 1) {
                    append(" ")
                }
            }
        }
    }

    private fun addCardToDatabase(card: Card) {
        lifecycleScope.launch {
            try {
                // Ensure card has a valid ID
                val cardWithValidId = if (card.id.isBlank()) {
                    card.copy(id = UUID.randomUUID().toString())
                } else {
                    card
                }

                // First insert the card
                app.cardRepository.insertCard(cardWithValidId)

                // Wait a moment before refreshing the UI
                kotlinx.coroutines.delay(200)

                // Load the updated data from database within a try-catch to isolate UI update errors
                try {
                    cards = app.cardRepository.getAllCards()

                    // Hide empty state since we now have at least one card
                    binding.emptyStateText.visibility = View.GONE

                    setupCardRecyclerView()

                    // Find and select the newly added card
                    val newCardIndex = cards.indexOfFirst { it.id == cardWithValidId.id }
                    if (newCardIndex >= 0) {
                        // Update transactions for the newly added card
                        updateTransactionsForCard(cards[newCardIndex])
                    }
                } catch (e: Exception) {
                    Log.e("CardsFragment", "Error updating UI after card insert: ${e.message}", e)
                }

                Snackbar.make(binding.root, "Card added successfully", Snackbar.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // Log the exception for debugging
                Log.e("CardsFragment", "Error adding card: ${e.message}", e)
                Snackbar.make(binding.root, "Failed to add card: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            // Load cards from repository
            cards = app.cardRepository.getAllCards()

            // Initialize with empty transactions first
            setupCardRecyclerView()

            // Select the first card if available
            if (cards.isNotEmpty()) {
                updateTransactionsForCard(cards[0])
            } else {
                showEmptyState()
            }
        }
    }

    private fun setupCardRecyclerView() {
        cardAdapter = CardAdapter(cards) { selectedCard ->
            updateTransactionsForCard(selectedCard)
        }

        binding.recyclerCards.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = cardAdapter

            // Add this for snap scrolling
            val snapHelper = PagerSnapHelper()
            snapHelper.attachToRecyclerView(this)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        // Find the card that is snapped to center
                        val centerView = snapHelper.findSnapView(layoutManager)
                        centerView?.let {
                            val position = layoutManager?.getPosition(it) ?: 0
                            if (position != RecyclerView.NO_POSITION && position < cards.size) {
                                // Update the selected card in adapter
                                updateSelectedPosition(position)
                                // Update transactions for the card at this position
                                updateTransactionsForCard(cards[position])
                            }
                        }
                    }
                }
            })
        }
    }

    // Add this method to CardAdapter class
    fun updateSelectedPosition(position: Int) = with(cardAdapter){
        val previousSelected = selectedPosition
        selectedPosition = position
        notifyItemChanged(previousSelected)
        notifyItemChanged(selectedPosition)
        onCardSelected(cards[position])
    }

    private fun setupTransactionsRecyclerView() {
        transactionAdapter = TransactionAdapter(cardTransactions)

        binding.recyclerTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
        }
    }

    private fun updateTransactionsForCard(card: Card) {
        lifecycleScope.launch {
            try {
                // Show transactions view since we have at least one card
                binding.recyclerTransactions.visibility = View.VISIBLE

                // Load transactions for this card
                cardTransactions = app.transactionRepository.getTransactionsForCard(card.id)

                // Update the adapter
                transactionAdapter = TransactionAdapter(cardTransactions)
                binding.recyclerTransactions.adapter = transactionAdapter

                // Update title to show which card's transactions we're viewing
                binding.titleTransactions.text = "Transactions: ${card.cardType} ****${card.number.takeLast(4)}"

                // Get total card count first to determine if this is the first and only card
                val cardCount = app.cardRepository.getCardCount()

                if (cardTransactions.isEmpty()) {
                    if (cardCount == 1) {
                        // This is the first card and it has no transactions yet
                        // Don't show any empty state message
                        binding.emptyStateText.visibility = View.GONE
                    } else {
                        // This is not the first card, so show "No transactions" message
                        binding.emptyStateText.text = "No transactions for this card"
                        binding.emptyStateText.visibility = View.VISIBLE
                    }
                } else {
                    // There are transactions, so hide any empty state message
                    binding.emptyStateText.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e("CardsFragment", "Error updating transactions: ${e.message}", e)
            }
        }
    }

    private fun showEmptyState() {
        binding.emptyStateText.text = "No cards available"
        binding.emptyStateText.visibility = View.VISIBLE
        binding.recyclerTransactions.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}