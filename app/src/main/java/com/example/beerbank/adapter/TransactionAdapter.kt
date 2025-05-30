package com.example.beerbank.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.beerbank.R
import com.example.beerbank.databinding.ItemTransactionBinding
import com.example.beerbank.model.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter(private val transactions: List<Transaction>) :
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            binding.transactionDescription.text = transaction.description
            val originalFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val displayFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

            val date = originalFormat.parse(transaction.date)
            date?.let {
                binding.transactionDate.text = displayFormat.format(it)
            } ?: run {
                binding.transactionDate.text = transaction.date
            }

            val format = NumberFormat.getCurrencyInstance(Locale.US)
            val prefix = if (transaction.isIncoming) "+ " else "- "
            binding.transactionAmount.text = prefix + format.format(transaction.amount)

            // Set different colors for income vs expense
            val colorRes = if (transaction.isIncoming) {
                R.color.income_green
            } else {
                R.color.expense_red
            }

            try {
                binding.transactionAmount.setTextColor(
                    ContextCompat.getColor(binding.root.context, colorRes)
                )
            } catch (e: Exception) {
                // Fallback colors if resource not found
                binding.transactionAmount.setTextColor(
                    if (transaction.isIncoming) Color.GREEN else Color.RED
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(transactions[position])
    }

    override fun getItemCount() = transactions.size
}