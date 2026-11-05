package ir.yadavar.shamsi.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import ir.yadavar.shamsi.data.AppDb
import ir.yadavar.shamsi.data.ReminderEntity
import ir.yadavar.shamsi.data.triggerAt

object AlarmScheduler {
    const val ACTION_RING = "ir.yadavar.shamsi.RING"
    const val ACTION_SNOOZE = "ir.yadavar.shamsi.SNOOZE"
    const val ACTION_DISMISS = "ir.yadavar.shamsi.DISMISS"
    const val EXTRA_ID = "reminder_id"
    const val EXTRA_MINUTES = "minutes"

    private fun pending(context: Context, id: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_RING
            putExtra(EXTRA_ID, id)
            data = Uri.parse("yadavar://reminder/$id")
        }
        return PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun cancel(context: Context, id: String) {
        val manager = context.getSystemService(AlarmManager::class.java)
        manager.cancel(pending(context, id))
    }

    fun sync(context: Context, reminder: ReminderEntity, now: Long = System.currentTimeMillis()) {
        val manager = context.getSystemService(AlarmManager::class.java)
        val pi = pending(context, reminder.id)
        val at = reminder.triggerAt(now)
        if (at == null) {
            manager.cancel(pi)
            return
        }
        val whenAt = when {
            at > now -> at
            now - at <= 6 * 60 * 60 * 1000L -> now + 1_500L
            else -> {
                manager.cancel(pi)
                return
            }
        }
        try {
            val exactAllowed = Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()
            if (exactAllowed) manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenAt, pi)
            else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenAt, pi)
        } catch (_: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenAt, pi)
        }
    }

    suspend fun rescheduleAll(context: Context) {
        val all = AppDb.get(context).reminders().all()
        all.forEach { sync(context, it) }
    }
}
