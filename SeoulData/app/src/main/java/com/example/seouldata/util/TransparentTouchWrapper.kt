package com.example.seouldata.util

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

class TouchWrapper @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var onTouch: (() -> Unit)? = null

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        when (ev?.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> onTouch?.invoke()
        }
        return super.dispatchTouchEvent(ev)
    }
}
