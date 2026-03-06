package com.mathapp.data

enum class MathOperation(
    val displayName: String,
    val description: String,
    val symbol: String
) {
    MULTIPLICATION("Multiplication", "Times tables", "×"),
    DIVISION("Division", "Divide numbers", "÷"),
    POWERS("Powers", "Exponents", "^"),
    FRACTIONS("Fractions", "Fractions to decimals", "/")
}
