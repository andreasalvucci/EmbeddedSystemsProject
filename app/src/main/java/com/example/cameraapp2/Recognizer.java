package com.example.cameraapp2;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;

public class Recognizer {
    private TextRecognizer recognizer;
    private InputImage inputImage;

    public Recognizer(Bitmap image){
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        inputImage = InputImage.fromBitmap(image,0);
    }

    public int getStopNumber(RecognizerCallback myCallback){
        if(recognizer==null){
            return -1;
        }
        int stopNumber = 0;
        recognizer.process(inputImage).addOnSuccessListener(new OnSuccessListener<Text>() {
            @Override
            public void onSuccess(Text text) {
                List<String> tutteLeParole = getWordsFromText(text);
                String parolaDaEsaminare = "";
                for (String parola:tutteLeParole
                     ) {
                    parolaDaEsaminare+=parola;
                    Log.d("parola da esaminare",parolaDaEsaminare);
                    //if(isAllUpper(parola) && parola.matches("[A-Z]+"))
                }

                        myCallback.onCallBack(parolaDaEsaminare);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
            }
        });




        return stopNumber;
    }

    private static boolean isAllUpper(String s) {
        for(char c : s.toCharArray()) {
            if(Character.isLetter(c) && Character.isLowerCase(c)) {
                return false;
            }
        }
        return true;
    }
    private List<String> getWordsFromText(Text text) {
        String resultText = text.getText();
        List<String> parole = new ArrayList<>();
        for (Text.TextBlock block : text.getTextBlocks()) {
            String blockText = block.getText();
            Point[] blockCornerPoints = block.getCornerPoints();
            Rect blockFrame = block.getBoundingBox();
            for (Text.Line line : block.getLines()) {
                String lineText = line.getText();
                Point[] lineCornerPoints = line.getCornerPoints();
                Rect lineFrame = line.getBoundingBox();
                for (Text.Element element : line.getElements()) {
                    String elementText = element.getText();
                    parole.add(elementText);
                    Point[] elementCornerPoints = element.getCornerPoints();
                    Rect elementFrame = element.getBoundingBox();
                    for (Text.Symbol symbol : element.getSymbols()) {
                        String symbolText = symbol.getText();
                        Point[] symbolCornerPoints = symbol.getCornerPoints();
                        Rect symbolFrame = symbol.getBoundingBox();
                    }
                }
            }
        }
return parole;

    }
}
