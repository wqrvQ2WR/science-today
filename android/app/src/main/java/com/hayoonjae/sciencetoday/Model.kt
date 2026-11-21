package com.hayoonjae.sciencetoday

import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

enum class Field(val key: String, val label: String) {
    CHEM("chem", "화학"),
    PHYS("phys", "물리"),
    BIO("bio", "생물"),
    EARTH("earth", "지구"),
    SPACE("space", "우주"),
    BODY("body", "인체");

    companion object {
        fun of(key: String) = entries.first { it.key == key }
    }
}

data class Source(val title: String, val publisher: String, val year: Int, val url: String)

sealed interface Question {
    val q: String
    val explain: String
}

/** OX 문제. 고른 값은 0 = O, 1 = X. */
data class OxQ(override val q: String, val answer: Boolean, override val explain: String) : Question
data class ChoiceQ(override val q: String, val options: List<String>, val answer: Int, override val explain: String) : Question

fun Question.isCorrect(pick: Int): Boolean = when (this) {
    is OxQ -> (pick == 0) == answer
    is ChoiceQ -> pick == answer
}

/** 정답 보기 번호. OX는 0 = O, 1 = X. */
fun Question.correctPick(): Int = when (this) {
    is OxQ -> if (answer) 0 else 1
    is ChoiceQ -> answer
}

data class Fact(
    val id: String,
    val field: Field,
    val brag: Boolean,
    val title: String,
    val short: String,
    val detail: String,
    val terms: List<String>,
    val safety: String?,
    val sources: List<Source>,
    val quiz: List<Question>,
)

data class Term(
    val id: String,
    val word: String,
    val en: String,
    val one: String,
    val detail: String,
    val field: Field,
)

/** 퀴즈 한 문제. key는 "상식id#문제번호"이고 오답노트에 이 값으로 저장한다. */
data class QItem(val fact: Fact, val index: Int) {
    val q: Question get() = fact.quiz[index]
    val key: String get() = "${fact.id}#$index"
}

class Library(val facts: List<Fact>, val terms: List<Term>) {
    val factById = facts.associateBy { it.id }
    val termById = terms.associateBy { it.id }

    /** 위젯과 오늘 화면에 나오는 순서. 분야가 매일 바뀌도록 섞어 둔다. */
    val daily: List<Fact> = Daily.order(facts)

    fun factOfDay(day: Long): Fact = daily[Daily.index(day, daily.size)]

    fun factsUsing(termId: String) = facts.filter { termId in it.terms }

    fun searchTerms(query: String): List<Term> =
        terms.filter { Hangul.matches(it.word, query) || Hangul.matches(it.en, query) || Hangul.matches(it.one, query) }

    fun dailyQuiz(day: Long): List<QItem> = factOfDay(day).let { f -> f.quiz.indices.map { QItem(f, it) } }

    fun factQuiz(f: Fact): List<QItem> = f.quiz.indices.map { QItem(f, it) }

    fun fieldQuiz(field: Field, rnd: Random, n: Int = 10): List<QItem> =
        facts.filter { it.field == field }.flatMap(::factQuiz).shuffled(rnd).take(n)

    fun randomQuiz(rnd: Random, n: Int = 10): List<QItem> = facts.flatMap(::factQuiz).shuffled(rnd).take(n)

    fun itemOf(key: String): QItem? {
        val (id, i) = key.split('#').takeIf { it.size == 2 } ?: return null
        val f = factById[id] ?: return null
        val idx = i.toIntOrNull()?.takeIf { it in f.quiz.indices } ?: return null
        return QItem(f, idx)
    }

