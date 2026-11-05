package ir.yadavar.shamsi.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class Settings(
    val theme: String = "system",
    val accent: String = "blue",
    val widgetFont: String = "md",
    val notify: Boolean = true,
)

object SettingsStore {
    private const val PREF = "yadavar_settings"
    private val state = MutableStateFlow(Settings())
    val flow: StateFlow<Settings> = state

    fun load(context: Context) {
        val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        state.value = Settings(
            theme = p.getString("theme", "system") ?: "system",
            accent = p.getString("accent", "blue") ?: "blue",
            widgetFont = p.getString("widgetFont", "md") ?: "md",
            notify = p.getBoolean("notify", true),
        )
    }

    fun update(context: Context, next: Settings) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString("theme", next.theme)
            .putString("accent", next.accent)
            .putString("widgetFont", next.widgetFont)
            .putBoolean("notify", next.notify)
            .apply()
        state.value = next
    }

    fun current(context: Context): Settings {
        if (state.value == Settings() && !context.getSharedPreferences(PREF, Context.MODE_PRIVATE).contains("theme")) {
            load(context)
        } else if (!context.getSharedPreferences(PREF, Context.MODE_PRIVATE).contains("widgetFont") && state.value.widgetFont == "md") {
            load(context)
        }
        return state.value
    }

    fun font(context: Context): String {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("widgetFont", "md") ?: "md"
    }
}
