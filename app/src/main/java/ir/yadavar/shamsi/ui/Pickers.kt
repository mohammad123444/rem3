package ir.yadavar.shamsi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.yadavar.shamsi.core.JDate
import ir.yadavar.shamsi.core.Jalali
import ir.yadavar.shamsi.core.faNum

private val WeekHeaders = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

@Composable
fun JalaliDatePickerDialog(initial: JDate, onDismiss: () -> Unit, onPick: (JDate) -> Unit) {
    var year by remember { mutableIntStateOf(initial.jy) }
    var month by remember { mutableIntStateOf(initial.jm) }
    var selected by remember { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("انتخاب تاریخ") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = {
                        if (month == 12) { month = 1; year += 1 } else month += 1
                    }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "ماه بعد") }
                    Text(
                        "${Jalali.MONTH_NAMES[month - 1]} ${faNum(year)}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    IconButton(onClick = {
                        if (month == 1) { month = 12; year -= 1 } else month -= 1
                    }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "ماه قبل") }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    WeekHeaders.forEach {
                        Text(
                            it,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                val offset = Jalali.iranWeekday(JDate(year, month, 1))
                val length = Jalali.monthLength(year, month)
                val today = Jalali.today()
                val cells = offset + length
                val rowCount = (cells + 6) / 7
                for (row in 0 until rowCount) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (col in 0 until 7) {
                            val day = row * 7 + col - offset + 1
                            Box(
                                modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (day in 1..length) {
                                    val date = JDate(year, month, day)
                                    val isSelected = Jalali.compare(date, selected) == 0
                                    val isToday = Jalali.compare(date, today) == 0
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    isToday -> MaterialTheme.colorScheme.primaryContainer
                                                    else -> androidx.compose.ui.graphics.Color.Transparent
                                                },
                                            )
                                            .clickable { selected = date }
                                            .fillMaxWidth()
                                            .aspectRatio(1f),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            faNum(day),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(selected) }) { Text("تأیید") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onPick: (Int, Int) -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("انتخاب ساعت") },
        text = { TimePicker(state = state) },
        confirmButton = { TextButton(onClick = { onPick(state.hour, state.minute) }) { Text("تأیید") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}

/** Date picker followed by a time picker; used for "other time" snooze. */
@Composable
fun JalaliDateTimeFlow(onDismiss: () -> Unit, onPicked: (JDate, Int, Int) -> Unit) {
    var date by remember { mutableStateOf<JDate?>(null) }
    val picked = date
    if (picked == null) {
        JalaliDatePickerDialog(initial = Jalali.today(), onDismiss = onDismiss, onPick = { date = it })
    } else {
        val cal = java.util.Calendar.getInstance()
        TimePickerDialog(
            initialHour = cal.get(java.util.Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(java.util.Calendar.MINUTE),
            onDismiss = onDismiss,
            onPick = { h, m -> onPicked(picked, h, m) },
        )
    }
}
