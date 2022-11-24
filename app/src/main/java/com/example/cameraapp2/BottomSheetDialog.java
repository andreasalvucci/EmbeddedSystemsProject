package com.example.cameraapp2;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class BottomSheetDialog extends BottomSheetDialogFragment {
    private String autobusJSON;
    private List<List<String>> listaAutobus;
    private String stopName;

    public BottomSheetDialog(List<List<String>> lista, String stopName) {
        this.listaAutobus = lista;
        this.stopName = stopName;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable
            ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.bottom_sheet_layout,
                container, false);

        Button buttonChiudi = v.findViewById(R.id.help_close_button);
        TextView nomeFermata = v.findViewById(R.id.nome_fermata);
        TextView codiceBus1 = v.findViewById(R.id.codice_bus_1);
        TextView codiceBus2 = v.findViewById(R.id.codice_bus_2);
        TextView orarioBus1 = v.findViewById(R.id.orario_bus_1);
        TextView orarioBus2 = v.findViewById(R.id.orario_bus_2);
        nomeFermata.setText(stopName);

        codiceBus1.setText(listaAutobus.get(0).get(0));
        orarioBus1.setText(listaAutobus.get(0).get(1));

        codiceBus2.setText(listaAutobus.get(1).get(0));
        orarioBus2.setText(listaAutobus.get(1).get(1));

        buttonChiudi.setOnClickListener(view -> dismiss());
        return v;
    }
}
