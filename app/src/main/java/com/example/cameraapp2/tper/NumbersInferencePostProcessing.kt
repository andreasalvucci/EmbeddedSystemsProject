package com.example.cameraapp2.tper

object NumbersInferencePostProcessing {
    private val numbersToSimilarCharacters = mapOf(
        "0" to listOf("o", "O", "Q", "D"),
        "1" to listOf("i", "l", "I"),
        "2" to listOf("z", "Z"),
        "3" to listOf("E"),
        "4" to listOf("A"),
        "5" to listOf("s", "S"),
        "6" to listOf("g", "G"),
        "7" to listOf("T"),
        "8" to listOf("B"),
        "9" to listOf("q")
    )

    private val charactersToSimilarNumber = mutableMapOf<Char, Char>(
        'o' to '0', 'O' to '0', 'Q' to '0', 'D' to '0',
        'i' to '1', 'l' to '1', 'I' to '1',
        'z' to '2', 'Z' to '2',
        'E' to '3',
        'A' to '4',
        's' to '5', 'S' to '5',
        'g' to '6', 'G' to '6',
        'T' to '7',
        'B' to '8',
        'q' to '9'
    )

    /**
     * Substitute similar characters with the correct number. Characters that are neither digits nor
     * letters are removed.
     *
     * @param string the string to be processed
     * @return the processed string
     */
    fun substitutionStep(string: String): String {
        var result = ""
        for (i in string.indices) {
            val char = string[i]
            if (char.isDigit()) {
                result += char
            } else {
                val similarNumber = charactersToSimilarNumber[char]
                result += similarNumber ?: ""
            }
        }
        return result
    }
}