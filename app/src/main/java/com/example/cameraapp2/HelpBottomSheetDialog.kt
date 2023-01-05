package com.example.cameraapp2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class HelpBottomSheetDialog : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(
            R.layout.help_bottom_sheet_layout,
            container, false
        )
        val helpCloseButton: Button = view.findViewById(R.id.help_close_button)
        helpCloseButton.setOnClickListener { dismiss() }
        return view
    }
}