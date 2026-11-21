package com.hayoonjae.sciencetoday

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RefTest {
    private val ref = RefData.parse(
        File("src/main/assets/elements.json").readText(),
        File("src/main/assets/tables.json").readText(),
    )

    @Test fun elementsComplete() {
        assertEquals((1..118).toList(), ref.elements.map { it.z })
        // 칸이 겹치지 않고 18열 안에 들어간다.
        assertEquals(118, ref.elements.map { it.row to it.col }.toSet().size)
        assertTrue(ref.elements.all { it.col in 1..18 && it.row in 0..9 && it.row != 7 })
        val na = ref.byZ.getValue(11)
        assertEquals("Na", na.sym)
        assertEquals("나트륨 (소듐)", na.name)
        assertEquals(1, ref.byZ.getValue(1).group)
        assertEquals(18, ref.byZ.getValue(118).col)
        assertTrue(ref.elements.all { it.ko.isNotBlank() && it.kind.isNotBlank() })
    }

    @Test fun elementSearch() {
        assertEquals(26, ref.searchElements("철").first().z)
        assertEquals(79, ref.searchElements("au").first().z)
        assertTrue(ref.searchElements("소듐").any { it.z == 11 })
        assertTrue(ref.searchElements("ㅅㅅ").any { it.z == 1 })
        assertEquals(6, ref.searchElements("6").first().z)
    }

    @Test fun tablesComplete() {
        assertEquals(18, ref.tables.size)
        assertTrue(ref.tables.sumOf { it.entries.size } > 1200)
        for (t in ref.tables) {
            assertEquals(t.id, t.columns.size, t.numeric.size)
            for (e in t.entries) assertEquals("${t.id} ${e.en}", t.columns.size, e.cells.size)
            assertTrue(t.id, t.source.url.startsWith("https://"))
        }
        val water = ref.tableById.getValue("density").entries.first { it.en == "Water (fresh)" }
        assertEquals(listOf("1000", "같음"), water.cells.take(2))
        val cork = ref.tableById.getValue("density").entries.first { it.en == "Cork" }
        assertEquals("뜬다", cork.cells[1])
    }

    @Test fun numbers() {
        assertEquals(1.59e-8, Num.first("1.59×10^−8")!!, 1e-20)
        assertEquals(7.49e-6, Num.first("7.49 × 10^−6")!!, 1e-18)
        assertEquals(1e23, Num.first("10^23 to 10^25")!!, 1e10)
        assertEquals(0.17, Num.first("0.170–0.200")!!, 1e-9)
        assertEquals(-4.101, Num.first("−4.101")!!, 1e-9)
        assertEquals(10614.0, Num.first("10614±79")!!, 1e-9)
        assertNull(Num.first("1 (by definition)")?.takeIf { it != 1.0 })
        assertNull(Num.first("Anisotropic"))
        assertEquals("1.59×10⁻⁸", Num.pretty("1.59×10^−8"))
        assertEquals("mol⁻¹", Num.pretty("mol^-1"))
        assertEquals("J T⁻¹", Num.pretty("J T^-1"))
        assertEquals("273.15 K (0.0 °C)", Num.kelvin("273.15"))
        assertEquals("[Ar]4s² 3d⁶", Num.config("[Ar]4s2 3d6"))
        assertEquals("[Xe]6s² 4f¹⁴ 5d¹⁰", Num.config("[Xe]6s2 4f14 5d10"))
    }

    @Test fun newTables() {
        val saturn = ref.tableById.getValue("planets").entries.first { it.en == "Saturn" }
        assertEquals("687", saturn.cells[2]) // 밀도가 물(1000)보다 작다
        val si = ref.tableById.getValue("si_prefix").entries
        assertEquals(24, si.size)
        assertEquals(1e-9, Num.first(si.first { it.en == "nano" }.cells[1])!!, 1e-20)
    }

    @Test fun constants() {
        val c = ref.tableById.getValue("constants").entries
        assertEquals("진공에서 빛의 속도", c.first().ko)
        assertEquals("299 792 458", c.first().cells[0])
        assertTrue(c.size > 300)
    }
}
