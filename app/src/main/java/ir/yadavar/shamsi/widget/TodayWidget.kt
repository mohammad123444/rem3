package ir.yadavar.shamsi.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.CheckboxDefaults
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import ir.yadavar.shamsi.MainActivity
import ir.yadavar.shamsi.R
import ir.yadavar.shamsi.core.Jalali
import ir.yadavar.shamsi.core.formatDayMonth
import ir.yadavar.shamsi.core.formatTime
import ir.yadavar.shamsi.data.AppDb
import ir.yadavar.shamsi.data.CheckItem
import ir.yadavar.shamsi.data.ReminderEntity
import ir.yadavar.shamsi.data.ReminderOps
import ir.yadavar.shamsi.data.Section
import ir.yadavar.shamsi.data.SettingsStore
import ir.yadavar.shamsi.data.dateOrNull
import ir.yadavar.shamsi.data.occurrenceEpoch
import ir.yadavar.shamsi.data.section

private val ReminderIdKey = ActionParameters.Key<String>("reminder_id")
private val ItemIdKey = ActionParameters.Key<String>("item_id")

private val BgColor = Color(0xFF16181D)
private val TextColor = Color(0xFFF4F6F8)
private val MutedColor = Color(0xFF9AA3AF)
private val AccentColor = Color(0xFF4C8DFF)

sealed interface WidgetRow {
    val key: Long

    data class Main(val reminder: ReminderEntity) : WidgetRow {
        override val key: Long get() = reminder.id.hashCode().toLong()
    }

    data class Sub(val reminder: ReminderEntity, val item: CheckItem) : WidgetRow {
        override val key: Long get() = (reminder.id + item.id).hashCode().toLong()
    }
}

/** Title and meta font sizes (sp) for the three widget font levels. */
private fun widgetSizes(font: String): Pair<Int, Int> = when (font) {
    "sm" -> 12 to 10
    "lg" -> 18 to 14
    else -> 15 to 12
}

class TodayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val now = System.currentTimeMillis()
        val reminders = AppDb.get(context).reminders().all()
            .filter { !it.completed }
            .filter {
                val s = it.section(now)
                s == Section.TODAY || s == Section.OVERDUE
            }
            .sortedBy { it.occurrenceEpoch() ?: Long.MAX_VALUE }
        val rows = buildList {
            reminders.forEach { r ->
                add(WidgetRow.Main(r))
                if (r.expanded) r.items.forEach { add(WidgetRow.Sub(r, it)) }
            }
        }
        val (titleSp, metaSp) = widgetSizes(SettingsStore.font(context))
        val header = "یادآورهای امروز · " + formatDayMonth(Jalali.today(now))
        provideContent { WidgetContent(rows, header, titleSp, metaSp) }
    }
}

@Composable
private fun WidgetContent(rows: List<WidgetRow>, header: String, titleSp: Int, metaSp: Int) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(BgColor))
            .cornerRadius(20.dp)
            .padding(12.dp),
    ) {
        Text(
            text = header,
            style = TextStyle(
                color = ColorProvider(TextColor),
                fontSize = (titleSp + 1).sp,
                fontWeight = FontWeight.Bold,
            ),
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .clickable(actionStartActivity(Intent(LocalContext.current, MainActivity::class.java))),
        )
        if (rows.isEmpty()) {
            Text(
                text = "برای امروز کاری باقی نمانده است",
                style = TextStyle(color = ColorProvider(MutedColor), fontSize = metaSp.sp),
            )
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(rows, itemId = { it.key }) { row ->
                    when (row) {
                        is WidgetRow.Main -> MainRow(row.reminder, titleSp, metaSp)
                        is WidgetRow.Sub -> SubRow(row.reminder, row.item, titleSp)
                    }
                }
            }
        }
    }
}

private fun metaText(r: ReminderEntity): String = buildString {
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
}

@Composable
private fun MainRow(r: ReminderEntity, titleSp: Int, metaSp: Int) {
    val meta = metaText(r)
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            CheckBox(
                checked = false,
                onCheckedChange = actionRunCallback<CompleteAction>(actionParametersOf(ReminderIdKey to r.id)),
                text = r.title.ifBlank { "بدون عنوان" },
                style = TextStyle(color = ColorProvider(TextColor), fontSize = titleSp.sp),
                colors = CheckboxDefaults.colors(
                    checkedColor = ColorProvider(AccentColor),
                    uncheckedColor = ColorProvider(MutedColor),
                ),
            )
            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    style = TextStyle(color = ColorProvider(MutedColor), fontSize = metaSp.sp),
                    modifier = GlanceModifier.padding(start = 32.dp),
                )
            }
        }
        if (r.items.isNotEmpty()) {
            Image(
                provider = ImageProvider(if (r.expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more),
                contentDescription = if (r.expanded) "بستن زیرمجموعه‌ها" else "باز کردن زیرمجموعه‌ها",
                colorFilter = ColorFilter.tint(ColorProvider(MutedColor)),
                modifier = GlanceModifier
                    .size(32.dp)
                    .padding(4.dp)
                    .clickable(actionRunCallback<ToggleExpandAction>(actionParametersOf(ReminderIdKey to r.id))),
            )
        }
    }
}

@Composable
private fun SubRow(r: ReminderEntity, item: CheckItem, titleSp: Int) {
    val style = if (item.done) {
        TextStyle(
            color = ColorProvider(MutedColor),
            fontSize = (titleSp - 1).sp,
            textDecoration = TextDecoration.LineThrough,
        )
    } else {
        TextStyle(color = ColorProvider(TextColor), fontSize = (titleSp - 1).sp)
    }
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(start = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckBox(
            checked = item.done,
            onCheckedChange = actionRunCallback<ToggleItemAction>(
                actionParametersOf(ReminderIdKey to r.id, ItemIdKey to item.id),
            ),
            text = item.title,
            style = style,
            colors = CheckboxDefaults.colors(
                checkedColor = ColorProvider(MutedColor),
                uncheckedColor = ColorProvider(MutedColor),
            ),
        )
    }
}

class CompleteAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[ReminderIdKey] ?: return
        ReminderOps.complete(context, id)
    }
}

class ToggleItemAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[ReminderIdKey] ?: return
        val itemId = parameters[ItemIdKey] ?: return
        ReminderOps.toggleItem(context, id, itemId)
    }
}

class ToggleExpandAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[ReminderIdKey] ?: return
        val dao = AppDb.get(context).reminders()
        val current = dao.byId(id) ?: return
        dao.upsert(current.copy(expanded = !current.expanded))
        TodayWidget().updateAll(context)
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}
import android.content.Intent
import androidx.glance.LocalContext
