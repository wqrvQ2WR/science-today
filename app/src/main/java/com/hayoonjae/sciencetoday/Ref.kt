package com.hayoonjae.sciencetoday

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.pow

/** 주기율표 한 칸. 값은 PubChem·위키백과 원문 그대로의 문자열이고, 없으면 빈 문자열. */
data class Element(
    val z: Int,
    val sym: String,
    val ko: String,
    val alt: String,
    val en: String,
    val mass: String,
    val group: Int?,
    val period: Int,
    val row: Int,
    val col: Int,
    val kind: String,
    val config: String,
    val chi: String,
    val radius: String,
    val ie: String,
    val ea: String,
    val ox: String,
    val state: String,
    val mp: String,
    val bp: String,
    val density: String,
    val year: String,
    val heat: String,
    val crust: String,
    val origin: String,
) {
    val name get() = if (alt.isEmpty()) ko else "$ko ($alt)"
}

data class RefSource(val title: String, val publisher: String, val url: String, val license: String)

sealed interface TableRow
data class TableSection(val title: String) : TableRow
data class TableEntry(val ko: String, val en: String, val cells: List<String>) : TableRow

data class DataTable(
    val id: String,
    val title: String,
    val desc: String,
    val columns: List<String>,
    val numeric: List<Boolean>,
    val rows: List<TableRow>,
    val source: RefSource,
) {
    val entries get() = rows.filterIsInstance<TableEntry>()
}

/** 법칙·원리 한 개. 식과 발견자는 출처에 있는 것만 적고 없으면 빈 문자열. */
data class Law(
    val id: String, val ko: String, val en: String, val field: Field,
    val formula: String, val statement: String, val why: String, val who: String, val source: RefSource,
) {
    companion object {
        fun parse(json: String): List<Law> {
            val a = JSONObject(json).getJSONArray("laws")
            return (0 until a.length()).map { a.getJSONObject(it) }.map { o ->
                val s = o.getJSONObject("source")
                Law(
                    o.getString("id"), o.getString("ko"), o.getString("en"), Field.of(o.getString("field")),
                    o.getString("formula"), o.getString("statement"), o.getString("why"), o.optString("who"),
                    RefSource(s.optString("title"), s.optString("publisher"), s.optString("url"), s.optString("license")),
                )
            }
        }
    }
}

class RefData(val elements: List<Element>, val elementSources: List<RefSource>, val tables: List<DataTable>) {
    val byZ = elements.associateBy { it.z }
    val tableById = tables.associateBy { it.id }

    fun searchElements(q: String): List<Element> = elements.filter {
        Hangul.matches(it.ko, q) || Hangul.matches(it.alt, q) || Hangul.matches(it.en, q) ||
            it.sym.equals(q.trim(), ignoreCase = true) || it.z.toString() == q.trim()
    }

    companion object {
        fun parse(elementsJson: String, tablesJson: String): RefData {
            val e = JSONObject(elementsJson)
            val elements = e.getJSONArray("elements").objs().map { o ->
                fun s(k: String) = if (o.isNull(k)) "" else o.get(k).toString()
                Element(
                    z = o.getInt("z"), sym = s("sym"), ko = s("ko"), alt = s("alt"), en = s("en"), mass = s("mass"),
                    group = if (o.isNull("group")) null else o.getInt("group"), period = o.getInt("period"),
                    row = o.getInt("row"), col = o.getInt("col"), kind = s("kind"), config = s("config"), chi = s("chi"),
                    radius = s("radius"), ie = s("ie"), ea = s("ea"), ox = s("ox"), state = s("state"), mp = s("mp"),
                    bp = s("bp"), density = s("density"), year = s("year"), heat = s("heat"), crust = s("crust"), origin = s("origin"),
                )
            }
            val tables = JSONObject(tablesJson).getJSONArray("tables").objs().map { t ->
                DataTable(
                    id = t.getString("id"),
                    title = t.getString("title"),
                    desc = t.getString("desc"),
                    columns = t.getJSONArray("columns").strs(),
                    numeric = t.getJSONArray("numeric").let { a -> (0 until a.length()).map { a.getBoolean(it) } },
                    rows = t.getJSONArray("rows").objs().map { r ->
                        if (r.has("section")) TableSection(r.getString("section"))
                        else TableEntry(r.getString("ko"), r.getString("en"), r.getJSONArray("cells").strs())
                    },
                    source = t.getJSONObject("source").toSource(),
                )
            }
            return RefData(elements, e.getJSONArray("sources").objs().map { it.toSource() }, tables)
        }

        private fun JSONObject.toSource() = RefSource(optString("title"), optString("publisher"), optString("url"), optString("license"))
        private fun JSONArray.objs() = (0 until length()).map { getJSONObject(it) }
        private fun JSONArray.strs() = (0 until length()).map { getString(it) }
    }
}

object Num {
    private val SUP = mapOf(
        '−' to '⁻', '-' to '⁻', '+' to '⁺', '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
    )

    /** "1.59×10^−8" -> "1.59×10⁻⁸", "mol^-1" -> "mol⁻¹". 위첨자로 못 바꾸는 글자가 나오면 거기서 멈춘다. */
    fun pretty(s: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '^') {
                i++
                while (i < s.length && SUP.containsKey(s[i])) { out.append(SUP.getValue(s[i])); i++ }
                continue
            }
            out.append(c)
            i++
        }
        return out.toString()
    }

    /**
     * 정렬용으로 맨 앞 숫자만 읽는다. "0.170–0.200" -> 0.17, "7.49 × 10^−6" -> 7.49e-6,
     * "10^23 to 10^25" -> 1e23, "−4.101" -> -4.101. 숫자가 없으면 null.
     */
    fun first(s: String): Double? {
        val t = s.replace(",", "").replace('−', '-').replace("~", "").replace("≈", "").replace(">", "").replace("<", "").trim()
        Regex("""^(-?\d+(?:\.\d+)?)?\s*(?:[×x]\s*)?10\^(-?\d+)""").find(t)?.let { m ->
            val mant = m.groupValues[1].takeIf { it.isNotEmpty() }?.toDouble() ?: 1.0
            return mant * 10.0.pow(m.groupValues[2].toInt())
        }
        return Regex("""^-?\d+(?:\.\d+)?(?:[eE]-?\d+)?""").find(t)?.value?.toDoubleOrNull()
    }

    /** "[Ar]4s2 3d6" -> "[Ar]4s² 3d⁶". 오비탈 글자(s,p,d,f) 바로 뒤 숫자만 위첨자로 바꾼다. */
    fun config(c: String): String =
        Regex("([spdf])(\\d+)").replace(c) { m -> m.groupValues[1] + m.groupValues[2].map { SUP.getValue(it) }.joinToString("") }

    /** 켈빈 문자열 -> "1234.5 K (961.4 °C)". 숫자가 아니면 그대로. */
    fun kelvin(k: String): String {
        val v = k.toDoubleOrNull() ?: return k
        val c = v - 273.15
        return "$k K (${"%.1f".format(c)} °C)"
    }
}
