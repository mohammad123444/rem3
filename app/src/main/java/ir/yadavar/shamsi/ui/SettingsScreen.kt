package ir.yadavar.shamsi.ui

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onClose: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    BackHandler(onBack = onClose)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تنظیمات") },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Section("پوسته") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system" to "سیستم", "light" to "روشن", "dark" to "تیره").forEach { (value, label) ->
                        FilterChip(
                            selected = settings.theme == value,
                            onClick = { vm.updateSettings(settings.copy(theme = value)) },
                            label = { Text(label) },
                        )
                    }
                }
            }
            Section("رنگ برنامه") {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val options = buildList {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add("dynamic" to "رنگ سیستم")
                        add("blue" to "آبی"); add("teal" to "سبزآبی"); add("amber" to "کهربایی"); add("rose" to "صورتی")
                    }
                    options.forEach { (value, label) ->
                        FilterChip(
                            selected = settings.accent == value,
                            onClick = { vm.updateSettings(settings.copy(accent = value)) },
                            label = { Text(label) },
                        )
                    }
                }
            }
            Section("اندازه فونت ویجت") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("sm" to "کوچک", "md" to "متوسط", "lg" to "بزرگ").forEach { (value, label) ->
                        FilterChip(
                            selected = settings.widgetFont == value,
                            onClick = { vm.updateSettings(settings.copy(widgetFont = value)) },
                            label = { Text(label) },
                        )
                    }
                }
                val sample = when (settings.widgetFont) { "sm" -> 12.sp; "lg" -> 18.sp; else -> 15.sp }
                Text("نمونه: خرید نان و میوه", fontSize = sample, modifier = Modifier.padding(top = 4.dp))
                Text(
                    "این تنظیم فقط روی متن ویجت اثر می‌گذارد و بلافاصله اعمال می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Section("اعلان‌ها") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("نمایش اعلان و زنگ یادآورها", modifier = Modifier.weight(1f))
                    Switch(checked = settings.notify, onCheckedChange = { vm.updateSettings(settings.copy(notify = it)) })
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = context.getSystemService(AlarmManager::class.java)
                    if (!manager.canScheduleExactAlarms()) {
                        Text(
                            "برای زنگ دقیق، اجازهٔ «آلارم‌ها و یادآورها» لازم است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        OutlinedButton(onClick = {
                            context.startActivity(
                                Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }) { Text("باز کردن تنظیمات آلارم") }
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}
