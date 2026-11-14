package com.hayoonjae.sciencetoday

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import kotlin.random.Random

class ModelTest {
    private val lib = Library.parse(File("src/main/assets/facts.json").readText())

    @Test fun dataIsComplete() {
        assertEquals(54, lib.facts.size)
        for (f in lib.facts) {
            assertEquals(f.id, 3, f.quiz.size)
            assertTrue(f.id, f.sources.isNotEmpty())
            assertTrue(f.id, f.title.length <= 30)
            for (t in f.terms) assertNotNull("${f.id} -> $t", lib.termById[t])
            for (q in f.quiz) if (q is ChoiceQ) {
                assertEquals(f.id, 4, q.options.size)
                assertTrue(f.id, q.answer in 0..3)
            }
        }
        assertEquals(lib.terms.size, lib.termById.size)
    }

    @Test fun startDayIsNov5() {
        assertEquals(LocalDate.of(2026, 11, 5).toEpochDay(), Daily.START_DAY)
        assertEquals(0, Daily.index(Daily.START_DAY, 30))
        assertEquals(29, Daily.index(Daily.START_DAY - 1, 30))
        assertEquals(0, Daily.index(Daily.START_DAY + 30, 30))
    }

    @Test fun dailyOrderAlternatesFields() {
        val firstSix = lib.daily.take(6).map { it.field }
        assertEquals(Field.entries.toList(), firstSix)
        assertEquals(lib.facts.toSet(), lib.daily.toSet())
        // 한 바퀴 도는 동안 같은 상식은 한 번만 나온다.
        val n = lib.daily.size
        val cycle = (0 until n).map { lib.factOfDay(Daily.START_DAY + it).id }
        assertEquals(n, cycle.toSet().size)
    }

    @Test fun choseongSearch() {
        assertEquals("ㅋㄹㅍㅂ", Hangul.choseong("쿨롱폭발"))
        assertTrue(Hangul.matches("쿨롱 폭발", "ㅋㄹㅍㅂ"))
        assertTrue(Hangul.matches("쿨롱 폭발", "쿨롱폭"))
        assertTrue(Hangul.matches("Coulomb explosion", "coulomb"))
        assertFalse(Hangul.matches("쿨롱 폭발", "ㅅㅎ"))
        assertTrue(lib.searchTerms("ㅋㄹㅍㅂ").any { it.id == "coulomb-explosion" })
        assertTrue(lib.searchTerms("광년").any { it.id == "light-year" })
    }

    @Test fun streak() {
        val t = 100L
        assertEquals(0, Streak.current(emptySet(), t))
        assertEquals(3, Streak.current(setOf(98, 99, 100), t))
        assertEquals(2, Streak.current(setOf(98, 99), t)) // 오늘 아직 안 풀어도 끊기지 않음
        assertEquals(0, Streak.current(setOf(97, 98), t))
        assertEquals(4, Streak.best(setOf(1, 2, 3, 4, 10, 11)))
    }

    @Test fun answers() {
        assertTrue(OxQ("", true, "").isCorrect(0))
        assertFalse(OxQ("", true, "").isCorrect(1))
        assertTrue(OxQ("", false, "").isCorrect(1))
        assertEquals(1, OxQ("", false, "").correctPick())
        assertTrue(ChoiceQ("", listOf("a", "b", "c", "d"), 2, "").isCorrect(2))
    }

    @Test fun quizBuilders() {
        val q = lib.fieldQuiz(Field.SPACE, Random(1))
        assertEquals(10, q.size)
        assertTrue(q.all { it.fact.field == Field.SPACE })
        assertEquals(10, q.map { it.key }.toSet().size)
        assertEquals(3, lib.dailyQuiz(Daily.START_DAY).size)
        val item = q.first()
        assertEquals(item, lib.itemOf(item.key))
        assertEquals(null, lib.itemOf("nope#1"))
        assertEquals(null, lib.itemOf("chem-001#9"))
    }

    @Test fun termLinks() {
        val r = linkRanges("자전축 기울기와 자전", listOf("a" to "자전", "b" to "자전축 기울기"))
        // "자전"이 먼저 앞자리를 잡으면 겹치는 "자전축 기울기"는 못 들어간다.
        assertEquals(listOf(Triple(0, 2, "a")), r)
        val r2 = linkRanges("물은 얼음이 된다. 얼음은", listOf("x" to "얼음"))
        assertEquals(listOf(Triple(3, 5, "x")), r2)
        // 실제 데이터: 모든 링크가 본문 범위 안에 있고 서로 겹치지 않는다.
        for (f in lib.facts) {
            val ranges = linkRanges(f.detail, f.terms.map { it to lib.termById.getValue(it).word })
            ranges.zipWithNext().forEach { (a, b) -> assertTrue(f.id, a.second <= b.first) }
        }
    }
}
