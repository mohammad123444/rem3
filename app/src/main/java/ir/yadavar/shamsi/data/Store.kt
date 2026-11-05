package ir.yadavar.shamsi.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import ir.yadavar.shamsi.core.JDate
import ir.yadavar.shamsi.core.Jalali
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class CheckItem(val id: String, val title: String, val done: Boolean)

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: String,
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val title: String,
    val items: List<CheckItem>,
    val jy: Int?,
    val jm: Int?,
    val jd: Int?,
    val hour: Int?,
    val minute: Int?,
    val location: String,
    val groupId: String,
    val starred: Boolean,
    val alert: Boolean,
    val repeatKind: String,
    val repeatEvery: Int,
    val completed: Boolean,
    val completedAt: Long?,
    val expanded: Boolean,
    val snoozeUntil: Long?,
    val dismissedKey: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

class Converters {
    @TypeConverter
    fun itemsToJson(items: List<CheckItem>): String {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().put("id", item.id).put("title", item.title).put("done", item.done))
        }
        return array.toString()
    }

    @TypeConverter
    fun jsonToItems(raw: String?): List<CheckItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(CheckItem(obj.getString("id"), obj.optString("title"), obj.optBoolean("done")))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups")
    fun observe(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups")
    suspend fun all(): List<GroupEntity>

    @Query("SELECT COUNT(*) FROM groups")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: GroupEntity)

    @Query("DELETE FROM groups WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY createdAt DESC")
    fun observe(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders")
    suspend fun all(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun byId(id: String): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(entities = [ReminderEntity::class, GroupEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDb : RoomDatabase() {
    abstract fun reminders(): ReminderDao
    abstract fun groups(): GroupDao

    companion object {
        @Volatile private var instance: AppDb? = null

        fun get(context: Context): AppDb {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, AppDb::class.java, "yadavar.db")
                    .build()
                    .also { instance = it }
            }
        }
    }
}

object Ids {
    fun new() = UUID.randomUUID().toString()
}

fun ReminderEntity.dateOrNull(): JDate? {
    val y = jy ?: return null
    val m = jm ?: return null
    val d = jd ?: return null
    return JDate(y, m, d)
}

fun ReminderEntity.occurrenceEpoch(): Long? {
    val y = jy ?: return null
    val m = jm ?: return null
    val d = jd ?: return null
    val h = hour ?: return null
    val min = minute ?: return null
    return Jalali.dateEpoch(y, m, d, h, min)
}

fun ReminderEntity.ringKey(now: Long): String? {
    if (completed || !alert) return null
    val window = 6 * 60 * 60 * 1000L
    val snooze = snoozeUntil
    if (snooze != null) {
        if (now < snooze || now - snooze > window) return null
        val key = "snooze:$snooze"
        if (dismissedKey == key) return null
        return key
    }
    val at = occurrenceEpoch() ?: return null
    if (now < at || now - at > window) return null
    val key = "at:$at"
    if (dismissedKey == key) return null
    return key
}

fun ReminderEntity.triggerAt(now: Long = System.currentTimeMillis()): Long? {
    if (completed || !alert) return null
    val snooze = snoozeUntil
    if (snooze != null) {
        if (dismissedKey == "snooze:$snooze") return null
        return snooze
    }
    val at = occurrenceEpoch() ?: return null
    if (dismissedKey == "at:$at") return null
    return at
}

private fun stepDate(date: JDate, kind: String, every: Int): JDate {
    val step = every.coerceAtLeast(1)
    return when (kind) {
        "daily" -> Jalali.addDays(date, step)
        "weekly" -> Jalali.addDays(date, 7 * step)
        "monthly" -> Jalali.addMonths(date, step)
        else -> Jalali.addMonths(date, 12 * step)
    }
}

fun ReminderEntity.advanceDate(now: Long): JDate? {
    val date = dateOrNull() ?: return null
    if (repeatKind == "none") return null
    val today = Jalali.today(now)
    var cursor = date
    repeat(500) {
        cursor = stepDate(cursor, repeatKind, repeatEvery)
        val cmp = Jalali.compare(cursor, today)
        if (cmp > 0) return cursor
        if (cmp == 0) {
            if (hour == null || minute == null) return cursor
            if (Jalali.dateEpoch(cursor.jy, cursor.jm, cursor.jd, hour, minute) > now) return cursor
        }
    }
    return cursor
}

enum class Section { OVERDUE, TODAY, TOMORROW, WEEK, LATER, UNDATED }

fun ReminderEntity.section(now: Long = System.currentTimeMillis()): Section {
    val date = dateOrNull() ?: return Section.UNDATED
    val today = Jalali.today(now)
    val cmp = Jalali.compare(date, today)
    if (cmp < 0) return Section.OVERDUE
    if (cmp == 0) {
        val at = occurrenceEpoch()
        if (at != null && at < now) return Section.OVERDUE
        return Section.TODAY
    }
    if (Jalali.compare(date, Jalali.addDays(today, 1)) == 0) return Section.TOMORROW
    val weekEnd = Jalali.addDays(Jalali.startOfIranWeek(today), 7)
    if (Jalali.compare(date, weekEnd) < 0) return Section.WEEK
    return Section.LATER
}

fun repeatLabel(kind: String, every: Int): String {
    if (kind == "none") return "بدون تکرار"
    val unit = when (kind) {
        "daily" -> "روز"
        "weekly" -> "هفته"
        "monthly" -> "ماه"
        else -> "سال"
    }
    if (every <= 1) {
        return when (kind) {
            "daily" -> "هر روز"
            "weekly" -> "هر هفته"
            "monthly" -> "هر ماه شمسی"
            else -> "هر سال شمسی"
        }
    }
    return "هر ${ir.yadavar.shamsi.core.faNum(every)} $unit"
}
