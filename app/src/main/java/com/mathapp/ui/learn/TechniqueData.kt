package com.mathapp.ui.learn

import com.mathapp.data.MathOperation

data class Technique(
    val id: String,
    val category: String,
    val title: String,
    val why: String,
    val howTo: List<String>,
    val examples: List<Pair<String, String>>,  // problem → inner monologue
    val relatedOperation: MathOperation? = null
)

val ALL_TECHNIQUES: List<Technique> = listOf(

    // ── FOUNDATIONS ─────────────────────────────────────────────────────────

    Technique(
        id = "anchors",
        category = "Foundations",
        title = "Anchors & Reference Numbers",
        why = "Our brains recall round numbers (5, 10, 20, 100) instantly. " +
              "Using a nearby anchor turns any problem into two easy steps: " +
              "adjust to the anchor, then correct the difference.",
        howTo = listOf(
            "Choose a round anchor near one of your numbers (10, 20, 25, 50, 100 are ideal).",
            "Calculate using the anchor instead.",
            "Add or subtract the difference to get the true answer."
        ),
        examples = listOf(
            "9 × 7" to "9 × 7 → use anchor 10: 10 × 7 = 70, then − 7 = 63",
            "8 × 6" to "8 × 6 → use anchor 10: 10 × 6 = 60, then − 12 = 48",
            "37 + 9" to "37 + 9 → use anchor 10: 37 + 10 = 47, then − 1 = 46",
            "48 × 3" to "48 × 3 → use anchor 50: 50 × 3 = 150, then − 6 = 144"
        ),
        relatedOperation = MathOperation.MULTIPLICATION
    ),

    Technique(
        id = "complement_pairs",
        category = "Foundations",
        title = "Complement Pairs (Number Bonds)",
        why = "Knowing which pairs sum to 10, 20, or 100 lets you spot simplifications instantly — " +
              "a key building block for fast mental addition and subtraction.",
        howTo = listOf(
            "Memorise pairs that sum to 10: (1,9), (2,8), (3,7), (4,6), (5,5).",
            "For larger numbers: pairs to 100 follow the same pattern (37+63, 45+55).",
            "When adding, look for a complement that brings you to a round number."
        ),
        examples = listOf(
            "37 + 63" to "37 + 63: 7 + 3 = 10, 30 + 60 = 90, total = 100",
            "47 + 28" to "47 + 28: 47 + 3 = 50 (bond!), then 50 + 25 = 75",
            "73 − 38" to "73 − 38: 38 + 2 = 40 (bond to 40), 40 + 33 = 73, so 33 + 2 = 35"
        ),
        relatedOperation = MathOperation.ADDITION
    ),

    Technique(
        id = "decomposition",
        category = "Foundations",
        title = "Breaking Apart (Decomposition)",
        why = "Complex sums become trivial when you split one number into friendly parts. " +
              "This is the most broadly applicable mental math strategy.",
        howTo = listOf(
            "Split one number into two parts that are easier to work with.",
            "Apply the operation to each part separately.",
            "Combine the results."
        ),
        examples = listOf(
            "37 + 48" to "37 + 48: split 48 → 40 + 8. 37+40=77, 77+8=85",
            "7 × 13" to "7 × 13: split 13 → 10+3. 7×10=70, 7×3=21, total 91",
            "6 × 27" to "6 × 27: split 27 → 25+2. 6×25=150, 6×2=12, total 162",
            "84 − 37" to "84 − 37: split 37 → 30+7. 84−30=54, 54−7=47"
        ),
        relatedOperation = MathOperation.MULTIPLICATION
    ),

    // ── MULTIPLICATION SHORTCUTS ─────────────────────────────────────────────

    Technique(
        id = "9s_trick",
        category = "Multiplication Shortcuts",
        title = "The 9s Trick",
        why = "9 × n = 10n − n. Multiplying by 10 is trivial, so the 9s table becomes effortless. " +
              "This is one of the most powerful single shortcuts to internalise.",
        howTo = listOf(
            "To compute 9 × n: first multiply by 10, then subtract n.",
            "Equivalently: the digits of any 9× result (up to 9×10) always sum to 9."
        ),
        examples = listOf(
            "9 × 8" to "9 × 8: 10×8=80, minus 8 = 72",
            "9 × 7" to "9 × 7: 10×7=70, minus 7 = 63",
            "9 × 12" to "9 × 12: 10×12=120, minus 12 = 108",
            "9 × 6" to "9 × 6: 10×6=60, minus 6 = 54"
        ),
        relatedOperation = MathOperation.MULTIPLICATION
    ),

    Technique(
        id = "multiply_by_11",
        category = "Multiplication Shortcuts",
        title = "Multiplying by 11",
        why = "11 × any 2-digit number follows a pattern: spread the digits and insert their sum in the middle. " +
              "Once you see it, you will never forget it.",
        howTo = listOf(
            "For single digits: repeat the digit (11 × 7 = 77).",
            "For 2-digit numbers: write the tens digit, add the two digits, write the units digit.",
            "If the middle sum ≥ 10, carry 1 into the tens digit."
        ),
        examples = listOf(
            "11 × 43" to "11 × 43: digits 4 and 3, middle = 4+3=7 → 473",
            "11 × 62" to "11 × 62: digits 6 and 2, middle = 8 → 682",
            "11 × 75" to "11 × 75: digits 7 and 5, middle = 12 → carry: (7+1)=8, so 825",
            "11 × 9" to "11 × 9: single digit → 99"
        ),
        relatedOperation = MathOperation.MULTIPLICATION
    ),

    Technique(
        id = "doubling_halving",
        category = "Multiplication Shortcuts",
        title = "Doubling & Halving to Simplify ×",
        why = "If one factor is even, halve it and double the other — the product stays the same. " +
              "This converts hard multiplications into easier ones.",
        howTo = listOf(
            "If one number is even, halve it.",
            "Double the other number to compensate.",
            "Repeat until one number becomes easy (like 5 or 10).",
            "Calculate the simplified product."
        ),
        examples = listOf(
            "15 × 12" to "15 × 12 → double 15 = 30, halve 12 = 6 → 30 × 6 = 180",
            "24 × 5" to "24 × 5 → halve 24 = 12, double 5 = 10 → 12 × 10 = 120",
            "16 × 25" to "16 × 25 → halve twice: 4 × 100 = 400",
            "14 × 15" to "14 × 15 → halve 14 = 7, double 15 = 30 → 7 × 30 = 210"
        ),
        relatedOperation = MathOperation.MULTIPLICATION
    ),

    Technique(
        id = "near_square",
        category = "Multiplication Shortcuts",
        title = "Near-Square Shortcut  (a−b)(a+b)",
        why = "The identity (a−b)(a+b) = a²−b² lets you multiply two numbers " +
              "near a square by first recalling the square and subtracting b².",
        howTo = listOf(
            "Find the midpoint a of the two numbers.",
            "Find b (the distance each number is from the midpoint).",
            "Recall a² and subtract b²."
        ),
        examples = listOf(
            "19 × 21" to "19×21: midpoint=20, b=1 → 20²−1²=400−1=399",
            "18 × 22" to "18×22: midpoint=20, b=2 → 400−4=396",
            "24 × 26" to "24×26: midpoint=25, b=1 → 625−1=624",
            "47 × 53" to "47×53: midpoint=50, b=3 → 2500−9=2491"
        ),
        relatedOperation = MathOperation.POWERS
    ),

    // ── PERCENTAGES & FRACTIONS ──────────────────────────────────────────────

    Technique(
        id = "ten_percent",
        category = "Percentages & Fractions",
        title = "The 10% Building Block",
        why = "10% of any number is trivially the number ÷ 10. " +
              "Most other percentages are quick multiples or halves of 10%.",
        howTo = listOf(
            "Find 10%: move the decimal point one place left.",
            "5% = half of 10%.",
            "20% = double 10%.",
            "15% = 10% + 5%.",
            "25% = double 10% + half 10%  (or ÷ 4).",
            "75% = 3 × 25%."
        ),
        examples = listOf(
            "15% of 80" to "10% of 80=8, 5%=4, total 12",
            "25% of 60" to "10% of 60=6, 25%=6×2.5=15 (or 60÷4=15)",
            "20% of 45" to "10% of 45=4.5, 20%=9",
            "30% of 70" to "10% of 70=7, 30%=21"
        ),
        relatedOperation = MathOperation.PERCENTAGES
    ),

    Technique(
        id = "fraction_decimals",
        category = "Percentages & Fractions",
        title = "Fraction–Decimal Conversions to Know",
        why = "Recognising common fraction-decimal pairs instantly removes a whole " +
              "class of calculations from your mental load.",
        howTo = listOf(
            "Memorise the core conversions:",
            "1/2 = 0.5  |  1/4 = 0.25  |  3/4 = 0.75",
            "1/3 ≈ 0.333  |  2/3 ≈ 0.667",
            "1/5 = 0.2  |  2/5 = 0.4  |  3/5 = 0.6  |  4/5 = 0.8",
            "1/8 = 0.125  |  1/10 = 0.1",
            "Use these as shortcuts when estimating or calculating percentages."
        ),
        examples = listOf(
            "3/4 of 80" to "3/4 = 0.75. 80×0.75: 80×0.5=40, 80×0.25=20, total 60",
            "2/5 of 35" to "2/5 = 0.4. 35×0.4: 35×0.2=7, ×2=14",
            "1/8 of 48" to "1/8 = 0.125. 48÷8=6",
            "3/8 of 24" to "3/8: first 1/8 of 24=3, then ×3=9"
        ),
        relatedOperation = MathOperation.FRACTIONS
    ),

    // ── MULTI-STEP STRATEGY ──────────────────────────────────────────────────

    Technique(
        id = "left_to_right_add",
        category = "Multi-step Strategy",
        title = "Left-to-Right Addition",
        why = "School taught right-to-left with carrying. Left-to-right works with how " +
              "we actually say numbers and avoids carrying confusion for most people.",
        howTo = listOf(
            "Start with the largest place value (hundreds, then tens, then units).",
            "Keep a running total as you move right.",
            "Adjust downward only if needed — no backwards carrying."
        ),
        examples = listOf(
            "356 + 278" to "300+200=500, 50+70=120→620, 6+8=14→634",
            "487 + 354" to "400+300=700, 80+50=130→830, 7+4=11→841",
            "63 + 48" to "60+40=100, 3+8=11→111"
        ),
        relatedOperation = MathOperation.ADDITION
    ),

    Technique(
        id = "multi_step_anchor",
        category = "Multi-step Strategy",
        title = "Multi-step Anchor Operations",
        why = "For larger multiplications, chain the anchor technique across two or more steps. " +
              "Breaking a hard product into easy steps is the key to fast mental arithmetic.",
        howTo = listOf(
            "Break the multiplication into two steps using anchors.",
            "Example strategy for ×15: × 10, then add half of that.",
            "Example strategy for ×12: × 10, then add ×2.",
            "Example strategy for ×9: × 10, then subtract the original.",
            "Chain these for complex multiplications."
        ),
        examples = listOf(
            "13 × 15" to "13×15: 13×10=130, 13×5=65 (half of 130), total=195",
            "24 × 12" to "24×12: 24×10=240, 24×2=48, total=288",
            "37 × 11" to "37×11: 37×10=370, +37=407",
            "16 × 25" to "16×25: 16×100=1600, ÷4=400"
        ),
        relatedOperation = MathOperation.MULTIPLICATION
    ),

    Technique(
        id = "rearranging",
        category = "Multi-step Strategy",
        title = "Rearranging for Easier Computation",
        why = "Multiplication and addition are commutative and associative — you can freely " +
              "reorder and regroup to find the path of least resistance.",
        howTo = listOf(
            "Reorder factors to put the easiest one last.",
            "Regroup to create round-number pairs.",
            "Look for factors of 10, 100, or other anchors hidden in the expression."
        ),
        examples = listOf(
            "5 × 7 × 2" to "5 × 7 × 2: reorder → 5×2×7=10×7=70",
            "4 × 8 × 25" to "4 × 8 × 25: regroup → 4×25=100, then ×8=800",
            "17 + 38 + 3" to "17+38+3: regroup → 17+3=20, +38=58",
            "125 × 8" to "125×8: 125×8=1000 (since 8×125=1000, a known pair)"
        ),
        relatedOperation = MathOperation.MIXED
    )
)

/** Group techniques by category. */
val TECHNIQUE_CATEGORIES: List<Pair<String, List<Technique>>> by lazy {
    ALL_TECHNIQUES
        .groupBy { it.category }
        .entries
        .map { it.key to it.value }
}

fun techniqueById(id: String): Technique? = ALL_TECHNIQUES.find { it.id == id }
