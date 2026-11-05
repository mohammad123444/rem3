package ir.yadavar.shamsi.alarm

import android.app.NotificationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.yadavar.shamsi.core.JDate
import ir.yadavar.shamsi.core.formatDayMonth
import ir.yadavar.shamsi.core.formatTime
import ir.yadavar.shamsi.data.AppDb
import ir.yadavar.shamsi.data.ReminderEntity
import ir.yadavar.shamsi.data.ReminderOps
import ir.yadavar.shamsi.data.SettingsStore
import ir.yadavar.shamsi.data.dateOrNull
import ir.yadavar.shamsi.ui.JalaliDateTimeFlow
import ir.yadavar.shamsi.ui.YadavarTheme
import kotlinx.coroutines.launch

class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val id = intent.getStringExtra(AlarmScheduler.EXTRA_ID)
        if (id == null) {
            finish()
            return
        }
        setContent {
            val settings by SettingsStore.flow.collectAsStateWithLifecycle()
            YadavarTheme(settings) { AlarmScreen(id) { finish() } }
        }
    }
}

private val SnoozeOptions = listOf(
    5 to "۵ دقیقه دیگر",
    15 to "۱۵ دقیقه دیگر",
    30 to "۳۰ دقیقه دیگر",
    60 to "۱ ساعت دیگر",
    120 to "۲ ساعت دیگر",
)

@Composable
private fun AlarmScreen(id: String, onFinish: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var reminder by remember { mutableStateOf<ReminderEntity?>(null) }
    var showSnooze by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }

    LaunchedEffect(id) { reminder = AppDb.get(context).reminders().byId(id) }

    fun clearNotification() {
        context.getSystemService(NotificationManager::class.java).cancel(id.hashCode())
    }

    fun run(block: suspend () -> Unit) {
        scope.launch {
            block()
            clearNotification()
            onFinish()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Default.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            val r = reminder
            Text(
                r?.title?.ifBlank { "یادآور" } ?: "یادآور",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
            if (r != null) {
                val date: JDate? = r.dateOrNull()
                val meta = buildString {
                    if (date != null) append(formatDayMonth(date))
                    if (r.hour != null && r.minute != null) {
                        if (isNotEmpty()) append(" · ")
                        append(formatTime(r.hour, r.minute))
                    }
                    if (r.location.isNotBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append(r.location)
                    }
                }
                if (meta.isNotEmpty()) Text(meta, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = { showSnooze = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                    Text("به تعویق", modifier = Modifier.padding(start = 8.dp))
                }
                Button(onClick = { run { ReminderOps.complete(context, id) } }, modifier = Modifier.weight(1f)) {
                    Text("انجام شد")
                }
            }
            TextButton(onClick = { run { ReminderOps.dismiss(context, id) } }) { Text("بستن") }
        }
    }

    if (showSnooze) {
        AlertDialog(
            onDismissRequest = { showSnooze = false },
            title = { Text("به تعویق انداختن") },
            text = {
                Column {
                    SnoozeOptions.forEach { (minutes, label) ->
                        TextButton(
                            onClick = { showSnooze = false; run { ReminderOps.snooze(context, id, minutes) } },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(label) }
                    }
                    TextButton(
                        onClick = { showSnooze = false; showPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("زمان دیگر…") }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showSnooze = false }) { Text("انصراف") } },
        )
    }
    if (showPicker) {
        JalaliDateTimeFlow(
            onDismiss = { showPicker = false },
            onPicked = { date, hour, minute ->
                showPicker = false
                run { ReminderOps.reschedule(context, id, date, hour, minute) }
            },
        )
    }
}
