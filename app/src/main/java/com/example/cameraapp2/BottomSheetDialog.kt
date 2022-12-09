package com.example.cameraapp2

import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView

class BottomSheetDialog(
    private val busesList: List<List<String>>,
    private val stopName: String
) : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(
            R.layout.bottom_sheet_layout,
            container, false
        )

        val closeButton = view.findViewById<Button>(R.id.help_close_button)
        closeButton.setOnClickListener { dismiss() }

        val stopName = view.findViewById<TextView>(R.id.nome_fermata)
        stopName.text = this.stopName

        val firstBusNumber = view.findViewById<TextView>(R.id.codice_bus_1)
        firstBusNumber.text = busesList[0][0]

        val firstBusArrivalTime = view.findViewById<TextView>(R.id.orario_bus_1)
        firstBusArrivalTime.text = busesList[0][1]

        val secondBusNumber = view.findViewById<TextView>(R.id.codice_bus_2)
        secondBusNumber.text = busesList[1][0]

        val secondBusArrivalTime = view.findViewById<TextView>(R.id.orario_bus_2)
        secondBusArrivalTime.text = busesList[1][1]

        return view
    }
}