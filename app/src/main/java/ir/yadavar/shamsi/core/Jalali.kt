package ir.yadavar.shamsi.core

import java.util.Calendar
import kotlin.math.min

data class JDate(val jy: Int, val jm: Int, val jd: Int)

object Jalali {
    val MONTH_NAMES = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
    )
    val WEEKDAY_NAMES = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")

    private fun div(a: Int, b: Int) = a / b
    private fun mod(a: Int, b: Int) = a - div(a, b) * b

    private data class JalCal(val leap: Int, val gy: Int, val march: Int)

    private fun jalCal(jy: Int): JalCal {
        val breaks = intArrayOf(-61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178)
        val bl = breaks.size
        val gy = jy + 621
        var leapJ = -14
        var jp = breaks[0]
        require(jy >= jp && jy < breaks[bl - 1]) { "Invalid Jalaali year $jy" }
        var jump = 0
        for (i in 1 until bl) {
            val jm = breaks[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += div(jump, 33) * 8 + div(mod(jump, 33), 4)
            jp = jm
        }
        var n = jy - jp
        leapJ += div(n, 33) * 8 + div(mod(n, 33) + 3, 4)
        if (mod(jump, 33) == 4 && jump - n == 4) leapJ += 1
        val leapG = div(gy, 4) - div((div(gy, 100) + 1) * 3, 4) - 150
        val march = 20 + leapJ - leapG
        if (jump - n < 6) n = n - jump + div(jump + 4, 33) * 33
        var leap = mod(mod(n + 1, 33) - 1, 4)
        if (leap == -1) leap = 4
        return JalCal(leap, gy, march)
    }

    private fun g2d(gy: Int, gm: Int, gd: Int): Int {
        var d = div((gy + div(gm - 8, 6) + 100100) * 1461, 4) + div(153 * mod(gm + 9, 12) + 2, 5) + gd - 34840408
        d = d - div(div(gy + 100100 + div(gm - 8, 6), 100) * 3, 4) + 752
        return d
    }

    data class GDate(val gy: Int, val gm: Int, val gd: Int)

    private fun d2g(jdn: Int): GDate {
        var j = 4 * jdn + 139361631
        j += div(div(4 * jdn + 183187720, 146097) * 3, 4) * 4 - 3908
        val i = div(mod(j, 1461), 4) * 5 + 308
        val gd = div(mod(i, 153), 5) + 1
        val gm = mod(div(i, 153), 12) + 1
        val gy = div(j, 1461) - 100100 + div(8 - gm, 6)
        return GDate(gy, gm, gd)
    }

    fun j2d(jy: Int, jm: Int, jd: Int): Int {
        val r = jalCal(jy)
        return g2d(r.gy, 3, r.march) + (jm - 1) * 31 - div(jm, 7) * (jm - 7) + jd - 1
    }

    fun d2j(jdn: Int): JDate {
        val g = d2g(jdn)
        var jy = g.gy - 621
        val r = jalCal(jy)
        val jdn1f = g2d(g.gy, 3, r.march)
        var k = jdn - jdn1f
        if (k >= 0) {
            if (k <= 185) {
                val jm = 1 + div(k, 31)
                val jd = mod(k, 31) + 1
                return JDate(jy, jm, jd)
            }
            k -= 186
        } else {
            jy -= 1
            k += 179
            if (r.leap == 1) k += 1
        }
        val jm = 7 + div(k, 30)
        val jd = mod(k, 30) + 1
        return JDate(jy, jm, jd)
    }

    fun toJalali(cal: Calendar): JDate =
        d2j(g2d(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)))

    fun today(now: Long = System.currentTimeMillis()): JDate {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        return toJalali(cal)
    }

    fun toGregorian(jy: Int, jm: Int, jd: Int): GDate = d2g(j2d(jy, jm, jd))

    fun isLeap(jy: Int) = jalCal(jy).leap == 0

    fun monthLength(jy: Int, jm: Int): Int = when {
        jm <= 6 -> 31
        jm <= 11 -> 30
        isLeap(jy) -> 30
        else -> 29
    }

    fun compare(a: JDate, b: JDate): Int = when {
        a.jy != b.jy -> a.jy - b.jy
        a.jm != b.jm -> a.jm - b.jm
        else -> a.jd - b.jd
    }

    fun addDays(j: JDate, days: Int) = d2j(j2d(j.jy, j.jm, j.jd) + days)

    fun addMonths(j: JDate, months: Int): JDate {
        val total = j.jy * 12 + (j.jm - 1) + months
        val jy = Math.floorDiv(total, 12)
        val jm = Math.floorMod(total, 12) + 1
        return JDate(jy, jm, min(j.jd, monthLength(jy, jm)))
    }

    fun iranWeekday(j: JDate): Int {
        val g = toGregorian(j.jy, j.jm, j.jd)
        val cal = Calendar.getInstance()
        cal.set(g.gy, g.gm - 1, g.gd, 12, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val js = cal.get(Calendar.DAY_OF_WEEK) - 1
        return (js + 1) % 7
    }

    fun nextWeekday(from: JDate, target: Int): JDate {
        val delta = (target - iranWeekday(from) + 7) % 7
        return addDays(from, delta)
    }

    fun startOfIranWeek(j: JDate) = addDays(j, -iranWeekday(j))

    fun dateEpoch(jy: Int, jm: Int, jd: Int, hour: Int, minute: Int): Long {
        val g = toGregorian(jy, jm, jd)
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, g.gy)
        cal.set(Calendar.MONTH, g.gm - 1)
        cal.set(Calendar.DAY_OF_MONTH, g.gd)
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

fun faNum(value: Any?): String = value.toString().map { ch ->
    if (ch in '0'..'9') "۰۱۲۳۴۵۶۷۸۹"[ch - '0'] else ch
}.joinToString("")

fun formatTime(hour: Int, minute: Int): String =
    "${faNum(hour.toString().padStart(2, '0'))}:${faNum(minute.toString().padStart(2, '0'))}"

fun formatDayMonth(date: JDate) = "${faNum(date.jd)} ${Jalali.MONTH_NAMES[date.jm - 1]}"

fun formatLongDate(date: JDate): String {
    val weekday = Jalali.WEEKDAY_NAMES[Jalali.iranWeekday(date)]
    return "$weekday ${faNum(date.jd)} ${Jalali.MONTH_NAMES[date.jm - 1]} ${faNum(date.jy)}"
}
