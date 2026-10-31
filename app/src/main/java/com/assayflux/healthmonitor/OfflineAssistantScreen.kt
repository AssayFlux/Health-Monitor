package com.assayflux.healthmonitor

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import dev.ffmpegkit.llama.LlamaModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private data class AssistantMessage(val text: String, val fromUser: Boolean)

@Composable
fun OfflineAssistantScreen(db: HealthDb, refreshToken: Int) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val modelDir = remember { (context.getExternalFilesDir("offline-models") ?: File(context.filesDir, "offline-models")).apply { mkdirs() } }
    var modelFile by remember { mutableStateOf(listOf("assistant-import.gguf", "assistant.gguf").map { File(modelDir, it) }.firstOrNull { it.isFile && it.length() > 100_000_000L }) }
    var modelName by remember { mutableStateOf(modelFile?.name ?: "") }
    var loadedModel by remember { mutableStateOf<LlamaModel?>(null) }
    var loadingModel by remember { mutableStateOf(false) }
    var generating by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(if (modelFile == null) "Import a compatible GGUF model to begin." else "Model file found. Load it to begin.") }
    var input by remember { mutableStateOf("") }
    var useRecords by remember { mutableStateOf(false) }
    var speakReplies by remember { mutableStateOf(true) }
    var selectedMember by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<AssistantMessage>() }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var offlineVoiceReady by remember { mutableStateOf(false) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    val members = remember(refreshToken) { db.members() }

    LaunchedEffect(members) {
        if (selectedMember.isBlank() || members.none { it[0] == selectedMember }) {
            selectedMember = members.firstOrNull()?.get(0).orEmpty()
        }
    }

    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { result ->
            if (result == TextToSpeech.SUCCESS) {
                val available = tts?.voices.orEmpty()
                val voice = available.firstOrNull { !it.isNetworkConnectionRequired }
                if (voice != null) {
                    tts?.voice = voice
                    offlineVoiceReady = true
                } else {
                    offlineVoiceReady = false
                }
            }
        }
        tts = engine
        onDispose {
            speechRecognizer?.destroy()
            speechRecognizer = null
            engine.stop()
            engine.shutdown()
            tts = null
        }
    }

    DisposableEffect(loadedModel) {
        onDispose { if (loadedModel != null) Llama.releaseModel(loadedModel!!) }
    }

    val importModel = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !generating) {
            scope.launch {
                loadingModel = true
                status = "Copying model into private app storage…"
                try {
                    val target = File(modelDir, "assistant-import.gguf")
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri).use { stream ->
                            requireNotNull(stream) { "Could not open selected model file." }
                            FileOutputStream(target).use { output ->
                                stream.copyTo(output, 1024 * 1024)
                                output.fd.sync()
                            }
                        }
                        require(target.length() > 100_000_000L) { "The selected file is too small to be a language model." }
                    }
                    loadedModel = null
                    modelFile?.takeIf { it.name == "assistant-import.gguf" }?.delete()
                    modelFile = target
                    modelName = target.name
                    status = "Model copied: ${target.length() / (1024 * 1024)} MB. Load it when ready."
                } catch (failure: Exception) {
                    status = "Model import failed: ${failure.message ?: "unknown error"}"
                } finally {
                    loadingModel = false
                }
            }
        }
    }

    fun buildHealthContext(): String {
        if (!useRecords) return "No health-record access was granted for this conversation."
        val member = members.firstOrNull { it[0] == selectedMember }
            ?: return "No family member is selected."
        val memberId = member[0].toLongOrNull() ?: return "Selected member ID is invalid."
        val sections = mutableListOf<String>()
        sections += "SELECTED MEMBER: ${member[1]}, DOB ${member[4]}, sex ${member[2]}, blood group ${member[3]}."
        sections += "VITALS: " + db.rows(
            "SELECT COALESCE(measurement_date,date(recorded_at)),COALESCE(left_systolic,''),COALESCE(left_diastolic,''),COALESCE(right_systolic,''),COALESCE(right_diastolic,''),COALESCE(pulse,''),COALESCE(weight,''),COALESCE(spo2,''),COALESCE(temp,''),COALESCE(notes,'') FROM vitals WHERE member_id=$memberId ORDER BY id DESC LIMIT 15"
        ).joinToString("; ") { it.joinToString(", ") }
        sections += "LAB TESTS: " + db.rows(
            "SELECT COALESCE(recorded_at,''),COALESCE(category,''),parameter,COALESCE(value_numeric,value_text,''),COALESCE(unit,''),COALESCE(ref_low,''),COALESCE(ref_high,''),COALESCE(status,''),COALESCE(notes,'') FROM clinical_tests WHERE member_id=$memberId ORDER BY id DESC LIMIT 30"
        ).joinToString("; ") { it.joinToString(", ") }
        sections += "LAB REPORTS: " + db.rows(
            "SELECT COALESCE(recorded_at,''),COALESCE(name,'Report'),values_json FROM labs WHERE member_id=$memberId ORDER BY id DESC LIMIT 20"
        ).joinToString("; ") { it.joinToString(", ") }
        sections += "MEDICINES: " + db.rows(
            "SELECT name,COALESCE(dose,''),COALESCE(start_date,''),COALESCE(end_date,''),COALESCE(notes,'') FROM medicines WHERE member_id=$memberId ORDER BY id DESC LIMIT 30"
        ).joinToString("; ") { it.joinToString(", ") }
        sections += "MEDICAL HISTORY: " + db.rows(
            "SELECT category,display,COALESCE(status,''),COALESCE(onset_date,''),COALESCE(resolved_date,''),COALESCE(severity,''),COALESCE(reaction,''),COALESCE(notes,'') FROM clinical_history WHERE member_id=$memberId ORDER BY id DESC LIMIT 40"
        ).joinToString("; ") { it.joinToString(", ") }
        sections += "APPOINTMENTS: " + db.rows(
            "SELECT title,scheduled_at,COALESCE(location,''),COALESCE(notes,'') FROM appointments WHERE member_id=$memberId ORDER BY scheduled_at DESC LIMIT 20"
        ).joinToString("; ") { it.joinToString(", ") }
        return sections.joinToString("\n").take(18_000)
    }

    fun loadModel() {
        val file = modelFile ?: run { status = "Import a GGUF model first."; return }
        if (!Build.SUPPORTED_ABIS.contains("arm64-v8a")) { status = "The current embedded llama.cpp package supports ARM64 phones only."; return }
        scope.launch {
            loadingModel = true
            status = "Loading model into memory. This may take a while…"
            try {
                loadedModel?.let(Llama::releaseModel)
                loadedModel = null
                val model = withContext(Dispatchers.IO) {
                    Llama.loadModel(
                        modelPath = file.absolutePath,
                        config = LlamaConfig(contextSize = 4096, threads = Runtime.getRuntime().availableProcessors().coerceIn(2, 6))
                    )
                }
                loadedModel = model
                status = "Offline model loaded and ready."
            } catch (failure: Throwable) {
                loadedModel = null
                status = "Model could not be loaded: ${failure.message ?: "runtime error"}"
            } finally {
                loadingModel = false
            }
        }
    }

    fun sendMessage(raw: String) {
        val question = raw.trim()
        val model = loadedModel
        if (question.isBlank() || generating) return
        if (model == null || !model.isLoaded) {
            status = "Load a compatible local GGUF model before chatting."
            return
        }
        messages += AssistantMessage(question, true)
        input = ""
        generating = true
        scope.launch {
            try {
                val history = messages.takeLast(8).joinToString("\n") {
                    (if (it.fromUser) "User: " else "Assistant: ") + it.text
                }
                val health = withContext(Dispatchers.IO) { buildHealthContext() }
                val system = """
                    You are Health Monitor's offline health-record assistant.
                    Explain information clearly and calmly. You are not a clinician and must not diagnose,
                    prescribe, change doses, or claim that a result is safe. Never invent values or facts.
                    When health records are supplied, use only those records, preserve dates and units,
                    distinguish missing data from normal data, and say when evidence is insufficient.
                    Explain that reference ranges vary by laboratory and patient. For severe or sudden
                    symptoms, advise urgent professional/emergency care; do not delay care for chat.
                    Never perform database writes. Ask before assuming which family member is meant.
                    Keep answers concise, structured, and understandable to a non-specialist.
                    HEALTH RECORD CONTEXT (read-only, user opted in):
                    $health
                """.trimIndent()
                val prompt = "$history\nAssistant:"
                val result = withContext(Dispatchers.IO) {
                    Llama.complete(model = model, prompt = prompt, systemPrompt = system, maxTokens = 512)
                }
                val answer = result.text.trim().ifBlank { "I couldn't generate a response. Please try again." }
                messages += AssistantMessage(answer, false)
                if (speakReplies) {
                    if (offlineVoiceReady) tts?.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "health-assistant-reply")
                    else status = "Reply generated. No offline TTS voice is installed on this device."
                }
            } catch (failure: Throwable) {
                messages += AssistantMessage("I couldn't complete that request: ${failure.message ?: "local inference error"}", false)
            } finally {
                generating = false
            }
        }
    }

    fun startListening() {
        if (Build.VERSION.SDK_INT < 31 || !SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            status = "Offline speech recognition is unavailable on this device. Install an on-device speech pack or type your message."
            return
        }
        val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        speechRecognizer?.destroy()
        speechRecognizer = recognizer
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) { status = "Listening… speak now." }
            override fun onBeginningOfSpeech() { status = "Listening…" }
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() { status = "Recognising speech offline…" }
            override fun onError(error: Int) { status = "Offline voice recognition failed (code $error). You can type instead." }
            override fun onResults(results: android.os.Bundle?) {
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (spoken.isNotBlank()) input = spoken
                status = if (spoken.isBlank()) "No speech was recognised." else "Voice captured. Review the text, then send."
                recognizer.destroy()
                speechRecognizer = null
            }
            override fun onPartialResults(partialResults: android.os.Bundle?) = Unit
            override fun onEvent(eventType: Int, params: android.os.Bundle?) = Unit
        })
        recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        })
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startListening() else status = "Microphone permission was denied."
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = Forest)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("PRIVATE · ON-DEVICE AI", color = Sage, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Health Assistant", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
                Text("Conversations and selected records stay on this phone.", color = Color.White.copy(alpha = .82f), fontSize = 12.sp)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Cream)) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("LOCAL MODEL", color = Forest, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(if (modelFile == null) "No model imported" else modelName, color = Ink, fontWeight = FontWeight.SemiBold)
                Text("Recommended: Qwen 3.5 4B Q4 GGUF on phones with 8 GB+ RAM. For lower-memory devices, use a 0.8B–2B quantized GGUF. Model files are not bundled in the APK.", color = Ink.copy(alpha = .72f), fontSize = 11.sp)
                TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://huggingface.co/Boollm/Qwen3.5-4B-GGUF"))) }) { Text("Get Qwen 3.5 GGUF model") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { importModel.launch(arrayOf("application/octet-stream", "application/x-gguf", "*/*")) }, enabled = !loadingModel && !generating) {
                        Icon(Icons.Default.FolderOpen, null); Spacer(Modifier.width(5.dp)); Text("Import GGUF")
                    }
                    Button(onClick = { loadModel() }, enabled = modelFile != null && !loadingModel && !generating, colors = ButtonDefaults.buttonColors(containerColor = Forest)) {
                        if (loadingModel) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Icon(Icons.Default.Memory, null)
                        Spacer(Modifier.width(5.dp)); Text("Load")
                    }
                }
                Text(status, color = Ink.copy(alpha = .7f), fontSize = 11.sp)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Cream)) {
            Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("HEALTH DATA ACCESS", color = Forest, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = useRecords, onCheckedChange = { useRecords = it })
                    Text("Allow read-only access to selected member records", color = Ink, fontSize = 11.sp)
                }
                if (useRecords) {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), enabled = members.isNotEmpty()) {
                            Text(members.firstOrNull { it[0] == selectedMember }?.get(1) ?: "Select a family member")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            members.forEach { row ->
                                DropdownMenuItem(text = { Text(row[1]) }, onClick = { selectedMember = row[0]; expanded = false })
                            }
                        }
                    }
                }
                Text("Record access is off by default. No database changes are permitted.", color = Ink.copy(alpha = .65f), fontSize = 10.sp)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Conversation", color = Forest, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Speak replies", color = Ink, fontSize = 11.sp)
            Switch(checked = speakReplies, onCheckedChange = { speakReplies = it })
            TextButton(onClick = { messages.clear() }, enabled = messages.isNotEmpty()) { Text("Clear") }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            if (messages.isEmpty()) item {
                Text("Ask about a health term, a recorded result, medicines, or trends. The assistant only sees personal records when you explicitly enable access.", color = Ink.copy(alpha = .7f), fontSize = 12.sp, modifier = Modifier.padding(8.dp))
            }
            items(messages) { message ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start) {
                    Card(colors = CardDefaults.cardColors(containerColor = if (message.fromUser) Forest else Cream)) {
                        Text(message.text, color = if (message.fromUser) Color.White else Ink, modifier = Modifier.padding(12.dp), fontSize = 12.sp)
                    }
                }
            }
            if (generating) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Forest) }
        }
        OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ask your health assistant…") }, maxLines = 3, enabled = !generating)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startListening()
                else micPermission.launch(Manifest.permission.RECORD_AUDIO)
            }, enabled = !generating) { Icon(Icons.Default.Mic, null); Text("Talk") }
            Button(onClick = { sendMessage(input) }, enabled = input.isNotBlank() && !generating && loadedModel?.isLoaded == true,
                modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Forest)) {
                Icon(Icons.Default.Send, null); Spacer(Modifier.width(6.dp)); Text("Send")
            }
        }
        Text("Not a diagnostic tool. Verify results against original reports and a qualified clinician. Seek urgent care for emergencies.", color = Ink.copy(alpha = .65f), fontSize = 10.sp, modifier = Modifier.padding(bottom = 5.dp))
    }
}
