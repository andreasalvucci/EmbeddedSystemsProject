package com.example.cameraapp2.textrecognition;

import android.graphics.Bitmap;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;

public class Recognizer {
    private final TextRecognizer textRecognizer;
    private final InputImage inputImage;

    public Recognizer(Bitmap image) {
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        inputImage = InputImage.fromBitmap(image, 0);
    }

    public void getStopNumber(RecognizerCallback myCallback) {
        textRecognizer.process(inputImage).addOnSuccessListener(text -> {
            List<String> allWords = getWordsFromText(text);
            StringBuilder wordToExamineBuilder = new StringBuilder();

            for (String word : allWords) {
                wordToExamineBuilder.append(word);
            }
            String wordToExamine = wordToExamineBuilder.toString();

            myCallback.onCallBack(wordToExamine);
        }).addOnFailureListener(e -> {
        });
    }

    private List<String> getWordsFromText(Text text) {
        List<String> words = new ArrayList<>();
        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                line.getBoundingBox();
                for (Text.Element element : line.getElements()) {
                    String elementText = element.getText();
                    words.add(elementText);
                    element.getSymbols();
                }
            }
        }

        return words;
    }
}
