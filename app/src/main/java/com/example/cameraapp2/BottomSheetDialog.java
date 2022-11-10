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

import java.sql.Time;
import java.util.List;
import java.util.Map;

public class BottomSheetDialog extends BottomSheetDialogFragment {
    private String autobusJSON;
    private List<List<String>> listaAutobus;
    public BottomSheetDialog(List<List<String>> lista){
        this.listaAutobus=lista;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable
            ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.bottom_sheet_layout,
                container, false);

        Button algo_button = v.findViewById(R.id.algo_button);
        Button course_button = v.findViewById(R.id.course_button);
        TextView autobus1 = v.findViewById(R.id.autobus1);
        TextView autobus2 = v.findViewById(R.id.autobus2);

        autobus1.setText(listaAutobus.get(0).get(0)+":"+listaAutobus.get(0).get(1));
        autobus2.setText(listaAutobus.get(1).get(0)+":"+listaAutobus.get(1).get(1));

        algo_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Toast.makeText(getActivity(),
                                "Algorithm Shared", Toast.LENGTH_SHORT)
                        .show();
                dismiss();
            }
        });

        course_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Toast.makeText(getActivity(),
                                "Course Shared", Toast.LENGTH_SHORT)
                        .show();
                dismiss();
            }
        });
        return v;
    }
}
