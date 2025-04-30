package com.example.seouldata.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.seouldata.R
import com.example.seouldata.dto.InventoryItem

class InventoryAdapter(
    private val items: List<InventoryItem>,
    private val onItemClick: (InventoryItem) -> Unit
) : RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION

    inner class InventoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageAsset: ImageView = itemView.findViewById(R.id.imageAsset)

        fun bind(item: InventoryItem, position: Int) {
            imageAsset.setImageResource(item.drawableResId)
            itemView.isSelected = (position == selectedPosition)

            itemView.setOnClickListener {
                val current = adapterPosition
                if (current == RecyclerView.NO_POSITION) return@setOnClickListener

                val previous = selectedPosition
                selectedPosition = current
                notifyItemChanged(previous)
                notifyItemChanged(selectedPosition)
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inventory_asset, parent, false)
        return InventoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, position)
    }

    override fun getItemCount(): Int = items.size
}
