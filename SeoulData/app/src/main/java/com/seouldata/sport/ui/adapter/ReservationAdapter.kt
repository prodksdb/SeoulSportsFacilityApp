package com.seouldata.sport.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.seouldata.sport.R
import com.seouldata.sport.databinding.ItemReservationSimpleBinding
import com.seouldata.sport.dto.Reservation


class ReservationAdapter :
    ListAdapter<Reservation, ReservationAdapter.VH>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Reservation>() {
            override fun areItemsTheSame(old: Reservation, new: Reservation) =
                old.placeName == new.placeName && old.dateTime == new.dateTime

            override fun areContentsTheSame(old: Reservation, new: Reservation) =
                old == new
        }
    }

    inner class VH(val binding: ItemReservationSimpleBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemReservationSimpleBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.binding.apply {
            tvPlaceName.text = item.placeName
            tvDateTime.text  = item.dateTime
            tvStatus.text    = item.status.ifBlank { "예약 대기" }
            // 상태별 색상 조절
            val color = when (item.status) {
                "이용 완료" -> R.color.green_primary
                "예약 확정" -> R.color.red_primary
                else        -> R.color.gray_primary
            }
            tvStatus.setTextColor(ContextCompat.getColor(root.context, color))
        }
    }
}
