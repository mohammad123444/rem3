package ir.yadavar.shamsi.ui

import android.app.Application
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.yadavar.shamsi.core.JDate
import ir.yadavar.shamsi.data.AppDb
import ir.yadavar.shamsi.data.GroupEntity
import ir.yadavar.shamsi.data.Ids
import ir.yadavar.shamsi.data.ReminderEntity
import ir.yadavar.shamsi.data.ReminderOps
import ir.yadavar.shamsi.data.Settings
import ir.yadavar.shamsi.data.SettingsStore
import ir.yadavar.shamsi.widget.TodayWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext
    private val db = AppDb.get(ctx)

    val reminders: StateFlow<List<ReminderEntity>> = db.reminders().observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val groups: StateFlow<List<GroupEntity>> = db.groups().observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val settings: StateFlow<Settings> = SettingsStore.flow

    private fun io(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) { block() }
    }

    fun save(reminder: ReminderEntity) = io { ReminderOps.save(ctx, reminder) }
    fun delete(id: String) = io { ReminderOps.delete(ctx, id) }
    fun toggleItem(reminderId: String, itemId: String) = io { ReminderOps.toggleItem(ctx, reminderId, itemId) }
    fun complete(id: String) = io { ReminderOps.complete(ctx, id) }
    fun restore(id: String) = io { ReminderOps.restore(ctx, id) }
    fun snooze(id: String, minutes: Int) = io { ReminderOps.snooze(ctx, id, minutes) }
    fun reschedule(id: String, date: JDate, hour: Int, minute: Int) = io { ReminderOps.reschedule(ctx, id, date, hour, minute) }

    fun toggleExpanded(id: String) = io {
        val current = db.reminders().byId(id) ?: return@io
        db.reminders().upsert(current.copy(expanded = !current.expanded))
        TodayWidget().updateAll(ctx)
    }

    fun toggleStar(id: String) = io {
        val current = db.reminders().byId(id) ?: return@io
        db.reminders().upsert(current.copy(starred = !current.starred, updatedAt = System.currentTimeMillis()))
        TodayWidget().updateAll(ctx)
    }

    fun saveGroup(id: String?, name: String, color: String) = io {
        db.groups().upsert(GroupEntity(id ?: Ids.new(), name.trim(), color))
    }

    fun deleteGroup(id: String) = io {
        db.groups().delete(id)
        db.reminders().all().filter { it.groupId == id }.forEach {
            ReminderOps.save(ctx, it.copy(groupId = "", updatedAt = System.currentTimeMillis()))
        }
    }

    fun updateSettings(next: Settings) = io {
        SettingsStore.update(ctx, next)
        TodayWidget().updateAll(ctx)
    }
}
