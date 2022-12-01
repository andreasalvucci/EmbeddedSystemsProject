package com.example.cameraapp2

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