package com.example.cameraapp2.textrecognition

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class Recognizer(image: Bitmap) {
    private val textRecognizer: TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val inputImage: InputImage

    init {
        inputImage = InputImage.fromBitmap(image, 0)
    }

    fun getStopNumber(myCallback: (String) -> Unit) {
        textRecognizer.process(inputImage).addOnSuccessListener { text: Text ->
            val allWords = getWordsFromText(text)
            val wordToExamineBuilder = StringBuilder()
            for (word in allWords) {
                wordToExamineBuilder.append(word).append(" ")
            }
            val wordToExamine = wordToExamineBuilder.toString().trim { it <= ' ' }
            myCallback(wordToExamine)
        }.addOnFailureListener { }
    }

    private fun getWordsFromText(text: Text): List<String> {
        val words: MutableList<String> = ArrayList()
        for (block in text.textBlocks) {
            for (line in block.lines) {
                line.boundingBox
                for (element in line.elements) {
                    val elementText = element.text
                    words.add(elementText)
                    element.symbols
                }
            }
        }
        return words
    }
}