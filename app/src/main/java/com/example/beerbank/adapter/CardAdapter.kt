// app/src/main/java/com/example/beerbank/adapter/CardAdapter.kt
package com.example.beerbank.adapter
import android.view.View
import androidx.core.content.ContextCompat
import com.example.beerbank.R
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.beerbank.databinding.ItemCardBinding
import com.example.beerbank.model.Card
import java.text.NumberFormat
import java.util.Locale

class CardAdapter(
    private val cards: List<Card>,
    val onCardSelected: (Card) -> Unit
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    var selectedPosition = 0
    // Track visibility state for each card
    private val detailsVisibility = HashMap<String, Boolean>()

    init {
        // Initialize all cards with details visible
        cards.forEach { detailsVisibility[it.id] = true }
    }

    class CardViewHolder(val binding: ItemCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(card: Card, isSelected: Boolean, isDetailsVisible: Boolean) {
            // Set all data values first
            binding.cardNumber.text = card.number
            binding.cardHolder.text = card.holderName
            binding.cardExpiry.text = "Exp: ${card.expiryDate}"
            binding.cardCvv.text = "CVV: ${card.cvv}"

            val format = NumberFormat.getCurrencyInstance(Locale.US)
            binding.cardBalance.text = format.format(card.balance)

            // Set card type icon
            when (card.cardType.uppercase()) {
                "VISA" -> binding.cardTypeIcon.setImageResource(R.drawable.ic_visa)
                "MASTERCARD" -> binding.cardTypeIcon.setImageResource(R.drawable.ic_mastercard)
                else -> binding.cardTypeIcon.visibility = View.GONE
            }

            // Highlight selected card
            binding.root.alpha = if (isSelected) 1.0f else 0.7f
            binding.root.strokeWidth = if (isSelected) 4 else 0
            binding.root.strokeColor = ContextCompat.getColor(binding.root.context, R.color.purple_500)

            // Toggle visibility of real vs masked data
            binding.cardNumber.visibility = if (isDetailsVisible) View.VISIBLE else View.GONE
            binding.cardNumberMasked.visibility = if (isDetailsVisible) View.GONE else View.VISIBLE

            binding.cardBalance.visibility = if (isDetailsVisible) View.VISIBLE else View.GONE
            binding.cardBalanceMasked.visibility = if (isDetailsVisible) View.GONE else View.VISIBLE

            binding.cardHolder.visibility = if (isDetailsVisible) View.VISIBLE else View.GONE
            binding.cardHolderMasked.visibility = if (isDetailsVisible) View.GONE else View.VISIBLE

            binding.cardExpiry.visibility = if (isDetailsVisible) View.VISIBLE else View.GONE
            binding.cardExpiryMasked.visibility = if (isDetailsVisible) View.GONE else View.VISIBLE

            binding.cardCvv.visibility = if (isDetailsVisible) View.VISIBLE else View.GONE
            binding.cardCvvMasked.visibility = if (isDetailsVisible) View.GONE else View.VISIBLE

            // Set appropriate icon for visibility toggle button
            binding.btnToggleDetails.setImageResource(
                if (isDetailsVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val binding = ItemCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return CardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val card = cards[position]
        val isDetailsVisible = detailsVisibility[card.id] ?: true

        holder.bind(card, position == selectedPosition, isDetailsVisible)

        // Handle card selection
        holder.itemView.setOnClickListener {
            val previousSelected = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(previousSelected)
            notifyItemChanged(selectedPosition)
            onCardSelected(card)
        }

        // Handle visibility toggle button click
        holder.binding.btnToggleDetails.setOnClickListener {
            // Toggle visibility state
            detailsVisibility[card.id] = !(detailsVisibility[card.id] ?: true)
            // Refresh the view
            notifyItemChanged(holder.adapterPosition)
        }
    }

    override fun getItemCount() = cards.size

    fun getSelectedCardId(): String? {
        return if (cards.isNotEmpty()) cards[selectedPosition].id else null
    }
}