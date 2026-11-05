package ir.yadavar.shamsi.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import ir.yadavar.shamsi.R
import ir.yadavar.shamsi.YadavarApp
import ir.yadavar.shamsi.core.formatDayMonth
import ir.yadavar.shamsi.core.formatTime
import ir.yadavar.shamsi.data.AppDb
import ir.yadavar.shamsi.data.ReminderEntity
import ir.yadavar.shamsi.data.ReminderOps
import ir.yadavar.shamsi.data.SettingsStore
import ir.yadavar.shamsi.data.dateOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(AlarmScheduler.EXTRA_ID) ?: return
        val pending = goAsync()
        val action = intent.action
        val minutes = intent.getIntExtra(AlarmScheduler.EXTRA_MINUTES, 0)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    AlarmScheduler.ACTION_SNOOZE -> ReminderOps.snooze(context, id, if (minutes > 0) minutes else 15)
                    AlarmScheduler.ACTION_DISMISS -> ReminderOps.dismiss(context, id)
                    else -> {
                        val reminder = AppDb.get(context).reminders().byId(id) ?: return@launch
                        if (!reminder.completed && reminder.alert && SettingsStore.current(context).notify) show(context, reminder)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun show(context: Context, reminder: ReminderEntity) {
        val open = Intent(context, AlarmActivity::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_ID, reminder.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val content = PendingIntent.getActivity(
            context,
            reminder.id.hashCode(),
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        fun action(label: String, act: String, minutes: Int): NotificationCompat.Action {
            val broadcast = Intent(context, AlarmReceiver::class.java).apply {
                this.action = act
                putExtra(AlarmScheduler.EXTRA_ID, reminder.id)
                putExtra(AlarmScheduler.EXTRA_MINUTES, minutes)
                data = android.net.Uri.parse("yadavar://$act/${reminder.id}/$minutes")
            }
            val pi = PendingIntent.getBroadcast(
                context,
                (reminder.id + act + minutes).hashCode(),
                broadcast,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            return NotificationCompat.Action.Builder(0, label, pi).build()
        }
        val date = reminder.dateOrNull()
        val text = buildString {
            if (date != null) append(formatDayMonth(date))
            if (reminder.hour != null && reminder.minute != null) {
                if (isNotEmpty()) append(" · ")
                append(formatTime(reminder.hour, reminder.minute))
            }
            if (reminder.location.isNotBlank()) {
                if (isNotEmpty()) append(" · ")
                append(reminder.location)
            }
        }
        val notification = NotificationCompat.Builder(context, YadavarApp.CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(reminder.title.ifBlank { "یادآور" })
            .setContentText(text.ifBlank { "وقت یادآور است" })
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(content)
            .setFullScreenIntent(content, true)
            .addAction(action("۵ دقیقه", AlarmScheduler.ACTION_SNOOZE, 5))
            .addAction(action("۱۵ دقیقه", AlarmScheduler.ACTION_SNOOZE, 15))
            .addAction(action("انجام شد", AlarmScheduler.ACTION_DISMISS, 0))
            .build()
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(reminder.id.hashCode(), notification)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AlarmScheduler.rescheduleAll(context)
            } finally {
                pending.finish()
            }
        }
    }
}
