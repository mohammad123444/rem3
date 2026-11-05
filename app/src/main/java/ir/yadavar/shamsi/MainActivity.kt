package ir.yadavar.shamsi

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.yadavar.shamsi.core.ParsedSpeech
import ir.yadavar.shamsi.core.Speech
import ir.yadavar.shamsi.data.SettingsStore
import ir.yadavar.shamsi.ui.EditorScreen
import ir.yadavar.shamsi.ui.GroupsScreen
import ir.yadavar.shamsi.ui.HomeScreen
import ir.yadavar.shamsi.ui.MainViewModel
import ir.yadavar.shamsi.ui.SettingsScreen
import ir.yadavar.shamsi.ui.YadavarTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by SettingsStore.flow.collectAsStateWithLifecycle()
            YadavarTheme(settings) { AppRoot() }
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val vm: MainViewModel = viewModel()
    var route by rememberSaveable { mutableStateOf("home") }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var prefill by remember { mutableStateOf<ParsedSpeech?>(null) }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (result.resultCode == Activity.RESULT_OK && !text.isNullOrBlank()) {
            prefill = Speech.parse(text)
            editId = null
            route = "editor"
        }
    }

    fun startVoice() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "بگو چه یادآوری بسازم")
        }
        try {
            speech.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "تشخیص گفتار روی این دستگاه در دسترس نیست", Toast.LENGTH_LONG).show()
        }
    }

    when (route) {
        "editor" -> {
            val reminders by vm.reminders.collectAsStateWithLifecycle()
            val groups by vm.groups.collectAsStateWithLifecycle()
            val initial = editId?.let { id -> reminders.firstOrNull { it.id == id } }
            if (editId != null && initial == null) {
                // Reminder list is still loading (or the reminder no longer exists).
                LaunchedEffect(editId) {
                    delay(1_500)
                    if (vm.reminders.value.none { it.id == editId }) {
                        editId = null
                        route = "home"
                    }
                }
                Box(Modifier.fillMaxSize())
            } else EditorScreen(
                initial = initial,
                prefill = if (initial == null) prefill else null,
                groups = groups,
                onSave = vm::save,
                onDelete = vm::delete,
                onClose = { prefill = null; editId = null; route = "home" },
            )
        }
        "groups" -> GroupsScreen(vm) { route = "home" }
        "settings" -> SettingsScreen(vm) { route = "home" }
        else -> HomeScreen(
            vm = vm,
            onNew = { editId = null; prefill = null; route = "editor" },
            onEdit = { editId = it; prefill = null; route = "editor" },
            onVoice = ::startVoice,
            onGroups = { route = "groups" },
            onSettings = { route = "settings" },
        )
    }
}
