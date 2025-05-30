package com.example.beerbank.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.beerbank.R
import com.example.beerbank.model.Card
import java.text.NumberFormat
import java.util.Locale

class CardSpinnerAdapter(
    context: Context,
    private val cards: List<Card>
) : ArrayAdapter<Card>(context, R.layout.item_card_spinner, cards) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_card_spinner, parent, false)

        val card = getItem(position)
        if (card != null) {
            bindCard(view, card)
        }

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_card_spinner_dropdown, parent, false)

        val card = getItem(position)
        if (card != null) {
            bindDropDownCard(view, card)
        }

        return view
    }

    private fun bindCard(view: View, card: Card?) {
        if (card == null) return

        val textCardNumber = view.findViewById<TextView>(R.id.text_card_number)
        val textCardType = view.findViewById<TextView>(R.id.text_card_type)

        textCardNumber?.text = "**** " + card.number.takeLast(4)
        textCardType?.text = card.cardType
    }

    private fun bindDropDownCard(view: View, card: Card?) {
        if (card == null) return

        val textCardNumber = view.findViewById<TextView>(R.id.text_card_number)
        val textCardType = view.findViewById<TextView>(R.id.text_card_type)
        val textCardBalance = view.findViewById<TextView>(R.id.text_card_balance)

        textCardNumber?.text = "**** " + card.number.takeLast(4)
        textCardType?.text = card.cardType

        val format = NumberFormat.getCurrencyInstance(Locale.US)
        textCardBalance?.text = format.format(card.balance)
    }
}