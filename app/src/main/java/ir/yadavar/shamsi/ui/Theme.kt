package ir.yadavar.shamsi.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import ir.yadavar.shamsi.data.Settings

val GroupColorNames = listOf("blue", "teal", "green", "amber", "orange", "rose", "violet", "slate")

val GroupColorLabels = mapOf(
    "blue" to "آبی", "teal" to "سبزآبی", "green" to "سبز", "amber" to "کهربایی",
    "orange" to "نارنجی", "rose" to "صورتی", "violet" to "بنفش", "slate" to "خاکستری",
)

fun groupColor(name: String): Color = when (name) {
    "teal" -> Color(0xFF0D9488)
    "green" -> Color(0xFF16A34A)
    "amber" -> Color(0xFFD97706)
    "orange" -> Color(0xFFEA580C)
    "rose" -> Color(0xFFE11D48)
    "violet" -> Color(0xFF7C3AED)
    "slate" -> Color(0xFF64748B)
    else -> Color(0xFF1A56DB)
}

private fun accentColor(name: String): Color = when (name) {
    "teal" -> Color(0xFF0D9488)
    "amber" -> Color(0xFFD97706)
    "rose" -> Color(0xFFE11D48)
    else -> Color(0xFF1A56DB)
}

@Composable
fun YadavarTheme(settings: Settings, content: @Composable () -> Unit) {
    val dark = when (settings.theme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val scheme = if (settings.accent == "dynamic" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        val primary = accentColor(settings.accent)
        if (dark) darkColorScheme(primary = primary, onPrimary = Color.White)
        else lightColorScheme(primary = primary, onPrimary = Color.White)
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
