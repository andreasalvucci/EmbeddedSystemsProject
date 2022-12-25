package com.example.cameraapp2

import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView

class BottomSheetDialog(
    private val busesList: MutableList<List<String>>,
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

        // fill busesList with N/A if length is less than 2
        for (i in 0 until 2 - busesList.size) {
            busesList.add(listOf("N/A", "N/A"))
        }

        // create map of TextViews
        val busesTextViewsMap = mapOf<TextView, TextView>(
            view.findViewById<TextView>(R.id.codice_bus_1) to view.findViewById(R.id.orario_bus_1),
            view.findViewById<TextView>(R.id.codice_bus_2) to view.findViewById(R.id.orario_bus_2)
        )

        // fill TextViews with busesList
        for ((i, bus) in busesList.withIndex()) {
            if (i == 2) break
            busesTextViewsMap.keys.elementAt(i).text = bus[0]
            busesTextViewsMap.values.elementAt(i).text = bus[1]
        }

        return view
    }
}