package ir.yadavar.shamsi.core

data class ParsedSpeech(
    val title: String,
    val jy: Int?,
    val jm: Int?,
    val jd: Int?,
    val hour: Int?,
    val minute: Int?,
)

object Speech {
    private val ones = listOf(
        "نوزده" to 19, "هجده" to 18, "هفده" to 17, "شانزده" to 16, "پونزده" to 15, "پانزده" to 15,
        "چهارده" to 14, "سیزده" to 13, "دوازده" to 12, "یازده" to 11, "ده" to 10, "نه" to 9,
        "هشت" to 8, "هفت" to 7, "شیش" to 6, "شش" to 6, "پنج" to 5, "چهار" to 4, "سه" to 3,
        "دو" to 2, "یک" to 1, "یه" to 1, "صفر" to 0,
    )
    private val tens = listOf(
        "نود" to 90, "هشتاد" to 80, "هفتاد" to 70, "شصت" to 60, "پنجاه" to 50, "چهل" to 40, "سی" to 30, "بیست" to 20,
    )
    private val weekdays = listOf(
        "چهارشنبه" to 4, "پنجشنبه" to 5, "سه شنبه" to 3, "سهشنبه" to 3, "دوشنبه" to 2,
        "یکشنبه" to 1, "یک شنبه" to 1, "شنبه" to 0, "جمعه" to 6,
    )

    private fun isFa(ch: Char?) = ch != null && ch in '\u0600'..'\u06FF'

