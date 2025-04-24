package com.example.seouldata.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.seouldata.dto.FacilitySummaryItem

class FacilityAdapter(
    //FacilitySummaryItem : 전체 객체
    private var items: MutableList<FacilitySummaryItem>,
    private val onClick: (FacilitySummaryItem) -> Unit
) : RecyclerView.Adapter<FacilityAdapter.FacilityViewHolder>() {

    inner class FacilityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtName: TextView = itemView.findViewById(android.R.id.text1)

        fun bind(item: FacilitySummaryItem) {
            txtName.text = item.svcName
            // 리스트에 표시될것 이미지나 상태 등 추가 바인딩
            itemView.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FacilityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return FacilityViewHolder(view)
    }

    override fun onBindViewHolder(holder: FacilityViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun addItems(newItems: List<FacilitySummaryItem>) {
        val start = items.size
        (items as MutableList).addAll(newItems)
        notifyItemRangeInserted(start, newItems.size)
    }

}