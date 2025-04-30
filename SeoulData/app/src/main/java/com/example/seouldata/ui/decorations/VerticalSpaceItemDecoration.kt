package com.example.seouldata.ui.decorations

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class VerticalSpaceItemDecoration(private val spaceHeight: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)

        if (position == 0) {
            // 첫 번째 아이템은 위쪽에 간격 추가
            outRect.top = spaceHeight
        }
        // 모든 아이템은 아래쪽 간격
        outRect.bottom = spaceHeight
    }
}
