package com.example.cameraapp2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

public class HelpBottomSheetDialog extends BottomSheetDialogFragment {
    private MaterialButton helpCloseButton;

    public View onCreateView(LayoutInflater inflater, @Nullable
            ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.help_bottom_sheet_layout,
                container, false);
        helpCloseButton = v.findViewById(R.id.help_close_button);
        helpCloseButton.setOnClickListener(view -> dismiss());

        return v;
    }
}
