package ir.yadavar.shamsi.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ir.yadavar.shamsi.core.JDate
import ir.yadavar.shamsi.core.ParsedSpeech
import ir.yadavar.shamsi.core.formatLongDate
import ir.yadavar.shamsi.core.formatTime
import ir.yadavar.shamsi.data.CheckItem
import ir.yadavar.shamsi.data.GroupEntity
import ir.yadavar.shamsi.data.Ids
import ir.yadavar.shamsi.data.ReminderEntity
import ir.yadavar.shamsi.data.dateOrNull

private val RepeatKinds = listOf("none" to "بدون تکرار", "daily" to "روزانه", "weekly" to "هفتگی", "monthly" to "ماهانه شمسی", "yearly" to "سالانه شمسی")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    initial: ReminderEntity?,
    prefill: ParsedSpeech?,
    groups: List<GroupEntity>,
    onSave: (ReminderEntity) -> Unit,
    onDelete: (String) -> Unit,
    onClose: () -> Unit,
) {
    var title by remember { mutableStateOf(initial?.title ?: prefill?.title.orEmpty()) }
    val items = remember { mutableStateListOf<CheckItem>().apply { addAll(initial?.items.orEmpty()) } }
    var date by remember {
        mutableStateOf(
            initial?.dateOrNull()
                ?: if (prefill?.jy != null && prefill.jm != null && prefill.jd != null) JDate(prefill.jy, prefill.jm, prefill.jd) else null,
        )
    }
    var hour by remember { mutableStateOf(initial?.hour ?: prefill?.hour) }
    var minute by remember { mutableStateOf(initial?.minute ?: prefill?.minute) }
    var location by remember { mutableStateOf(initial?.location.orEmpty()) }
    var groupId by remember { mutableStateOf(initial?.groupId ?: groups.firstOrNull()?.id.orEmpty()) }
    var starred by remember { mutableStateOf(initial?.starred ?: false) }
    var alert by remember { mutableStateOf(initial?.alert ?: true) }
    var repeatKind by remember { mutableStateOf(initial?.repeatKind ?: "none") }
    var repeatEvery by remember { mutableIntStateOf(initial?.repeatEvery ?: 1) }
    var newItem by remember { mutableStateOf("") }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    BackHandler(onBack = onClose)

    fun commit() {
        val now = System.currentTimeMillis()
        val pending = newItem.trim()
        val finalItems = if (pending.isNotEmpty()) items + CheckItem(Ids.new(), pending, false) else items.toList()
        if (title.isBlank() && finalItems.isEmpty()) return
        val d = date
        val entity = ReminderEntity(
            id = initial?.id ?: Ids.new(),
            title = title.trim(),
            items = finalItems,
            jy = d?.jy, jm = d?.jm, jd = d?.jd,
            hour = if (d != null) hour ?: 9 else hour,
            minute = if (d != null) minute ?: 0 else minute,
            location = location.trim(),
            groupId = groupId,
            starred = starred,
            alert = alert,
            repeatKind = repeatKind,
            repeatEvery = repeatEvery.coerceAtLeast(1),
            completed = initial?.completed ?: false,
            completedAt = initial?.completedAt,
            expanded = initial?.expanded ?: finalItems.isNotEmpty(),
            snoozeUntil = null,
            dismissedKey = null,
            createdAt = initial?.createdAt ?: now,
            updatedAt = now,
        )
        onSave(entity)
        onClose()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initial == null) "یادآور جدید" else "ویرایش یادآور") },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت") }
                },
                actions = {
                    IconButton(onClick = { starred = !starred }) {
                        Icon(if (starred) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = "ستاره")
                    }
                    if (initial != null) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف")
                        }
                    }
                    TextButton(onClick = { commit() }) { Text("ذخیره") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("عنوان") }, modifier = Modifier.fillMaxWidth(),
            )

            Text("چک‌لیست", style = MaterialTheme.typography.titleSmall)
            items.forEachIndexed { index, item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = item.done, onCheckedChange = { items[index] = item.copy(done = it) })
                    OutlinedTextField(
                        value = item.title,
                        onValueChange = { items[index] = item.copy(title = it) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    IconButton(onClick = { items.removeAt(index) }) {
                        Icon(Icons.Default.Close, contentDescription = "حذف مورد")
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newItem, onValueChange = { newItem = it },
                    placeholder = { Text("افزودن زیرمجموعه") },
                    modifier = Modifier.weight(1f), singleLine = true,
                )
                IconButton(onClick = {
                    val text = newItem.trim()
                    if (text.isNotEmpty()) {
                        items.add(CheckItem(Ids.new(), text, false))
                        newItem = ""
                    }
                }) { Icon(Icons.Default.Add, contentDescription = "افزودن") }
            }

            Text("زمان", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showDate = true }, modifier = Modifier.weight(1f)) {
                    Text(date?.let { formatLongDate(it) } ?: "انتخاب تاریخ شمسی")
                }
                OutlinedButton(onClick = { showTime = true }, modifier = Modifier.weight(1f)) {
                    val h = hour
                    val m = minute
                    Text(if (h != null && m != null) formatTime(h, m) else "انتخاب ساعت")
                }
            }
            if (date != null || hour != null) {
                TextButton(onClick = { date = null; hour = null; minute = null }) { Text("حذف تاریخ و ساعت") }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("هشدار (زنگ و اعلان)", modifier = Modifier.weight(1f))
                Switch(checked = alert, onCheckedChange = { alert = it })
            }

            OutlinedTextField(
                value = location, onValueChange = { location = it },
                label = { Text("مکان") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            )

            Text("تکرار", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RepeatKinds.forEach { (kind, label) ->
                    FilterChip(selected = repeatKind == kind, onClick = { repeatKind = kind }, label = { Text(label) })
                }
            }
            if (repeatKind != "none") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("هر")
                    OutlinedButton(onClick = { if (repeatEvery > 1) repeatEvery -= 1 }) { Text("−") }
                    Text(ir.yadavar.shamsi.core.faNum(repeatEvery))
                    OutlinedButton(onClick = { if (repeatEvery < 99) repeatEvery += 1 }) { Text("+") }
                }
            }

            Text("گروه رنگی", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                groups.forEach { g ->
                    FilterChip(
                        selected = groupId == g.id,
                        onClick = { groupId = g.id },
                        label = { Text(g.name) },
                        leadingIcon = {
                            Box(Modifier.size(12.dp).clip(CircleShape).background(groupColor(g.color)))
                        },
                    )
                }
            }
        }
    }

    if (showDate) {
        JalaliDatePickerDialog(
            initial = date ?: ir.yadavar.shamsi.core.Jalali.today(),
            onDismiss = { showDate = false },
            onPick = { date = it; showDate = false },
        )
    }
    if (showTime) {
        TimePickerDialog(
            initialHour = hour ?: 9,
            initialMinute = minute ?: 0,
            onDismiss = { showTime = false },
            onPick = { h, m -> hour = h; minute = m; showTime = false },
        )
    }
    if (confirmDelete && initial != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف یادآور") },
            text = { Text("این یادآور برای همیشه حذف شود؟") },
            confirmButton = {
                TextButton(onClick = { onDelete(initial.id); confirmDelete = false; onClose() }) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("انصراف") } },
        )
    }
}
