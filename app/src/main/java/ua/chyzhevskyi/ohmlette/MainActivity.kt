package ua.chyzhevskyi.ohmlette

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ua.chyzhevskyi.ohmlette.datasheets.DatasheetScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF006B58), background = Color(0xFFF7F9F5))) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { Ohmlette() }
            }
        }
    }
}

@Composable private fun Ohmlette() {
    var screen by rememberSaveable { mutableStateOf("home") }
    BackHandler(screen != "home") { screen = "home" }
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        if (screen != "home") TextButton(onClick = { screen = "home" }) { Text("← Інструменти") }
        Text("Ohmlette", style = MaterialTheme.typography.headlineLarge)
        when (screen) {
            "home" -> {
                Text("Розрахунки під рукою", style = MaterialTheme.typography.titleMedium)
                Tool("01 / Одиниці", "Перерахунок інженерних префіксів", { screen = "units" })
                Tool("02 / Номінали", "Найближчі резистори E12 · E24 · E96", { screen = "resistors" })
                Tool("03 / Даташити", "PDF → AI-класифікація → чернетка картки", { screen = "datasheets" })
                Text("Далі: резисторні мережі, допуски та RC-фільтри.", style = MaterialTheme.typography.bodyMedium)
            }
            "units" -> Units()
            "resistors" -> Resistors()
            "datasheets" -> DatasheetScreen()
        }
    }
}
@Composable private fun Tool(title: String, subtitle: String, action: () -> Unit) {
    ElevatedCard(onClick = action, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(subtitle)
        }
    }
}
@Composable private fun Choice(label: String, options: List<String>, selected: String, change: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { expanded = true }) { Text("$selected ▾") }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                options.forEach { value -> DropdownMenuItem(text = { Text(value) }, onClick = { change(value); expanded = false }) }
            }
        }
    }
}
@Composable private fun Units() {
    var input by rememberSaveable { mutableStateOf("1") }
    var from by rememberSaveable { mutableStateOf("М") }
    var to by rememberSaveable { mutableStateOf("п") }
    var unit by rememberSaveable { mutableStateOf("Ом") }
    Text("Інженерні одиниці", style = MaterialTheme.typography.headlineSmall)
    Choice("Величина", listOf("Ом", "Ф", "Гн", "В", "А", "Гц", "с"), unit) { unit = it }
    OutlinedTextField(input, { input = it }, label = { Text("Значення") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Text("Тут вводьте число без префікса. Масштаб оберіть нижче.")
    Choice("З префікса", Engineering.prefixes.keys.toList(), from) { from = it }
    Choice("У префікс", Engineering.prefixes.keys.toList(), to) { to = it }
    val result = runCatching {
        require(!input.any { it.isLetter() && it != 'e' && it != 'E' || it == 'µ' || it == 'μ' }) { "Оберіть префікс окремо" }
        Engineering.convert(input, Engineering.prefixes.getValue(from), Engineering.prefixes.getValue(to))
    }
    Result(result.map { Engineering.format(it) + " " + (if(to == "без префікса") "" else to) + unit })
    Text("Результат = значення × 10^(степінь початкового префікса − степінь кінцевого).")
}
@Composable private fun Resistors() {
    var input by rememberSaveable { mutableStateOf("4.7k") }
    var series by rememberSaveable { mutableStateOf("E24") }
    Text("Підбір номіналів", style = MaterialTheme.typography.headlineSmall)
    OutlinedTextField(input, { input = it }, label = { Text("Потрібний опір, Ом") }, supportingText = { Text("Наприклад: 4700, 4.7k, 2.2M. m і M відрізняються.") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Choice("Стандартний ряд", listOf("E12", "E24", "E96"), series) { series = it }
    val result = runCatching { Engineering.nearest(Engineering.parse(input), series.drop(1).toInt()) }
    result.onSuccess { values -> values.forEachIndexed { index, nominal ->
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) {
            Text(if(index == 0) "Найближчий номінал" else "Альтернатива", style = MaterialTheme.typography.labelLarge)
            Text("${Engineering.format(nominal.resistance)} Ом", style = MaterialTheme.typography.titleLarge)
            Text("Відхилення: ${Engineering.format(nominal.errorPercent)} %")
        } }
    } }.onFailure { Text(it.message ?: "Перевірте значення", color = MaterialTheme.colorScheme.error) }
    Text("Відхилення = (номінал − потрібний опір) / потрібний опір × 100%. Це відхилення номіналу, а не допуск резистора.")
}
@Composable private fun Result(result: kotlin.Result<String>) {
    Card(Modifier.fillMaxWidth()) { Text(result.getOrElse { it.message ?: "Перевірте введення" }, Modifier.padding(20.dp), style = MaterialTheme.typography.titleLarge, color = if(result.isFailure) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }
}
