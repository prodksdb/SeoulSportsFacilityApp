package com.seouldata.sport.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.TypedValue
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

fun getColoredMarkerBitmap(
    context: Context,
    drawableId: Int,
    color: Int,
    sizeDp: Float = 48f
): BitmapDescriptor {
    val drawable = AppCompatResources.getDrawable(context, drawableId)!!.mutate()
    drawable.setTint(color)

    val sizePx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        sizeDp,
        context.resources.displayMetrics
    ).toInt()

    drawable.setBounds(0, 0, sizePx, sizePx)

    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.draw(canvas)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}