package com.hayoonjae.sciencetoday

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ln

private val KIND_COLOR = mapOf(
    "알칼리 금속" to Color(0xFFFF5A6E),
    "알칼리 토금속" to Color(0xFFFF8A4C),
    "전이 금속" to Color(0xFFF5A300),
    "전이후 금속" to Color(0xFF22C55E),
    "준금속" to Color(0xFF14B8A6),
    "비금속" to Color(0xFF3DA9FF),
    "할로젠" to Color(0xFF5B5CFF),
    "비활성 기체" to Color(0xFF9B5CFF),
    "란타넘족" to Color(0xFFFF4D9D),
    "악티늄족" to Color(0xFFC850C0),
)

fun kindColor(kind: String) = KIND_COLOR[kind] ?: Color(0xFF9CA3AF)

/** 주기율표를 무엇으로 칠할지. */
private enum class Paint(val label: String, val log: Boolean, val pick: (Element) -> String) {
    KIND("분류", false, { "" }),
    DENSITY("밀도", false, { it.density }),
    MP("녹는점", false, { it.mp }),
    CHI("전기음성도", false, { it.chi }),
    RADIUS("원자 반지름", false, { it.radius }),
    IE("이온화 에너지", false, { it.ie }),
}

@Composable
fun RefHomeScreen(ref: RefData, nav: Nav) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Gap,
    ) {
        item { BigTitle("과학 자료") }
        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(BrandBrush)
                    .clickable { nav.periodic() }.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("주기율표", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text("원소 ${ref.elements.size}개 · 원자량, 전자 배치, 녹는점, 밀도, 이름 유래까지", color = Color.White.copy(alpha = 0.92f), fontSize = 14.sp)
            }
        }
        item {
            val n = Law.get(androidx.compose.ui.platform.LocalContext.current).size
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(BrandBrush)
                    .clickable { nav.laws() }.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("법칙·원리", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text("옴, 뉴턴, 멘델, 케플러… 법칙·원리 ${n}개를 식과 함께", color = Color.White.copy(alpha = 0.92f), fontSize = 14.sp)
            }
        }
        item {
            val total = ref.tables.sumOf { it.entries.size }
            SectionTitle("데이터 표 ${ref.tables.size}개 · ${"%,d".format(total)}줄")
        }
        items(ref.tables, key = { it.id }) { t ->
            Card(onClick = { nav.table(t.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t.title, color = Palette.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(t.desc, color = Palette.dim, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("${t.entries.size}", color = Palette.purple, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PeriodicScreen(ref: RefData, nav: Nav) {
    var paint by rememberSaveable { mutableStateOf(Paint.KIND) }
    var query by rememberSaveable { mutableStateOf("") }
    val range = remember(paint) {
        val vs = ref.elements.mapNotNull { Num.first(paint.pick(it)) }.filter { !paint.log || it > 0 }
            .map { if (paint.log) ln(it) else it }
        (vs.minOrNull() ?: 0.0) to (vs.maxOrNull() ?: 1.0)
    }

    fun colorOf(e: Element): Color {
        if (paint == Paint.KIND) return kindColor(e.kind)
        val v = Num.first(paint.pick(e))?.let { if (paint.log) (if (it > 0) ln(it) else null) else it } ?: return Color(0xFFD1D5DB)
        val t = ((v - range.first) / (range.second - range.first).coerceAtLeast(1e-9)).toFloat()
        return if (t < 0.5f) lerp(Palette.sky, Palette.purple, t * 2) else lerp(Palette.purple, Palette.pink, (t - 0.5f) * 2)
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("주기율표", onBack = nav::back)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp),
            verticalArrangement = Gap,
        ) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Paint.entries.forEach { p ->
                    val on = p == paint
                    Text(
                        p.label,
                        Modifier.clip(RoundedCornerShape(50))
                            .then(if (on) Modifier.background(BrandBrush) else Modifier.background(Palette.card).border(1.dp, Palette.line, RoundedCornerShape(50)))
                            .clickable { paint = p }.padding(horizontal = 14.dp, vertical = 8.dp),
                        color = if (on) Color.White else Palette.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    )
                }
            }

            // 18칸이라 폰 화면보다 넓다. 옆으로 밀어서 본다.
            Box(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    for (row in 0..9) {
                        if (row == 7) { Spacer(Modifier.height(8.dp)); continue }
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            for (col in 1..18) {
                                val e = ref.elements.firstOrNull { it.row == row && it.col == col }
                                when {
                                    e != null -> ElementCell(e, colorOf(e)) { nav.element(e.z) }
                                    row == 5 && col == 3 -> GapCell("57–71")
                                    row == 6 && col == 3 -> GapCell("89–103")
                                    else -> Spacer(Modifier.size(CELL))
                                }
                            }
                        }
                    }
                }
            }

            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Gap) {
                if (paint == Paint.KIND) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        KIND_COLOR.forEach { (k, c) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(c))
                                Spacer(Modifier.width(5.dp))
                                Text(k, color = Palette.ink, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    Column {
                        Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Brush.horizontalGradient(listOf(Palette.sky, Palette.purple, Palette.pink))))
                        Row {
                            Text("낮음", color = Palette.dim, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Text(if (paint.log) "높음 (로그 눈금)" else "높음", color = Palette.dim, fontSize = 12.sp)
                        }
                        Text("회색 칸은 값이 알려지지 않은 원소", color = Palette.dim, fontSize = 12.sp)
                    }
                }

                OutlinedTextField(
                    value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text("원소 찾기 (이름, 기호, 번호, 초성)", color = Palette.dim) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = Palette.dim) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, "지우기", tint = Palette.dim) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Palette.card, unfocusedContainerColor = Palette.card,
                        focusedBorderColor = Palette.purple, unfocusedBorderColor = Palette.line,
                    ),
                )
                if (query.isNotBlank()) {
                    ref.searchElements(query).take(30).forEach { e ->
                        Card(onClick = { nav.element(e.z) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ElementCell(e, kindColor(e.kind)) { nav.element(e.z) }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(e.name, color = Palette.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("${e.en} · ${e.kind}", color = Palette.dim, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
                SourceList(ref.elementSources)
            }
        }
    }
}

private val CELL = 46.dp

@Composable
private fun ElementCell(e: Element, color: Color, onClick: () -> Unit) {
    Column(
        Modifier.size(CELL).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.18f))
            .border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(8.dp)).clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(e.z.toString(), color = Palette.dim, fontSize = 8.sp, lineHeight = 9.sp, modifier = Modifier.fillMaxWidth())
        Text(e.sym, color = Palette.ink, fontSize = 15.sp, lineHeight = 16.sp, fontWeight = FontWeight.ExtraBold)
        Text(e.ko, color = Palette.ink, fontSize = 7.sp, lineHeight = 8.sp, maxLines = 1, overflow = TextOverflow.Clip)
    }
}

