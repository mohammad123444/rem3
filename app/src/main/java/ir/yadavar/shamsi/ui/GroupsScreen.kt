package ir.yadavar.shamsi.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.yadavar.shamsi.data.GroupEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(vm: MainViewModel, onClose: () -> Unit) {
    val groups by vm.groups.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<GroupEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    BackHandler(onBack = onClose)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("گروه‌های رنگی") },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) { Icon(Icons.Default.Add, contentDescription = "گروه جدید") }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(groups, key = { it.id }) { g ->
                Card(Modifier.fillMaxWidth().clickable { editing = g }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(16.dp).clip(CircleShape).background(groupColor(g.color)))
                        Text(g.name, modifier = Modifier.weight(1f).padding(start = 12.dp))
                        IconButton(onClick = { vm.deleteGroup(g.id) }) { Icon(Icons.Default.Delete, contentDescription = "حذف گروه") }
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        val target = editing
        var name by remember(target) { mutableStateOf(target?.name.orEmpty()) }
        var color by remember(target) { mutableStateOf(target?.color ?: "blue") }
        AlertDialog(
            onDismissRequest = { creating = false; editing = null },
            title = { Text(if (target == null) "گروه جدید" else "ویرایش گروه") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("نام گروه") }, singleLine = true)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GroupColorNames.forEach { c ->
                            FilterChip(
                                selected = color == c,
                                onClick = { color = c },
                                label = { Text(GroupColorLabels.getValue(c)) },
                                leadingIcon = { Box(Modifier.size(12.dp).clip(CircleShape).background(groupColor(c))) },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) vm.saveGroup(target?.id, name, color)
                    creating = false; editing = null
                }) { Text("ذخیره") }
            },
            dismissButton = { TextButton(onClick = { creating = false; editing = null }) { Text("انصراف") } },
        )
    }
}
