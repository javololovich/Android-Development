package com.example.beerbank.frag

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.beerbank.BeerBankApplication
import com.example.beerbank.databinding.FragmentPaymentsBinding
import com.example.beerbank.model.Card
import kotlinx.coroutines.launch

class PaymentsFragment : Fragment() {
    private var _binding: FragmentPaymentsBinding? = null
    private val binding get() = _binding!!

    private lateinit var app: BeerBankApplication
    private var cards = listOf<Card>()
    private var selectedCardId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        app = requireActivity().application as BeerBankApplication

        // Load cards from database
        loadCards()

        binding.btnPay.setOnClickListener {
            val recipient = binding.recipientLayout.editText?.text.toString()
            val amountText = binding.amountLayout.editText?.text.toString()

            if (recipient.isBlank() || amountText.isBlank() || selectedCardId == null) {
                Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            } else {
                try {
                    val amount = amountText.toDouble()
                    processPayment(selectedCardId!!, recipient, amount)
                } catch (e: NumberFormatException) {
                    Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadCards() {
        lifecycleScope.launch {
            cards = app.cardRepository.getAllCards()
            setupCardDropdown()
        }
    }

    private fun setupCardDropdown() {
        // Create card display items for dropdown
        val cardItems = cards.map { "****${it.number.takeLast(4)} - ${it.cardType}" }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            cardItems
        )

        binding.cardDropdown.setAdapter(adapter)

        binding.cardDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedCardId = cards[position].id
        }

        // Set default selection if we have cards
        if (cards.isNotEmpty()) {
            binding.cardDropdown.setText(cardItems[0], false)
            selectedCardId = cards[0].id
        }
    }

    private fun processPayment(cardId: String, recipient: String, amount: Double) {
        lifecycleScope.launch {
            val success = app.paymentService.processPayment(cardId, recipient, amount)

            if (success) {
                val selectedCard = cards.find { it.id == cardId }
                val cardLabel = selectedCard?.let { "****${it.number.takeLast(4)}" } ?: ""

                Toast.makeText(
                    context,
                    "Payment of $$amount to $recipient from card $cardLabel successful",
                    Toast.LENGTH_SHORT
                ).show()

                clearInputs()

                // Refresh card data
                loadCards()
            } else {
                Toast.makeText(
                    context,
                    "Payment failed. Please check your balance and try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun clearInputs() {
        binding.recipientLayout.editText?.setText("")
        binding.amountLayout.editText?.setText("")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}