@Composable
private fun GapCell(text: String) {
    Box(
        Modifier.size(CELL).clip(RoundedCornerShape(8.dp)).border(1.dp, Palette.line, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Palette.dim, fontSize = 8.sp, textAlign = TextAlign.Center) }
}

@Composable
fun ElementScreen(e: Element, ref: RefData, nav: Nav) {
    val c = kindColor(e.kind)
    Column(Modifier.fillMaxSize()) {
        TopBar("원소", onBack = nav::back) {
            IconButton(onClick = { nav.replaceElement(e.z - 1) }, enabled = e.z > 1) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "이전 원소", tint = if (e.z > 1) Palette.ink else Palette.line)
            }
            IconButton(onClick = { nav.replaceElement(e.z + 1) }, enabled = e.z < ref.elements.size) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "다음 원소", tint = if (e.z < ref.elements.size) Palette.ink else Palette.line)
            }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Gap,
        ) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(c, lerp(c, Palette.purple, 0.45f)))).padding(22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${e.z}번 · ${e.kind}", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(e.name, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text(e.en, color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp)
                }
                Text(e.sym, color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.ExtraBold)
            }

            Card {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Prop("원자량", e.mass, "u")
                    Prop("족 · 주기", "${e.group?.let { "${it}족" } ?: "f 블록"} · ${e.period}주기", "")
                    Prop("상온 상태", e.state, "")
                    Prop("전자 배치", Num.config(e.config), "")
                    Prop("산화수", e.ox, "")
                    Prop("전기음성도", e.chi, "(폴링)")
                    Prop("원자 반지름", e.radius, "pm")
                    Prop("이온화 에너지", e.ie, "eV")
                    Prop("전자 친화도", e.ea, "eV")
                    Prop("녹는점", if (e.mp.isEmpty()) "" else Num.kelvin(e.mp), "")
                    Prop("끓는점", if (e.bp.isEmpty()) "" else Num.kelvin(e.bp), "")
                    Prop("밀도", e.density, "g/cm³")
                    Num.first(e.density)?.let { d ->
                        if (e.state.startsWith("고체") || e.state.startsWith("액체")) {
                            Prop("물에서", if (d < 1.0) "뜬다" else "가라앉는다", "(밀도로 계산)")
                        }
                    }
                    Prop("비열", e.heat, "J/g·K")
                    Prop("지각 속 함량", e.crust, "mg/kg")
                    Prop("발견", e.year.let { if (it == "Ancient") "고대부터 알려짐" else it }, "")
                }
            }
            if (e.origin.isNotEmpty()) {
                SectionTitle("이름 유래")
                Card { Text(e.origin, color = Palette.ink, fontSize = 15.sp, lineHeight = 22.sp) }
            }
            Text("빈 칸은 출처에 값이 없는 항목이다. 지어내서 채우지 않았다.", color = Palette.dim, fontSize = 12.sp)
            SourceList(ref.elementSources)
        }
    }
}

