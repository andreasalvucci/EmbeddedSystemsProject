@file:JvmName("MainActivityUtilities")

package com.example.cameraapp2

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import androidx.appcompat.app.AlertDialog
import java.io.ByteArrayOutputStream

fun cropImage(bitmap: Bitmap, frame: View?, reference: View?): ByteArray {
    val heightOriginal = frame!!.height
    val widthOriginal = frame.width
    val heightFrame = reference!!.height
    val widthFrame = reference.width
    val leftFrame = reference.left
    val topFrame = reference.top
    val heightReal = bitmap.height
    val widthReal = bitmap.width
    val widthFinal = widthFrame * widthReal / widthOriginal
    val heightFinal = heightFrame * heightReal / heightOriginal
    val leftFinal = leftFrame * widthReal / widthOriginal
    val topFinal = topFrame * heightReal / heightOriginal
    val bitmapFinal = Bitmap.createBitmap(
        bitmap, leftFinal, topFinal, widthFinal, heightFinal
    )
    val stream = ByteArrayOutputStream()
    bitmapFinal.compress(
        Bitmap.CompressFormat.JPEG, 100, stream
    ) //100 is the best quality possible
    return stream.toByteArray()
}

fun showPermissionsDeniedDialog(context: Context) {
    AlertDialog.Builder(context)
        .setTitle(context.getString(R.string.permission_denied_dialog_title))
        .setMessage(context.getString(R.string.permission_denied_dialog_message))
        .setPositiveButton(R.string.ok_string) { dialog, _ ->
            dialog.dismiss()
        }.show()
}

fun showPositionPermissionsDeniedDialog(context: Context) {
    AlertDialog.Builder(context)
        .setTitle(context.getString(R.string.position_permission_denied_dialog_title))
        .setMessage(context.getString(R.string.position_permission_denied_dialog_message))
        .setPositiveButton(R.string.ok_string) { dialog, _ ->
            dialog.dismiss()
        }.show()
}