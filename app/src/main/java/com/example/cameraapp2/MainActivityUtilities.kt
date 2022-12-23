package com.example.cameraapp2

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageProxy
import java.util.concurrent.Executor

fun ImageCapture.takePicture(
    executor: Executor,
    callback: OnImageCapturedCallback.(ImageProxy) -> Unit
) {
    this.takePicture(executor, object : OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {
            super.onCaptureSuccess(image)
            callback(image)
        }
    })
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