@Composable
private fun Prop(label: String, value: String, unit: String) {
    Row {
        Text(label, color = Palette.dim, fontSize = 14.sp, modifier = Modifier.width(110.dp))
        Text(
            if (value.isBlank()) "-" else Num.pretty(value) + if (unit.isNotEmpty()) " $unit" else "",
            color = if (value.isBlank()) Palette.line else Palette.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TableScreen(t: DataTable, nav: Nav) {
    var query by rememberSaveable { mutableStateOf("") }
    // -1: 원래 순서, 0..: 그 열로 정렬
    var sortCol by rememberSaveable { mutableIntStateOf(-1) }
    var desc by rememberSaveable { mutableStateOf(true) }

    val rows: List<TableRow> = remember(t, query, sortCol, desc) {
        val filtered = t.rows.filter { r ->
            r is TableSection || (r is TableEntry && (Hangul.matches(r.ko, query) || Hangul.matches(r.en, query)))
        }
        if (sortCol < 0 && query.isBlank()) filtered
        else {
            val entries = filtered.filterIsInstance<TableEntry>()
            if (sortCol < 0) entries
            else {
                val (has, none) = entries.partition { Num.first(it.cells[sortCol]) != null }
                val sorted = has.sortedBy { Num.first(it.cells[sortCol]) }
                (if (desc) sorted.reversed() else sorted) + none
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(t.title, onBack = nav::back)
        LazyColumn(
            Modifier.fillMaxSize().navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Card { Text(t.desc, color = Palette.ink, fontSize = 14.sp, lineHeight = 21.sp) } }
            item {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text("물질 찾기 (한글, 영어, 초성)", color = Palette.dim) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = Palette.dim) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, "지우기", tint = Palette.dim) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Palette.card, unfocusedContainerColor = Palette.card,
                        focusedBorderColor = Palette.purple, unfocusedBorderColor = Palette.line,
                    ),
                )
            }
            if (t.numeric.any { it }) item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SortPill("원래 순서", sortCol < 0) { sortCol = -1 }
                    t.columns.forEachIndexed { i, name ->
                        if (t.numeric[i]) {
                            val on = sortCol == i
                            SortPill(if (on) "$name ${if (desc) "큰 순" else "작은 순"}" else name, on) {
                                if (on) desc = !desc else { sortCol = i; desc = true }
                            }
                        }
                    }
                }
            }
            item { Text("${rows.count { it is TableEntry }}개", color = Palette.dim, fontSize = 13.sp) }
            items(rows.size) { i ->
                when (val r = rows[i]) {
                    is TableSection -> Text(r.title, color = Palette.purple, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 6.dp))
                    is TableEntry -> Card {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(r.ko, color = Palette.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            if (r.en != r.ko) Text(r.en, color = Palette.dim, fontSize = 12.sp)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                r.cells.forEachIndexed { ci, v ->
                                    if (v.isNotBlank()) Column {
                                        Text(t.columns[ci], color = Palette.dim, fontSize = 11.sp)
                                        Text(Num.pretty(v), color = if (ci == sortCol) Palette.purple else Palette.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { SourceList(listOf(t.source)) }
        }
    }
}

@Composable
private fun SortPill(text: String, on: Boolean, onClick: () -> Unit) {
    Text(
        text,
        Modifier.clip(RoundedCornerShape(50))
            .then(if (on) Modifier.background(BrandBrush) else Modifier.background(Palette.card).border(1.dp, Palette.line, RoundedCornerShape(50)))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
        color = if (on) Color.White else Palette.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun SourceList(sources: List<RefSource>) {
    val uri = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionTitle("출처")
        sources.forEach { s ->
            Card(onClick = if (s.url.isNotEmpty()) ({ runCatching { uri.openUri(s.url) } }) else null) {
                Column {
                    Text(s.title, color = if (s.url.isNotEmpty()) Palette.purple else Palette.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(listOf(s.publisher, s.license).filter { it.isNotEmpty() }.joinToString(" · "), color = Palette.dim, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun LawsScreen(laws: List<Law>, nav: Nav) {
    var query by rememberSaveable { mutableStateOf("") }
    var field by rememberSaveable { mutableStateOf<Field?>(null) }
    val uri = LocalUriHandler.current
    val list = laws.filter { l ->
        (field == null || l.field == field) &&
            (Hangul.matches(l.ko, query) || Hangul.matches(l.en, query) || Hangul.matches(l.formula, query) || Hangul.matches(l.statement, query))
    }
    Column(Modifier.fillMaxSize()) {
        TopBar("법칙·원리", onBack = nav::back)
        LazyColumn(
            Modifier.fillMaxSize().navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Gap,
        ) {
            item {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text("법칙 찾기 (이름, 영어, 초성)", color = Palette.dim) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = Palette.dim) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, "지우기", tint = Palette.dim) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Palette.card, unfocusedContainerColor = Palette.card,
                        focusedBorderColor = Palette.purple, unfocusedBorderColor = Palette.line,
                    ),
                )
            }
            item { FieldFilter(field) { field = it } }
            item { Text("${list.size}개", color = Palette.dim, fontSize = 13.sp) }
            if (list.isEmpty()) item { Text("찾는 법칙이 없어요.", color = Palette.dim, fontSize = 14.sp) }
            items(list, key = { it.id }) { l ->
                Card {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FieldChip(l.field, onColor = false, extra = l.who.ifEmpty { null })
                        Text(l.ko, color = Palette.ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text(l.en, color = Palette.dim, fontSize = 12.sp)
                        Text(
                            l.formula, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(l.field.brush()).padding(12.dp),
                        )
                        Text(l.statement, color = Palette.ink, fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold)
                        Text(l.why, color = Palette.dim, fontSize = 14.sp, lineHeight = 21.sp)
                        Text(
                            "출처: ${l.source.title}", color = Palette.purple, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { runCatching { uri.openUri(l.source.url) } },
                        )
                    }
                }
            }
            item { Text("식과 설명은 교과서 수준의 요약이고, 위 위키백과 문서로 자세한 내용을 확인할 수 있다.", color = Palette.dim, fontSize = 12.sp) }
        }
    }
}
