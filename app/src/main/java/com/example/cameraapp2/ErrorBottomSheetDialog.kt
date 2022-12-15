package com.example.cameraapp2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ErrorBottomSheetDialog : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(
            R.layout.error_bottom_sheet,
            container, false
        )

        val retryButton = view.findViewById<Button>(R.id.error_riprova)
        retryButton.setOnClickListener{dismiss()}

        return view
    }
}