package app.calculator.offline.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CalculatorColors = darkColorScheme(
    primary = Color(0xFFC7F36B),
    onPrimary = Color(0xFF17200C),
    secondary = Color(0xFFB5C99A),
    background = Color(0xFF101311),
    surface = Color(0xFF171C19),
    surfaceVariant = Color(0xFF222823),
    onBackground = Color(0xFFF0F4EA),
    onSurface = Color(0xFFF0F4EA),
    onSurfaceVariant = Color(0xFFA6B0A7),
    outline = Color(0xFF3A443D)
)

@Composable
fun CalculatorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CalculatorColors,
        content = content
    )
}