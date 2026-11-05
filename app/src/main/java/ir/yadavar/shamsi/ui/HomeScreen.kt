package ir.yadavar.shamsi.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.yadavar.shamsi.core.formatDayMonth
import ir.yadavar.shamsi.core.formatLongDate
import ir.yadavar.shamsi.core.formatTime
import ir.yadavar.shamsi.core.Jalali
import ir.yadavar.shamsi.data.GroupEntity
import ir.yadavar.shamsi.data.ReminderEntity
import ir.yadavar.shamsi.data.Section
import ir.yadavar.shamsi.data.dateOrNull
import ir.yadavar.shamsi.data.occurrenceEpoch
import ir.yadavar.shamsi.data.repeatLabel
import ir.yadavar.shamsi.data.section
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class ListFilter(val label: String) {
    ALL("همه"), TODAY("امروز"), SCHEDULED("زمان‌دار"), STARRED("ستاره‌دار"), DONE("انجام‌شده‌ها"),
}

private val SectionTitles = mapOf(
    Section.OVERDUE to "گذشته",
    Section.TODAY to "امروز",
    Section.TOMORROW to "فردا",
    Section.WEEK to "این هفته",
    Section.LATER to "بعداً",
    Section.UNDATED to "بدون تاریخ",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: MainViewModel,
    onNew: () -> Unit,
    onEdit: (String) -> Unit,
    onVoice: () -> Unit,
    onGroups: () -> Unit,
    onSettings: () -> Unit,
) {
    val reminders by vm.reminders.collectAsStateWithLifecycle()
    val groups by vm.groups.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf(ListFilter.ALL) }
    var groupFilter by remember { mutableStateOf<String?>(null) }
    val now = remember(reminders) { System.currentTimeMillis() }
    val groupMap = remember(groups) { groups.associateBy { it.id } }

    val visible = remember(reminders, filter, groupFilter, now) {
        val base = reminders.filter { r -> groupFilter == null || r.groupId == groupFilter }
        when (filter) {
            ListFilter.DONE -> base.filter { it.completed }.sortedByDescending { it.completedAt ?: 0L }
            ListFilter.ALL -> base.filter { !it.completed }
            ListFilter.TODAY -> base.filter {
                !it.completed && (it.section(now) == Section.TODAY || it.section(now) == Section.OVERDUE)
            }
            ListFilter.SCHEDULED -> base.filter { !it.completed && it.dateOrNull() != null }
            ListFilter.STARRED -> base.filter { !it.completed && it.starred }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("یادآور شمسی")
                        Text(
                            formatLongDate(Jalali.today(now)),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onGroups) { Icon(Icons.Default.Folder, contentDescription = "گروه‌ها") }
                    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, contentDescription = "تنظیمات") }
                },
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallFloatingActionButton(onClick = onVoice) {
                    Icon(Icons.Default.Mic, contentDescription = "یادآور صوتی")
                }
                FloatingActionButton(onClick = onNew) {
                    Icon(Icons.Default.Add, contentDescription = "یادآور جدید")
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ListFilter.entries.forEach { f ->
                    FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.label) })
                }
            }
            if (groups.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    groups.forEach { g ->
                        FilterChip(
                            selected = groupFilter == g.id,
                            onClick = { groupFilter = if (groupFilter == g.id) null else g.id },
                            label = { Text(g.name) },
                        )
                    }
                }
            }
            if (visible.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (filter == ListFilter.DONE) "هنوز کاری انجام نشده است" else "یادآوری وجود ندارد",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 160.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (filter == ListFilter.DONE) {
                        items(visible, key = { it.id }) { r ->
                            DoneCard(r, groupMap[r.groupId], onRestore = { vm.restore(r.id) }, onDelete = { vm.delete(r.id) })
                        }
                    } else {
                        val grouped = visible.groupBy { it.section(now) }
                        Section.entries.forEach { section ->
                            val list = grouped[section]?.sortedBy { it.occurrenceEpoch() ?: Long.MAX_VALUE }.orEmpty()
                            if (list.isNotEmpty()) {
                                item(key = "header-$section") {
                                    Text(
                                        SectionTitles.getValue(section),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (section == Section.OVERDUE) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                                    )
                                }
                                items(list, key = { it.id }) { r ->
                                    ReminderCard(
                                        reminder = r,
                                        group = groupMap[r.groupId],
                                        onOpen = { onEdit(r.id) },
                                        onComplete = { vm.complete(r.id) },
                                        onToggleItem = { itemId -> vm.toggleItem(r.id, itemId) },
                                        onToggleExpand = { vm.toggleExpanded(r.id) },
                                        onToggleStar = { vm.toggleStar(r.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun metaLine(r: ReminderEntity): String = buildString {
    val date = r.dateOrNull()
    if (date != null) append(formatDayMonth(date))
    if (r.hour != null && r.minute != null) {
        if (isNotEmpty()) append(" · ")
        append(formatTime(r.hour, r.minute))
    }
    if (r.location.isNotBlank()) {
        if (isNotEmpty()) append(" · ")
        append(r.location)
    }
    if (r.repeatKind != "none") {
        if (isNotEmpty()) append(" · ")
        append(repeatLabel(r.repeatKind, r.repeatEvery))
    }
}

@Composable
private fun ReminderCard(
    reminder: ReminderEntity,
    group: GroupEntity?,
    onOpen: () -> Unit,
    onComplete: () -> Unit,
    onToggleItem: (String) -> Unit,
    onToggleExpand: () -> Unit,
    onToggleStar: () -> Unit,
) {
    var visible by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    AnimatedVisibility(
        visible = visible,
        exit = fadeOut() + shrinkVertically(),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Row(Modifier.height(IntrinsicSize.Min)) {
                Box(
                    Modifier.width(5.dp).fillMaxHeight()
                        .background(groupColor(group?.color ?: "slate")),
                )
                Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp).weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = false,
                            onCheckedChange = {
                                scope.launch {
                                    visible = false
                                    delay(350)
                                    onComplete()
                                }
                            },
                        )
                        Column(Modifier.weight(1f)) {
                            Text(reminder.title.ifBlank { "بدون عنوان" }, style = MaterialTheme.typography.titleMedium)
                            val meta = metaLine(reminder)
                            if (meta.isNotEmpty()) {
                                Text(
                                    meta,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (reminder.items.isNotEmpty()) {
                            IconButton(onClick = onToggleExpand) {
                                Icon(
                                    if (reminder.expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (reminder.expanded) "بستن زیرمجموعه‌ها" else "باز کردن زیرمجموعه‌ها",
                                )
                            }
                        }
                        IconButton(onClick = onToggleStar) {
                            Icon(
                                if (reminder.starred) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "ستاره",
                                tint = if (reminder.starred) androidx.compose.ui.graphics.Color(0xFFF59E0B)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (reminder.expanded) {
                        reminder.items.forEach { item ->
                            Row(
                                modifier = Modifier.padding(start = 28.dp).alpha(if (item.done) 0.5f else 1f),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(checked = item.done, onCheckedChange = { onToggleItem(item.id) })
                                Text(
                                    item.title,
                                    textDecoration = if (item.done) TextDecoration.LineThrough else null,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DoneCard(r: ReminderEntity, group: GroupEntity?, onRestore: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(5.dp).fillMaxHeight().background(groupColor(group?.color ?: "slate")))
            Column(Modifier.weight(1f).padding(12.dp).alpha(0.6f)) {
                Text(
                    r.title.ifBlank { "بدون عنوان" },
                    textDecoration = TextDecoration.LineThrough,
                    style = MaterialTheme.typography.titleMedium,
                )
                val meta = metaLine(r)
                if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onRestore) { Icon(Icons.Default.Restore, contentDescription = "بازگرداندن") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "حذف") }
        }
    }
}
