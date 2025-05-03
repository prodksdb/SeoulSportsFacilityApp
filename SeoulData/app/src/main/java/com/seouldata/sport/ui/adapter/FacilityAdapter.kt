package com.seouldata.sport.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.seouldata.sport.R
import com.seouldata.sport.dto.FacilitySummaryItem

class FacilityAdapter(
    private var items: MutableList<FacilitySummaryItem>,
    private val onClick: (FacilitySummaryItem) -> Unit
) : RecyclerView.Adapter<FacilityAdapter.FacilityViewHolder>() {

    inner class FacilityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtName: TextView = itemView.findViewById(R.id.itemTitle)
        val txtDescription: TextView = itemView.findViewById(R.id.itemDescription)
        val itemImage: ImageView = itemView.findViewById(R.id.itemImage)

        fun bind(item: FacilitySummaryItem) {
            txtName.text = item.placeName
            txtDescription.text = item.svcName  // 예시로 svcName을 설명으로 설정

            // 이미지를 URL로 로드할 경우(Glide 사용)
            Glide.with(itemView.context)
                .load(item.imgUrl) //item.imageUrl이 이미지 URL이라고 가정
                .into(itemImage)

            itemView.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FacilityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_layout, parent, false) // item_layout.xml을 연결
        return FacilityViewHolder(view)
    }

    override fun onBindViewHolder(holder: FacilityViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateItems(newItems: List<FacilitySummaryItem>) {
        // 🔽 이 부분을 contentsEquals 로 바꿉니다.
        items = newItems.toMutableList()  // 🔁 새 리스트로 덮어씌움
        notifyDataSetChanged()
    }
}