    companion object {
        fun parse(json: String): Library {
            val root = JSONObject(json)
            val facts = root.getJSONArray("facts").objects().map { o ->
                Fact(
                    id = o.getString("id"),
                    field = Field.of(o.getString("field")),
                    brag = o.getString("kind") == "brag",
                    title = o.getString("title"),
                    short = o.getString("short"),
                    detail = o.getString("detail"),
                    terms = o.getJSONArray("terms").strings(),
                    safety = if (o.isNull("safety")) null else o.getString("safety"),
                    sources = o.getJSONArray("sources").objects().map {
                        Source(it.getString("title"), it.getString("publisher"), it.optInt("year", 0), it.optString("url", ""))
                    },
                    quiz = o.getJSONArray("quiz").objects().map { q ->
                        if (q.getString("type") == "ox") OxQ(q.getString("q"), q.getBoolean("answer"), q.getString("explain"))
                        else ChoiceQ(q.getString("q"), q.getJSONArray("options").strings(), q.getInt("answer"), q.getString("explain"))
                    },
                )
            }
            val terms = root.getJSONArray("terms").objects().map {
                Term(it.getString("id"), it.getString("word"), it.getString("en"), it.getString("one"), it.getString("detail"), Field.of(it.getString("field")))
            }
            return Library(facts, terms)
        }

        private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
        private fun JSONArray.strings() = (0 until length()).map { getString(it) }
    }
}

object Daily {
    /** 첫 상식이 나오는 날(2026-11-05). 이 날부터 순서대로 돈다. */
    const val START_DAY = 20762L

    /** 분야별로 id 순서대로 줄 세운 뒤, 화학-물리-생물-지구-우주-인체 순으로 하나씩 번갈아 꺼낸다. */
    fun order(facts: List<Fact>): List<Fact> {
        val queues = Field.entries.map { f -> ArrayDeque(facts.filter { it.field == f }.sortedBy { it.id }) }
        val out = ArrayList<Fact>(facts.size)
        while (queues.any { it.isNotEmpty() }) {
            for (q in queues) q.removeFirstOrNull()?.let(out::add)
        }
        return out
    }

    fun index(day: Long, size: Int): Int = Math.floorMod(day - START_DAY, size.toLong()).toInt()
}

object Hangul {
    private const val CHO = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"

    /** 완성형 한글을 초성으로 바꾼다. 한글이 아닌 글자는 그대로 둔다. */
    fun choseong(s: String): String =
        s.map { c -> if (c in '가'..'힣') CHO[(c - '가') / 588] else c }.joinToString("")

    private fun isCho(c: Char) = c in CHO

    /** 검색어가 그대로 들어 있거나, 검색어가 전부 초성이면 초성끼리 비교한다. 띄어쓰기와 대소문자는 무시. */
    fun matches(text: String, query: String): Boolean {
        val q = query.replace(" ", "").lowercase()
        if (q.isEmpty()) return true
        val t = text.replace(" ", "").lowercase()
        if (t.contains(q)) return true
        return q.all(::isCho) && choseong(t).contains(q)
    }
}

object Streak {
    /** 오늘(또는 아직 안 풀었으면 어제)부터 거꾸로 하루도 안 빠지고 이어진 날 수. */
    fun current(days: Set<Long>, today: Long): Int {
        var d = if (today in days) today else today - 1
        var n = 0
        while (d in days) { n++; d-- }
        return n
    }

    fun best(days: Set<Long>): Int {
        var best = 0
        for (d in days) {
            if (d - 1 in days) continue
            var n = 0
            while (d + n in days) n++
            best = maxOf(best, n)
        }
        return best
    }
}

/** 본문에서 용어가 처음 나오는 자리. 겹치면 먼저 찾은 쪽을 쓴다. (시작, 끝, 용어id) */
fun linkRanges(text: String, words: List<Pair<String, String>>): List<Triple<Int, Int, String>> {
    val out = ArrayList<Triple<Int, Int, String>>()
    for ((id, word) in words) {
        if (word.isEmpty()) continue
        var from = 0
        while (true) {
            val at = text.indexOf(word, from)
            if (at < 0) break
            val end = at + word.length
            if (out.none { at < it.second && end > it.first }) {
                out += Triple(at, end, id)
                break
            }
            from = at + 1
        }
    }
    return out.sortedBy { it.first }
}
