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
    public BottomSheetDialog(List<List<String>> lista, String stopName){
        this.listaAutobus=lista;
        this.stopName = stopName;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable
            ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.bottom_sheet_layout,
                container, false);
        if(listaAutobus.size()==0){
            Toast.makeText(inflater.getContext(), "Nessuna altra corsa prevista per oggi", Toast.LENGTH_SHORT).show();
            return null;
        }

        Button button_chiudi = v.findViewById(R.id.retry_button);
        TextView nome_fermata = v.findViewById(R.id.nome_fermata);
        TextView codice_bus1 = v.findViewById(R.id.codice_bus_1);
        TextView codice_bus_2 = v.findViewById(R.id.codice_bus_2);
        TextView orario_bus_1 = v.findViewById(R.id.orario_bus_1);
        TextView orario_bus_2 = v.findViewById(R.id.orario_bus_2);
        nome_fermata.setText(stopName);

        codice_bus1.setText(listaAutobus.get(0).get(0));
        orario_bus_1.setText(listaAutobus.get(0).get(1));

        codice_bus_2.setText(listaAutobus.get(1).get(0));
        orario_bus_2.setText(listaAutobus.get(1).get(1));

        button_chiudi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                dismiss();
            }
        });
        return v;
    }
}
