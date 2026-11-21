package com.hayoonjae.sciencetoday

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object Today {
    fun day(): Long = LocalDate.now().toEpochDay()
    fun label(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN))
}

/** 퀴즈 기록, 오답노트, 즐겨찾기. 전부 SharedPreferences 한 곳에 둔다. */
class Progress(ctx: Context) {
    private val p = ctx.getSharedPreferences("progress", 0)

    /** 오늘의 퀴즈를 끝낸 날들(epochDay). 연속 기록 계산에 쓴다. */
    var quizDays by mutableStateOf(p.getStringSet("quizDays", emptySet())!!.mapNotNull { it.toLongOrNull() }.toSet())
        private set
    var wrong by mutableStateOf(p.getStringSet("wrong", emptySet())!!.toSet())
        private set
    var favorites by mutableStateOf(p.getStringSet("fav", emptySet())!!.toSet())
        private set

    /** 분야별 (맞힌 수, 푼 수). */
    var stats by mutableStateOf(Field.entries.associateWith { p.getInt("ok_${it.key}", 0) to p.getInt("all_${it.key}", 0) })
        private set

    fun dailyScore(day: Long): Int? = p.getInt("daily_$day", -1).takeIf { it >= 0 }

    fun record(item: QItem, ok: Boolean) {
        val f = item.fact.field
        val (o, a) = stats.getValue(f)
        stats = stats + (f to ((if (ok) o + 1 else o) to a + 1))
        wrong = if (ok) wrong - item.key else wrong + item.key
        p.edit()
            .putInt("ok_${f.key}", stats.getValue(f).first)
            .putInt("all_${f.key}", stats.getValue(f).second)
            .putStringSet("wrong", wrong)
            .apply()
    }

    fun finishDaily(day: Long, score: Int) {
        quizDays = quizDays + day
        p.edit()
            .putStringSet("quizDays", quizDays.map { it.toString() }.toSet())
            .putInt("daily_$day", maxOf(score, dailyScore(day) ?: 0))
            .apply()
    }

    fun toggleFavorite(termId: String) {
        favorites = if (termId in favorites) favorites - termId else favorites + termId
        p.edit().putStringSet("fav", favorites).apply()
    }
}
