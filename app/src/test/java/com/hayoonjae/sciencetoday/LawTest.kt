package com.hayoonjae.sciencetoday

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LawTest {
    private val laws = Law.parse(File("src/main/assets/laws.json").readText())

    @Test fun `67개이고 id가 겹치지 않는다`() {
        assertEquals(67, laws.size)
        assertEquals(laws.size, laws.map { it.id }.toSet().size)
    }

    @Test fun `모든 항목에 식 설명 출처가 있다`() {
        for (l in laws) {
            assertTrue(l.id, l.formula.isNotBlank() && l.statement.isNotBlank() && l.why.isNotBlank())
            assertTrue(l.id, l.source.url.startsWith("https://"))
            assertTrue(l.id, '%' !in l.en)
        }
    }

    @Test fun `초성으로 찾는다`() {
        assertTrue(laws.any { Hangul.matches(it.ko, "ㅇㅇ") })
        assertTrue(laws.filter { Hangul.matches(it.ko, "옴") }.any { it.id == "ohm" })
    }
}
