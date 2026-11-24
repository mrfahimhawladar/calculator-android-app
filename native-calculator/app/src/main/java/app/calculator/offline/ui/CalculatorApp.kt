package app.calculator.offline.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.calculator.offline.core.ExpressionEvaluator

private val Background = Color(0xFF101311)
private val KeyBackground = Color(0xFF1A201C)
private val UtilityBackground = Color(0xFF242C26)
private val Accent = Color(0xFFC7F36B)
private val Muted = Color(0xFFAAB4AA)

private data class KeySpec(
    val label: String,
    val action: String,
    val kind: KeyKind = KeyKind.Number
)

private enum class KeyKind { Number, Utility, Operator, Accent, Scientific }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorApp() {
    var expression by rememberSaveable { mutableStateOf("0") }
    var angleLabel by rememberSaveable { mutableStateOf("DEG") }
    var scientific by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var answer by remember { mutableStateOf("0") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var resultCommitted by rememberSaveable { mutableStateOf(false) }
    val history = remember { mutableStateListOf<String>() }
    val angleMode = if (angleLabel == "DEG") ExpressionEvaluator.AngleMode.DEG else ExpressionEvaluator.AngleMode.RAD

    fun refreshPreview(value: String) {
        expression = value
        errorText = null
        resultCommitted = false
        answer = runCatching { ExpressionEvaluator.format(ExpressionEvaluator.evaluate(value, angleMode)) }
            .getOrElse { "" }
    }

    fun appendToken(token: String) {
        var base = expression
        val startsValue = token.firstOrNull()?.isDigit() == true || token.startsWith("π") || token == "e" || token == "(" || token.firstOrNull()?.isLetter() == true
        if (resultCommitted && startsValue) base = ""
        if (base == "0" && (token.firstOrNull()?.isDigit() == true || token.startsWith("π") || token == "e")) base = ""
        resultCommitted = false
        if (base.isNotEmpty() && (base.last().isDigit() || base.last() == ')' || base.last() == 'π' || base.last() == 'e') &&
            (token.firstOrNull()?.isDigit() == true || token.startsWith("π") || token.startsWith("e") || token == "(" || token.firstOrNull()?.isLetter() == true)
        ) {
            base += "×"
        }
        refreshPreview(base + token)
    }

    fun appendOperator(operator: String) {
        var base = expression
        resultCommitted = false
        if (base == "0" && operator != "−") return
        if (base.isNotEmpty() && base.last() in charArrayOf('+', '−', '×', '÷', '^')) {
            base = base.dropLast(1)
        }
        refreshPreview(base + operator)
    }

    fun press(action: String) {
        when (action) {
            "clear" -> {
                expression = "0"
                answer = "0"
                errorText = null
                resultCommitted = false
            }
            "delete" -> {
                val next = expression.dropLast(1).ifBlank { "0" }
                refreshPreview(next)
            }
            "equals" -> {
                runCatching { ExpressionEvaluator.evaluate(expression, angleMode) }
                    .onSuccess { value ->
                        val formatted = ExpressionEvaluator.format(value)
                        history.add(0, "$expression = $formatted")
                        refreshPreview(formatted)
                        answer = formatted
                        resultCommitted = true
                    }
                    .onFailure { error ->
                        errorText = error.message ?: "Invalid expression"
                    }
            }
            "sign" -> {
                if (expression != "0") {
                    val next = if (expression.startsWith("-(") && expression.endsWith(")")) {
                        expression.substring(2, expression.length - 1)
                    } else {
                        "-($expression)"
                    }
                    refreshPreview(next)
                }
            }
            "open" -> {
                val last = expression.lastOrNull()
                val prefix = if (last != null && (last.isDigit() || last == ')' || last == 'π' || last == 'e')) "×" else ""
                appendToken(prefix + "(")
            }
            "close" -> {
                val opened = expression.count { it == '(' }
                val closed = expression.count { it == ')' }
                val last = expression.lastOrNull()
                if (opened > closed && last != null && (last.isDigit() || last == ')' || last == '%' || last == '!')) {
                    refreshPreview(expression + ")")
                }
            }
            "decimal" -> {
                val tail = expression.takeLastWhile { it.isDigit() || it == '.' }
                if (!tail.contains('.')) appendToken(if (tail.isEmpty()) "0." else ".")
            }
            "percent" -> appendToken("%")
            "pi" -> appendToken("π")
            "e" -> appendToken("e")
            "square" -> appendToken("^2")
            "power" -> appendOperator("^")
            "factorial" -> appendToken("!")
            "sin", "cos", "tan", "sqrt", "ln", "log", "asin", "acos", "atan" -> {
                val prefix = if (!resultCommitted && expression != "0" && (expression.last().isDigit() || expression.last() == ')' || expression.last() == 'π' || expression.last() == 'e')) "×" else ""
                val base = if (expression == "0" || resultCommitted) "" else expression
                resultCommitted = false
                refreshPreview(base + prefix + action + "(")
            }
            "+", "−", "×", "÷" -> appendOperator(action)
            else -> if (action.length == 1 && action[0].isDigit()) appendToken(action)
        }
    }

    if (expression.isNotBlank() && answer.isBlank()) {
        // Keep the display stable while a partially typed expression is being entered.
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(42.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "◷",
                    color = Muted,
                    fontSize = 25.sp,
                    modifier = Modifier.clickable { showHistory = true }
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    maxLines = 1
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ModeChip(label = angleLabel, active = true) {
                        angleLabel = if (angleLabel == "DEG") "RAD" else "DEG"
                        val nextMode = if (angleLabel == "DEG") ExpressionEvaluator.AngleMode.DEG else ExpressionEvaluator.AngleMode.RAD
                        answer = runCatching { ExpressionEvaluator.format(ExpressionEvaluator.evaluate(expression, nextMode)) }.getOrElse { "" }
                    }
                    ModeChip(label = if (scientific) "SCI ✓" else "SCI", active = scientific) {
                        scientific = !scientific
                    }
                }
            }

            Column(
                modifier = Modifier.weight(0.37f).fillMaxWidth().padding(bottom = 12.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = expression,
                    color = Muted,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                        .padding(bottom = 12.dp)
                )
                AnimatedContent(targetState = answer.ifBlank { "—" }, label = "live-result") { shown ->
                    Text(
                        text = shown,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-1.5).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth().animateContentSize()
                    )
                }
                if (errorText != null) {
                    Text(
                        text = errorText.orEmpty(),
                        color = Color(0xFFFF8B83),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(0.63f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                if (scientific) {
                    KeyRow(
                        keys = listOf(
                            KeySpec("sin", "sin", KeyKind.Scientific),
                            KeySpec("cos", "cos", KeyKind.Scientific),
                            KeySpec("tan", "tan", KeyKind.Scientific),
                            KeySpec("√", "sqrt", KeyKind.Scientific)
                        ), onPress = { press(it) }
                    )
                    KeyRow(
                        keys = listOf(
                            KeySpec("ln", "ln", KeyKind.Scientific),
                            KeySpec("log", "log", KeyKind.Scientific),
                            KeySpec("π", "pi", KeyKind.Scientific),
                            KeySpec("x²", "square", KeyKind.Scientific)
                        ), onPress = { press(it) }
                    )
                    KeyRow(
                        keys = listOf(
                            KeySpec("xʸ", "power", KeyKind.Scientific),
                            KeySpec("!", "factorial", KeyKind.Scientific),
                            KeySpec("sin⁻¹", "asin", KeyKind.Scientific),
                            KeySpec("e", "e", KeyKind.Scientific)
                        ), onPress = { press(it) }
                    )
                }

                val rows = listOf(
                    listOf(
                        KeySpec("AC", "clear", KeyKind.Utility),
                        KeySpec("(", "open", KeyKind.Utility),
                        KeySpec(")", "close", KeyKind.Utility),
                        KeySpec("⌫", "delete", KeyKind.Utility)
                    ),
                    listOf(
                        KeySpec("7", "7"), KeySpec("8", "8"), KeySpec("9", "9"), KeySpec("÷", "÷", KeyKind.Operator)
                    ),
                    listOf(
                        KeySpec("4", "4"), KeySpec("5", "5"), KeySpec("6", "6"), KeySpec("×", "×", KeyKind.Operator)
                    ),
                    listOf(
                        KeySpec("1", "1"), KeySpec("2", "2"), KeySpec("3", "3"), KeySpec("−", "−", KeyKind.Operator)
                    ),
                    listOf(
                        KeySpec("±", "sign", KeyKind.Utility), KeySpec("%", "percent", KeyKind.Utility), KeySpec("0", "0"), KeySpec(".", "decimal", KeyKind.Utility)
                    ),
                    listOf(
                        KeySpec("+", "+", KeyKind.Operator), KeySpec("=", "equals", KeyKind.Accent)
                    )
                )
                rows.forEach { row -> KeyRow(keys = row, onPress = { press(it) }) }
            }
        }
    }

    if (showHistory) {
        ModalBottomSheet(
            onDismissRequest = { showHistory = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF171C19),
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp)) {
                Text("History", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                if (history.isEmpty()) {
                    Text("No calculations yet", color = Muted, modifier = Modifier.padding(vertical = 22.dp))
                } else {
                    history.take(30).forEach { item ->
                        TextButton(
                            onClick = {
                                val selected = item.substringBefore(" = ")
                                refreshPreview(selected)
                                showHistory = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(item, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    TextButton(onClick = { history.clear() }, modifier = Modifier.align(Alignment.End)) {
                        Text("Clear history", color = Accent)
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun ModeChip(label: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (active) Color(0xFF2A3524) else Color(0xFF1A201C),
        contentColor = if (active) Accent else Muted
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
    }
}

@Composable
private fun ColumnScope.KeyRow(keys: List<KeySpec>, onPress: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().weight(1f),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        keys.forEach { key ->
            val weight = if (key.kind == KeyKind.Accent && keys.size == 2) 3f else 1f
            KeyButton(
                spec = key,
                modifier = Modifier.weight(weight).fillMaxHeight(),
                onClick = { onPress(key.action) }
            )
        }
    }
}

@Composable
private fun KeyButton(spec: KeySpec, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(stiffness = 700f),
        label = "key-press"
    )
    val container = when (spec.kind) {
        KeyKind.Accent -> Accent
        KeyKind.Operator -> Color(0xFF28342A)
        KeyKind.Utility -> UtilityBackground
        KeyKind.Scientific -> Color(0xFF202A22)
        KeyKind.Number -> KeyBackground
    }
    val content = when (spec.kind) {
        KeyKind.Accent -> Color(0xFF16200B)
        KeyKind.Operator -> Accent
        KeyKind.Utility, KeyKind.Scientific -> Color(0xFFDAE3D8)
        KeyKind.Number -> Color(0xFFF0F4EA)
    }
    Surface(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = modifier.scale(scale),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(if (spec.kind == KeyKind.Accent) 24.dp else 20.dp),
        color = container,
        contentColor = content
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(2.dp)) {
            Text(
                text = spec.label,
                fontSize = when (spec.kind) {
                    KeyKind.Number -> 25.sp
                    KeyKind.Scientific -> 15.sp
                    else -> 20.sp
                },
                fontWeight = if (spec.kind == KeyKind.Number) FontWeight.Light else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF101311, widthDp = 390, heightDp = 844)
@Composable
private fun CalculatorPreview() {
    CalculatorTheme { CalculatorApp() }
}
