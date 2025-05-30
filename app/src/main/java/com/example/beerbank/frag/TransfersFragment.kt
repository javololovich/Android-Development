package com.example.beerbank.frag

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.beerbank.BeerBankApplication
import com.example.beerbank.adapter.CardSpinnerAdapter
import com.example.beerbank.databinding.FragmentTransfersBinding
import com.example.beerbank.model.Card
import com.example.beerbank.service.TransferService
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class TransfersFragment : Fragment() {
    private var _binding: FragmentTransfersBinding? = null
    private val binding get() = _binding!!

    private lateinit var app: BeerBankApplication
    private lateinit var transferService: TransferService

    private var cards = listOf<Card>()
    private var sourceCardPosition = -1
    private var targetCardPosition = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransfersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        app = requireActivity().application as BeerBankApplication

        // Add safety check
        try {
            transferService = app.transferService

            loadCards()
            setupListeners()
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Error initializing transfer service: ${e.message}",
                Snackbar.LENGTH_LONG).show()
        }
    }

    private fun loadCards() {
        lifecycleScope.launch {
            try {
                cards = app.cardRepository.getAllCards()

                // Check if cards list is valid
                if (cards.isEmpty()) {
                    Snackbar.make(
                        binding.root,
                        "You need to add cards before making transfers",
                        Snackbar.LENGTH_LONG
                    ).show()
                    binding.btnTransfer.isEnabled = false
                    return@launch
                }

                if (cards.size < 2) {
                    Snackbar.make(
                        binding.root,
                        "You need at least two cards to make transfers",
                        Snackbar.LENGTH_LONG
                    ).show()
                    binding.btnTransfer.isEnabled = false
                } else {
                    setupCardSpinners()
                }
            } catch (e: Exception) {
                Snackbar.make(
                    binding.root,
                    "Error loading cards: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setupCardSpinners() {
        // From card spinner
        val fromCardAdapter = CardSpinnerAdapter(requireContext(), cards)
        binding.spinnerFromCard.adapter = fromCardAdapter

        // To card spinner
        val toCardAdapter = CardSpinnerAdapter(requireContext(), cards)
        binding.spinnerToCard.adapter = toCardAdapter

        // Set default selections (different cards if possible)
        if (cards.size >= 2) {
            binding.spinnerFromCard.setSelection(0)
            binding.spinnerToCard.setSelection(1)
            sourceCardPosition = 0
            targetCardPosition = 1
        } else if (cards.size == 1) {
            binding.spinnerFromCard.setSelection(0)
            sourceCardPosition = 0
        }
    }

    private fun setupListeners() {
        // Spinner selection listeners
        binding.spinnerFromCard.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                sourceCardPosition = position
                updateTransferButtonState()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                sourceCardPosition = -1
                updateTransferButtonState()
            }
        }

        binding.spinnerToCard.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                targetCardPosition = position
                updateTransferButtonState()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                targetCardPosition = -1
                updateTransferButtonState()
            }
        }

        // Transfer button listener
        binding.btnTransfer.setOnClickListener {
            if (validateTransferData()) {
                processTransfer()
            }
        }
    }

    private fun validateTransferData(): Boolean {
        // Check if source and target cards are selected and not the same
        if (sourceCardPosition == -1) {
            showError("Please select a source card")
            return false
        }

        if (targetCardPosition == -1) {
            showError("Please select a target card")
            return false
        }

        if (sourceCardPosition == targetCardPosition) {
            showError("Source and target cards cannot be the same")
            return false
        }

        // Validate amount
        val amountText = binding.editAmount.text.toString()
        if (amountText.isBlank()) {
            binding.inputLayoutAmount.error = "Please enter an amount"
            return false
        }

        val amount = try {
            amountText.toDouble()
        } catch (e: NumberFormatException) {
            binding.inputLayoutAmount.error = "Invalid amount"
            return false
        }

        if (amount <= 0) {
            binding.inputLayoutAmount.error = "Amount must be greater than zero"
            return false
        }

        // Check if source card has enough balance
        val sourceCard = cards.getOrNull(sourceCardPosition)
        if (sourceCard != null && amount > sourceCard.balance) {
            binding.inputLayoutAmount.error = "Insufficient funds"
            return false
        }

        // Clear any previous errors
        binding.inputLayoutAmount.error = null

        return true
    }

    private fun processTransfer() {
        val sourceCard = cards[sourceCardPosition]
        val targetCard = cards[targetCardPosition]
        val amount = binding.editAmount.text.toString().toDouble()

        // Show loading state
        setLoadingState(true)

        lifecycleScope.launch {
            val result = transferService.transferBetweenCards(
                sourceCardId = sourceCard.id,
                targetCardId = targetCard.id,
                amount = amount
            )

            // Hide loading state
            setLoadingState(false)

            // Process result
            result.fold(
                onSuccess = {
                    // Clear the amount field
                    binding.editAmount.text?.clear()

                    // Show success message
                    Snackbar.make(
                        binding.root,
                        "Transfer successful",
                        Snackbar.LENGTH_LONG
                    ).show()

                    // Reload cards to update balances
                    loadCards()
                },
                onFailure = { error ->
                    showError("Transfer failed: ${error.message}")
                }
            )
        }
    }

    private fun updateTransferButtonState() {
        // Enable transfer button only if different cards are selected
        binding.btnTransfer.isEnabled = sourceCardPosition != -1 &&
                targetCardPosition != -1 &&
                sourceCardPosition != targetCardPosition
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.cardTransfer.isEnabled = !isLoading
        binding.btnTransfer.isEnabled = !isLoading
        binding.spinnerFromCard.isEnabled = !isLoading
        binding.spinnerToCard.isEnabled = !isLoading
        binding.editAmount.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}