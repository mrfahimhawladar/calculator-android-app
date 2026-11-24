package app.calculator.offline.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ExpressionEvaluatorTest {
    @Test fun multiplicationPrecedesAddition() {
        assertEquals(7.0, ExpressionEvaluator.evaluate("1+2×3"), 1e-10)
    }

    @Test fun parenthesesOverridePrecedence() {
        assertEquals(9.0, ExpressionEvaluator.evaluate("(1+2)×3"), 1e-10)
    }

    @Test fun exponentiationIsRightAssociative() {
        assertEquals(512.0, ExpressionEvaluator.evaluate("2^3^2"), 1e-10)
    }

    @Test fun unaryMinusHasLowerPrecedenceThanPower() {
        assertEquals(-4.0, ExpressionEvaluator.evaluate("-2^2"), 1e-10)
    }

    @Test fun percentageIsPostfix() {
        assertEquals(0.1, ExpressionEvaluator.evaluate("10%"), 1e-10)
    }

    @Test fun degreeModeTrigonometry() {
        assertEquals(0.5, ExpressionEvaluator.evaluate("sin(30)"), 1e-10)
    }

    @Test fun radiansModeTrigonometry() {
        assertEquals(1.0, ExpressionEvaluator.evaluate("sin(π/2)", ExpressionEvaluator.AngleMode.RAD), 1e-10)
    }

    @Test fun scientificFunctions() {
        assertEquals(9.0, ExpressionEvaluator.evaluate("sqrt(81)"), 1e-10)
        assertEquals(2.0, ExpressionEvaluator.evaluate("log(100)"), 1e-10)
    }

    @Test fun constantsAndFactorial() {
        assertEquals(Math.E, ExpressionEvaluator.evaluate("e"), 1e-10)
        assertEquals(120.0, ExpressionEvaluator.evaluate("5!"), 1e-10)
    }

    @Test fun divisionByZeroIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ExpressionEvaluator.evaluate("5÷0")
        }
    }
}