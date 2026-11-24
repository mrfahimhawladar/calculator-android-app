package app.calculator.offline.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/** A small recursive-descent evaluator. It never executes arbitrary code. */
object ExpressionEvaluator {
    enum class AngleMode { DEG, RAD }

    fun evaluate(expression: String, angleMode: AngleMode = AngleMode.DEG): Double {
        val parser = Parser(expression, angleMode)
        val value = parser.parseExpression()
        parser.skipWhitespace()
        require(parser.atEnd()) { "Unexpected input" }
        require(value.isFinite()) { "Result is out of range" }
        return if (value == 0.0) 0.0 else value
    }

    fun format(value: Double): String {
        require(value.isFinite()) { "Result is out of range" }
        if (value == 0.0) return "0"
        val rounded = java.math.BigDecimal.valueOf(value)
            .round(java.math.MathContext(12, java.math.RoundingMode.HALF_UP))
            .stripTrailingZeros()
        return rounded.toPlainString()
    }

    private class Parser(
        private val source: String,
        private val angleMode: AngleMode
    ) {
        private var index = 0

        fun atEnd(): Boolean = index >= source.length

        fun skipWhitespace() {
            while (!atEnd() && source[index].isWhitespace()) index++
        }

        fun parseExpression(): Double = parseAdditive()

        private fun parseAdditive(): Double {
            var value = parseMultiplicative()
            while (true) {
                skipWhitespace()
                value = when {
                    take('+') -> value + parseMultiplicative()
                    take('-') || take('−') -> value - parseMultiplicative()
                    else -> return value
                }
            }
        }

        private fun parseMultiplicative(): Double {
            var value = parseUnary()
            while (true) {
                skipWhitespace()
                value = when {
                    take('*') || take('×') -> value * parseUnary()
                    take('/') || take('÷') -> {
                        val divisor = parseUnary()
                        require(divisor != 0.0) { "Cannot divide by zero" }
                        value / divisor
                    }
                    else -> return value
                }
            }
        }

        private fun parseUnary(): Double {
            skipWhitespace()
            return when {
                take('+') -> parseUnary()
                take('-') || take('−') -> -parseUnary()
                else -> parsePower()
            }
        }

        private fun parsePower(): Double {
            val base = parsePostfix()
            skipWhitespace()
            return if (take('^')) base.pow(parseUnary()) else base
        }

        private fun parsePostfix(): Double {
            var value = parsePrimary()
            while (true) {
                skipWhitespace()
                value = when {
                    take('%') -> value / 100.0
                    take('!') -> factorial(value)
                    else -> return value
                }
            }
        }

        private fun parsePrimary(): Double {
            skipWhitespace()
            require(!atEnd()) { "Incomplete expression" }
            return when {
                take('(') -> {
                    val nested = parseExpression()
                    skipWhitespace()
                    require(take(')')) { "Missing closing parenthesis" }
                    nested
                }
                source[index].isDigit() || source[index] == '.' -> parseNumber()
                source[index].isLetter() || source[index] == 'π' -> parseIdentifier()
                else -> throw IllegalArgumentException("Unexpected character")
            }
        }

        private fun parseNumber(): Double {
            val start = index
            var decimalSeen = false
            while (!atEnd()) {
                val c = source[index]
                when {
                    c.isDigit() -> index++
                    c == '.' && !decimalSeen -> {
                        decimalSeen = true
                        index++
                    }
                    else -> break
                }
            }
            val text = source.substring(start, index)
            require(text != ".") { "Invalid number" }
            return text.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid number")
        }

        private fun parseIdentifier(): Double {
            val start = index
            while (!atEnd() && (source[index].isLetter() || source[index] == 'π')) index++
            val name = source.substring(start, index).lowercase()
            return when (name) {
                "pi", "π" -> PI
                "e" -> Math.E
                else -> {
                    skipWhitespace()
                    require(take('(')) { "Function needs parentheses" }
                    val argument = parseExpression()
                    skipWhitespace()
                    require(take(')')) { "Missing function parenthesis" }
                    applyFunction(name, argument)
                }
            }
        }

        private fun applyFunction(name: String, value: Double): Double {
            val radians = if (angleMode == AngleMode.DEG) Math.toRadians(value) else value
            return when (name) {
                "sin" -> sin(radians)
                "cos" -> cos(radians)
                "tan" -> tan(radians)
                "asin" -> angleResult(asin(value))
                "acos" -> angleResult(acos(value))
                "atan" -> angleResult(atan(value))
                "sqrt" -> {
                    require(value >= 0.0) { "Square root needs a non-negative value" }
                    sqrt(value)
                }
                "abs" -> abs(value)
                "ln" -> {
                    require(value > 0.0) { "Logarithm needs a positive value" }
                    ln(value)
                }
                "log" -> {
                    require(value > 0.0) { "Logarithm needs a positive value" }
                    log10(value)
                }
                "exp" -> exp(value)
                "recip" -> {
                    require(value != 0.0) { "Cannot divide by zero" }
                    1.0 / value
                }
                "cbrt" -> Math.cbrt(value)
                else -> throw IllegalArgumentException("Unknown function")
            }
        }

        private fun angleResult(radians: Double): Double =
            if (angleMode == AngleMode.DEG) Math.toDegrees(radians) else radians

        private fun factorial(value: Double): Double {
            require(value >= 0.0 && value <= 170.0 && value % 1.0 == 0.0) {
                "Factorial needs a whole number from 0 to 170"
            }
            var result = 1.0
            for (n in 2..value.toInt()) result *= n.toDouble()
            return result
        }

        private fun take(char: Char): Boolean {
            if (!atEnd() && source[index] == char) {
                index++
                return true
            }
            return false
        }
    }
}
