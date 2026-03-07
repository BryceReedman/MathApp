package com.mathapp.data

enum class MathOperation(val displayName: String, val description: String, val symbol: String) {
    MULTIPLICATION("Multiplication",   "Times tables",          "×"),
    DIVISION      ("Division",         "Divide numbers",        "÷"),
    ADDITION      ("Addition",         "Add numbers",           "+"),
    SUBTRACTION   ("Subtraction",      "Subtract numbers",      "−"),
    POWERS        ("Powers",           "Exponents",             "^"),
    SQUARE_ROOTS  ("Square Roots",     "Perfect square roots",  "√"),
    CUBE_ROOTS    ("Cube Roots",       "Perfect cube roots",    "∛"),
    FRACTIONS     ("Fractions",        "Fractions to decimals", "/"),
    PERCENTAGES   ("Percentages",      "Percentage of numbers", "%"),
    FACTORIZATION ("Factor Breakdown", "Find factor pairs",     "÷×"),
    MIXED         ("Mixed Mode",       "Multiple operations",   "∞")
    // ESTIMATION ("Estimation",       "Approximate answers",   "≈")  // future feature
}