    fun normalize(input: String): String {
        val fa = "۰۱۲۳۴۵۶۷۸۹"
        val ar = "٠١٢٣٤٥٦٧٨٩"
        return input
            .replace("\u200c", " ")
            .replace("\u200d", " ")
            .replace('\u064a', '\u06cc')
            .replace('\u0643', '\u06a9')
            .map { ch ->
                val fi = fa.indexOf(ch)
                if (fi >= 0) fi.digitToChar()
                else {
                    val ai = ar.indexOf(ch)
                    if (ai >= 0) ai.digitToChar() else ch
                }
            }
            .joinToString("")
            .replace(Regex("[،,؛;]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private data class Num(val value: Int, val consumed: Int)

    private fun readNumber(source: String): Num? {
        val digit = Regex("^(\\d{1,4})").find(source)
        if (digit != null) return Num(digit.groupValues[1].toInt(), digit.groupValues[1].length)
        for ((word, value) in tens) {
            if (!source.startsWith(word)) continue
            val rest = source.substring(word.length)
            val and = Regex("^\\s+و\\s+").find(rest)
            if (and != null) {
                val after = rest.substring(and.value.length)
                for ((onesWord, onesValue) in ones) {
                    if (onesValue >= 10) continue
                    if (after.startsWith(onesWord) && !isFa(after.getOrNull(onesWord.length))) {
                        return Num(value + onesValue, word.length + and.value.length + onesWord.length)
                    }
                }
            }
            if (!isFa(source.getOrNull(word.length))) return Num(value, word.length)
        }
        for ((word, value) in ones) {
            if (source.startsWith(word) && !isFa(source.getOrNull(word.length))) return Num(value, word.length)
        }
        return null
    }

    private fun applyPeriod(hour: Int, minute: Int, period: String?): Pair<Int, Int>? {
        if (minute > 59 || hour > 24) return null
        if (period == null) return if (hour > 23) null else hour to minute
        val p = period.replace(" ", "")
        if (p == "صبح" || p == "بامداد") {
            if (hour == 12) return 0 to minute
            if (hour > 23) return null
            return hour to minute
        }
        if (p == "ظهر") {
            if (hour == 0 || hour == 12) return 12 to minute
            if (hour < 12) return hour + 12 to minute
            if (hour > 23) return null
            return hour to minute
        }
        if (hour == 12 && p == "شب") return 0 to minute
        if (hour in 13..23) return hour to minute
        if (hour == 12) return 12 to minute
        if (hour > 12) return null
        return hour + 12 to minute
    }

    private data class Clock(val hour: Int, val minute: Int, val consumed: Int)

    private fun parseClock(source: String): Clock? {
        val n = readNumber(source) ?: return null
        var i = n.consumed
        var minute = 0
        if (source.getOrNull(i) == ':' || source.getOrNull(i) == '.') {
            val mm = Regex("^[:.](\\d{2})").find(source.substring(i)) ?: return null
            minute = mm.groupValues[1].toInt()
            i += mm.value.length
        } else {
            val and = Regex("^\\s+و\\s+").find(source.substring(i))
            if (and != null) {
                val after = source.substring(i + and.value.length)
                when {
                    after.startsWith("نیم") && !isFa(after.getOrNull(3)) -> {
                        minute = 30
                        i += and.value.length + 3
                    }
                    after.startsWith("ربع") && !isFa(after.getOrNull(3)) -> {
                        minute = 15
                        i += and.value.length + 3
                    }
                    else -> {
                        val second = readNumber(after)
                        if (second != null && second.value <= 59) {
                            minute = second.value
                            i += and.value.length + second.consumed
                        }
                    }
                }
            }
        }
        var period: String? = null
        val periodMatch = Regex("^\\s+(صبح|بامداد|ظهر|بعد از ظهر|بعدازظهر|عصر|غروب|شب)").find(source.substring(i))
        if (periodMatch != null) {
            period = periodMatch.groupValues[1]
            i += periodMatch.value.length
        }
        val applied = applyPeriod(n.value, minute, period) ?: return null
        return Clock(applied.first, applied.second, i)
    }

    private data class Span(val start: Int, val end: Int, val jy: Int? = null, val jm: Int? = null, val jd: Int? = null, val hour: Int? = null, val minute: Int? = null)

    private fun extractTime(text: String): Span? {
        var from = 0
        while (from < text.length) {
            val at = text.indexOf("ساعت", from)
            if (at < 0) break
            val prev = if (at > 0) text[at - 1] else ' '
            if (prev == ' ' || at == 0) {
                var i = at + 4
                if (text.getOrNull(i) == ' ') i += 1
                val parsed = parseClock(text.substring(i))
                if (parsed != null) return Span(at, i + parsed.consumed, hour = parsed.hour, minute = parsed.minute)
            }
            from = at + 4
        }
        val clock = Regex("(^|\\s)(\\d{1,2}):(\\d{2})(?=\\s|$)").find(text)
        if (clock != null) {
            val hour = clock.groupValues[2].toInt()
            val minute = clock.groupValues[3].toInt()
            if (hour <= 23 && minute <= 59) {
                val start = clock.range.first + clock.groupValues[1].length
                return Span(start, clock.range.last + 1, hour = hour, minute = minute)
            }
        }
        return null
    }

    private fun findBounded(text: String, word: String): Int {
        var from = 0
        while (from < text.length) {
            val index = text.indexOf(word, from)
            if (index < 0) return -1
            val prev = text.getOrNull(index - 1)
            val next = text.getOrNull(index + word.length)
            if (!isFa(prev) && !isFa(next)) return index
            from = index + word.length
        }
        return -1
    }

    private fun trailingNumber(before: String): Pair<Int, Int>? {
        val digits = Regex("(\\d{1,2})$").find(before)
        if (digits != null) {
            val prev = before.getOrNull(digits.range.first - 1) ?: ' '
            if (digits.range.first == 0 || prev == ' ') return digits.groupValues[1].toInt() to digits.range.first
        }
        val max = minOf(18, before.length)
        for (len in max downTo 1) {
            val start = before.length - len
            val prev = if (start > 0) before[start - 1] else ' '
            if (start > 0 && prev != ' ') continue
            val parsed = readNumber(before.substring(start))
            if (parsed != null && parsed.consumed == len) return parsed.value to start
        }
        return null
    }

    private fun resolveMonthDay(today: JDate, jm: Int, jdWant: Int): JDate {
        fun clamp(jy: Int) = JDate(jy, jm, jdWant.coerceIn(1, Jalali.monthLength(jy, jm)))
        val current = clamp(today.jy)
        return if (Jalali.compare(current, today) >= 0) current else clamp(today.jy + 1)
    }

    private fun extractDate(text: String, today: JDate): Span? {
        val hits = mutableListOf<Span>()
        val relative = listOf("پس فردا" to 2, "پسفردا" to 2, "فردا" to 1, "امروز" to 0, "دیروز" to -1)
        for ((word, delta) in relative) {
            val index = findBounded(text, word)
            if (index >= 0) {
                val d = Jalali.addDays(today, delta)
                hits.add(Span(index, index + word.length, d.jy, d.jm, d.jd))
            }
        }
        for (word in listOf("هفته آینده", "هفته بعد")) {
            val index = findBounded(text, word)
            if (index >= 0) {
                val d = Jalali.addDays(today, 7)
                hits.add(Span(index, index + word.length, d.jy, d.jm, d.jd))
            }
        }
        val monthAhead = findBounded(text, "ماه آینده")
        if (monthAhead >= 0) {
            val d = Jalali.addMonths(today, 1)
            hits.add(Span(monthAhead, monthAhead + "ماه آینده".length, d.jy, d.jm, d.jd))
        }
        for ((word, weekday) in weekdays) {
            val index = findBounded(text, word)
            if (index >= 0) {
                val d = Jalali.nextWeekday(today, weekday)
                hits.add(Span(index, index + word.length, d.jy, d.jm, d.jd))
            }
        }
        Jalali.MONTH_NAMES.forEachIndexed { monthIndex, name ->
            val jm = monthIndex + 1
            val index = findBounded(text, name)
            if (index < 0) return@forEachIndexed
            val before = text.substring(0, index).replace(Regex("\\s+$"), "")
            val lead = trailingNumber(before)
            if (lead != null && lead.first in 1..31) {
                val d = resolveMonthDay(today, jm, lead.first)
                hits.add(Span(lead.second, index + name.length, d.jy, d.jm, d.jd))
                return@forEachIndexed
            }
            val after = text.substring(index + name.length)
            val space = Regex("^\\s+").find(after) ?: return@forEachIndexed
            val parsed = readNumber(after.substring(space.value.length)) ?: return@forEachIndexed
            if (parsed.value !in 1..31) return@forEachIndexed
            if (isFa(after.getOrNull(space.value.length + parsed.consumed))) return@forEachIndexed
            val d = resolveMonthDay(today, jm, parsed.value)
            hits.add(Span(index, index + name.length + space.value.length + parsed.consumed, d.jy, d.jm, d.jd))
        }
        return hits.minWithOrNull(compareBy<Span> { it.start }.thenBy { it.end })
    }

    private fun cut(text: String, span: Span?): String {
        if (span == null) return text
        return "${text.substring(0, span.start)} ${text.substring(span.end)}".replace(Regex("\\s+"), " ").trim()
    }

    private fun cleanupTitle(text: String, hadCue: Boolean): String {
        var next = " $text "
        val cues = listOf("یادآوری کنید", "یادآوری کن", "یاد اوری کن", "یادم بنداز", "یادم بیار", "یادآور بساز", "یه یادآور", "یک یادآور", "یادآوری", "یادآور")
        for (cue in cues) next = next.replace(cue, " ")
        if (hadCue) next = next.replace("برای", " ")
        next = next.replace(Regex("\\s+"), " ").trim()
        repeat(4) {
            val stripped = next.replace(Regex("^(و|که|را|رو|یه|یک|این|ساعت)\\s+"), "")
            if (stripped == next) return@repeat
            next = stripped
        }
        return next.trim(' ', '.', '-')
    }

    fun parse(input: String, now: Long = System.currentTimeMillis()): ParsedSpeech {
        val original = normalize(input)
        val hadCue = Regex("یادآوری|یادآور|یادم").containsMatchIn(original)
        val time = extractTime(original)
        var rest = cut(original, time)
        val date = extractDate(rest, Jalali.today(now))
        rest = cut(rest, date)
        val title = cleanupTitle(rest, hadCue).ifBlank { cleanupTitle(cut(cut(original, time), date), false) }
        return ParsedSpeech(title, date?.jy, date?.jm, date?.jd, time?.hour, time?.minute)
    }
}
