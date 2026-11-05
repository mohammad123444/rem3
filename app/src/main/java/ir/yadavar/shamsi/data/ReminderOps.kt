package ir.yadavar.shamsi.data

import android.app.NotificationManager
import android.content.Context
import ir.yadavar.shamsi.alarm.AlarmScheduler
import ir.yadavar.shamsi.core.JDate
import androidx.glance.appwidget.updateAll
import ir.yadavar.shamsi.widget.TodayWidget
import java.util.UUID

object ReminderOps {
    private fun cancelNote(context: Context, id: String) {
        context.getSystemService(NotificationManager::class.java).cancel(id.hashCode())
    }

    private suspend fun refresh(context: Context) {
        TodayWidget().updateAll(context)
    }

    suspend fun save(context: Context, reminder: ReminderEntity) {
        AppDb.get(context).reminders().upsert(reminder)
        AlarmScheduler.sync(context, reminder)
        refresh(context)
    }

    suspend fun delete(context: Context, id: String) {
        AppDb.get(context).reminders().delete(id)
        AlarmScheduler.cancel(context, id)
        cancelNote(context, id)
        refresh(context)
    }

    suspend fun toggleItem(context: Context, reminderId: String, itemId: String) {
        val db = AppDb.get(context)
        val current = db.reminders().byId(reminderId) ?: return
        val next = current.copy(
            items = current.items.map { if (it.id == itemId) it.copy(done = !it.done) else it },
            updatedAt = System.currentTimeMillis(),
        )
        db.reminders().upsert(next)
        refresh(context)
    }

    suspend fun complete(context: Context, id: String) {
        val db = AppDb.get(context)
        val current = db.reminders().byId(id) ?: return
        val now = System.currentTimeMillis()
        if (current.repeatKind != "none") {
            val nextDate = current.advanceDate(now)
            if (nextDate != null) {
                val archived = current.copy(
                    id = UUID.randomUUID().toString(),
                    repeatKind = "none",
                    repeatEvery = 1,
                    completed = true,
                    completedAt = now,
                    alert = false,
                    snoozeUntil = null,
                    dismissedKey = null,
                    updatedAt = now,
                )
                val moved = current.copy(
                    jy = nextDate.jy,
                    jm = nextDate.jm,
                    jd = nextDate.jd,
                    completed = false,
                    completedAt = null,
                    snoozeUntil = null,
                    dismissedKey = null,
                    items = current.items.map { it.copy(done = false) },
                    updatedAt = now,
                )
                db.reminders().upsert(moved)
                db.reminders().upsert(archived)
                AlarmScheduler.sync(context, moved)
                cancelNote(context, id)
                refresh(context)
                return
            }
        }
        val done = current.copy(completed = true, completedAt = now, snoozeUntil = null, updatedAt = now)
        db.reminders().upsert(done)
        AlarmScheduler.cancel(context, id)
        cancelNote(context, id)
        refresh(context)
    }

    suspend fun restore(context: Context, id: String) {
        val db = AppDb.get(context)
        val current = db.reminders().byId(id) ?: return
        val next = current.copy(completed = false, completedAt = null, updatedAt = System.currentTimeMillis())
        db.reminders().upsert(next)
        AlarmScheduler.sync(context, next)
        refresh(context)
    }

    suspend fun snooze(context: Context, id: String, minutes: Int) {
        val db = AppDb.get(context)
        val current = db.reminders().byId(id) ?: return
        val until = System.currentTimeMillis() + minutes.coerceAtLeast(1) * 60_000L
        val next = current.copy(snoozeUntil = until, dismissedKey = null, updatedAt = System.currentTimeMillis())
        db.reminders().upsert(next)
        AlarmScheduler.sync(context, next)
        cancelNote(context, id)
        refresh(context)
    }

    suspend fun reschedule(context: Context, id: String, date: JDate, hour: Int, minute: Int) {
        val db = AppDb.get(context)
        val current = db.reminders().byId(id) ?: return
        val next = current.copy(
            jy = date.jy,
            jm = date.jm,
            jd = date.jd,
            hour = hour,
            minute = minute,
            snoozeUntil = null,
            dismissedKey = null,
            alert = true,
            updatedAt = System.currentTimeMillis(),
        )
        db.reminders().upsert(next)
        AlarmScheduler.sync(context, next)
        cancelNote(context, id)
        refresh(context)
    }

    suspend fun dismiss(context: Context, id: String) {
        val db = AppDb.get(context)
        val current = db.reminders().byId(id) ?: return
        val now = System.currentTimeMillis()
        val key = current.ringKey(now) ?: current.snoozeUntil?.let { "snooze:$it" } ?: current.occurrenceEpoch()?.let { "at:$it" }
        val next = current.copy(dismissedKey = key, snoozeUntil = null, updatedAt = now)
        db.reminders().upsert(next)
        AlarmScheduler.sync(context, next)
        cancelNote(context, id)
        refresh(context)
    }
}
