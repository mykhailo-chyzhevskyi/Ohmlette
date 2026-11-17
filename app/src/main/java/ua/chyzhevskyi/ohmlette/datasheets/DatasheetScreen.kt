package ua.chyzhevskyi.ohmlette.datasheets

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

private val requests = Executors.newSingleThreadExecutor()
private val main = Handler(Looper.getMainLooper())
private const val LIMIT = 15 * 1024 * 1024

private fun createCard(endpoint: String, part: String, link: String, file: Uri?, context: Context): JSONObject {
    val url = URL(endpoint.trimEnd('/') + "/v1/cards")
    require(url.protocol in listOf("http", "https") && url.host.isNotBlank()) { "Перевірте адресу backend" }
    val payload = JSONObject().put("partNumber", part.trim())
    if (file != null) {
        val bytes = context.contentResolver.openInputStream(file)?.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while(true) {
                val count = input.read(buffer)
                if(count < 0) break
                require(output.size() + count <= LIMIT) { "Файл має бути до 15 МіБ" }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } ?: error("Не вдалося відкрити файл")
        require(bytes.size <= LIMIT) { "Файл має бути до 15 МіБ" }
        payload.put("pdfBase64", Base64.encodeToString(bytes, Base64.NO_WRAP))
    } else payload.put("url", link.trim())
    val connection = url.openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "POST"
        connection.connectTimeout = 10000
        connection.readTimeout = 90000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val body = (if(code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() } ?: error("Порожня відповідь")
        val result = JSONObject(body)
        require(code in 200..299) { result.optString("error", "Помилка HTTP $code") }
        return result
    } finally { connection.disconnect() }
}

@Composable fun DatasheetScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("backend", Context.MODE_PRIVATE) }
    var endpoint by rememberSaveable { mutableStateOf(prefs.getString("url", "http://10.0.2.2:8000")!!) }
    var link by rememberSaveable { mutableStateOf("https://www.onsemi.com/pdf/datasheet/bzx84c2v4lt1-d.pdf") }
    var part by rememberSaveable { mutableStateOf("BZX84C2V4LT1G") }
    var file by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var card by remember { mutableStateOf<JSONObject?>(null) }
    var cardEndpoint by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }
    var alive by remember { mutableStateOf(true) }
    DisposableEffect(Unit) { alive = true; onDispose { alive = false } }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> file = uri; card = null }
    Text("Картка даташита", style = MaterialTheme.typography.headlineSmall)
    Text("Навчальний MVP: PDF обробляється на твоєму ПК. Без хмарного AI.")
    OutlinedTextField(endpoint, { endpoint = it }, label = { Text("Адреса backend") }, supportingText = { Text("Емулятор: 10.0.2.2:8000. Телефон: IP комп'ютера у Wi-Fi.") }, enabled = !busy, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(part, { part = it; card = null }, label = { Text("Точний артикул") }, enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(link, { link = it; file = null; card = null }, label = { Text("HTTPS-посилання на PDF") }, enabled = !busy, modifier = Modifier.fillMaxWidth())
    OutlinedButton(onClick = { picker.launch(arrayOf("application/pdf")) }, enabled = !busy) { Text(if(file == null) "Обрати PDF на телефоні" else "Обрано PDF · змінити") }
    if(file != null) TextButton(onClick = { file = null }, enabled = !busy) { Text("Використати посилання замість файла") }
    Button(onClick = {
        busy = true; error = null; card = null; saved = false
        val address = endpoint; val number = part; val source = link; val chosen = file
        prefs.edit().putString("url", address).apply()
        requests.execute {
            val outcome = runCatching { createCard(address, number, source, chosen, context.applicationContext) }
            main.post {
                if(alive) {
                    busy = false
                    outcome.onSuccess { card = it; cardEndpoint = address }.onFailure { error = it.message ?: "Не вдалося з'єднатися з backend" }
                }
            }
        }
    }, enabled = !busy && part.isNotBlank() && (file != null || link.isNotBlank())) { Text("Створити картку") }
    if(busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Завантаження PDF та аналіз…") }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    card?.let { result ->
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(result.getString("partNumber"), style = MaterialTheme.typography.titleLarge)
            Text("${result.getJSONObject("classification").getString("label")} · неперевірено")
            Text(result.getString("summary"))
            if(!result.getBoolean("partFound")) Text("Артикул у тексті не знайдено. Не використовуйте картку як підтвердження параметрів.")
            val evidence = result.getJSONArray("evidence")
            for(i in 0 until evidence.length()) {
                val item = evidence.getJSONObject(i)
                Text("Сторінка ${item.getInt("page")}", style = MaterialTheme.typography.titleMedium)
                Text(item.getString("quote"))
                TextButton(onClick = {
                    val uri = Uri.parse(cardEndpoint.trimEnd('/') + "/documents/" + result.getString("documentId") + ".pdf#page=" + item.getInt("page"))
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }.onFailure { error = "Немає програми для відкриття PDF" }
                }) { Text("Відкрити PDF") }
            }
            val limitations = result.getJSONArray("limitations")
            for(i in 0 until limitations.length()) Text(limitations.getString(i), style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = {
                runCatching {
                    val dir = java.io.File(context.filesDir, "cards").apply { mkdirs() }
                    java.io.File(dir, result.getString("documentId") + "-" + result.getString("partNumber").hashCode() + ".json").writeText(result.toString(2))
                }.onSuccess { saved = true }.onFailure { error = "Не вдалося зберегти картку" }
            }) { Text(if(saved) "Картку збережено на телефоні" else "Зберегти картку") }
        } }
    }
}
