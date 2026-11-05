package ir.yadavar.shamsi

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import ir.yadavar.shamsi.data.AppDb
import ir.yadavar.shamsi.data.GroupEntity
import ir.yadavar.shamsi.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class YadavarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsStore.load(this)
        createChannel()
        CoroutineScope(Dispatchers.IO).launch { seedGroups() }
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val channel = NotificationChannel(CHANNEL, "یادآورها", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "اعلان و زنگ یادآورها"
            enableVibration(true)
            setSound(sound, attrs)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    private suspend fun seedGroups() {
        val dao = AppDb.get(this).groups()
        if (dao.count() > 0) return
        dao.upsert(GroupEntity("personal", "شخصی", "blue"))
        dao.upsert(GroupEntity("work", "کار", "teal"))
        dao.upsert(GroupEntity("shopping", "خرید", "amber"))
        dao.upsert(GroupEntity("health", "سلامت", "rose"))
    }

    companion object {
        const val CHANNEL = "reminders_alarm"
    }
}
