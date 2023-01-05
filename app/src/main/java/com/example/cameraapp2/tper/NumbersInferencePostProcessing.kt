package com.example.cameraapp2.tper

object NumbersInferencePostProcessing {
    private const val MAX_NUMBER_OF_EDITS = 2

    private val charactersToSimilarNumber = mutableMapOf(
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
     * letters are removed. The maximum number of edits is [MAX_NUMBER_OF_EDITS], more edits will
     * result in an empty string being returned.
     *
     * @param string the string to be processed
     * @return the processed string
     */
    fun substitutionStep(string: String): String {
        var edits = 0
        var result = ""
        for (i in string.indices) {
            val char = string[i]
            if (char.isDigit()) {
                result += char
            } else {
                if (++edits > MAX_NUMBER_OF_EDITS) {
                    return ""
                }
                val similarNumber = charactersToSimilarNumber[char]
                result += similarNumber ?: ""
            }
        }
        return result
    }
}