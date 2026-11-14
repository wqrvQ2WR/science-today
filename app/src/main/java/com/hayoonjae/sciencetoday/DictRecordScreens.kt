package com.hayoonjae.sciencetoday

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.Collator
import java.util.Locale

@Composable
fun DictScreen(lib: Library, progress: Progress, nav: Nav) {
    var query by rememberSaveable { mutableStateOf("") }
    var onlyFav by rememberSaveable { mutableStateOf(false) }
    val collator = remember { Collator.getInstance(Locale.KOREAN) }
    val list = lib.searchTerms(query)
        .filter { !onlyFav || it.id in progress.favorites }
        .sortedWith { a, b -> collator.compare(a.word, b.word) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { BigTitle("과학 사전") }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                placeholder = { Text("용어 검색 (초성도 돼요: ㅋㄹㅍㅂ)", color = Palette.dim) },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = Palette.dim) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, "지우기", tint = Palette.dim) }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Palette.card,
                    unfocusedContainerColor = Palette.card,
                    focusedBorderColor = Palette.purple,
                    unfocusedBorderColor = Palette.line,
                ),
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${list.size}개", color = Palette.dim, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Row(
                    Modifier.clip(RoundedCornerShape(50)).clickable { onlyFav = !onlyFav }.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Favorite, null, tint = if (onlyFav) Palette.pink else Palette.dim, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (onlyFav) "전체 보기" else "즐겨찾기만",
                        color = if (onlyFav) Palette.pink else Palette.dim, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        if (list.isEmpty()) item {
            Text(
                if (onlyFav) "즐겨찾기한 용어가 없어요. 용어 화면 오른쪽 위 하트를 눌러 추가해요." else "찾는 용어가 없어요.",
                color = Palette.dim, fontSize = 14.sp,
            )
        }
        items(list, key = { it.id }) { t ->
            Card(onClick = { nav.term(t.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(t.field.main())
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(t.word, color = Palette.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(6.dp))
                            Text(t.en, color = Palette.dim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(t.one, color = Palette.dim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (t.id in progress.favorites) Icon(Icons.Filled.Favorite, null, tint = Palette.pink)
                }
            }
        }
    }
}

@Composable
fun RecordScreen(lib: Library, progress: Progress, nav: Nav) {
    val today = Today.day()
    val streak = Streak.current(progress.quizDays, today)
    val best = Streak.best(progress.quizDays)
    val solved = progress.stats.values.sumOf { it.second }
    val ok = progress.stats.values.sumOf { it.first }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Gap,
    ) {
        item { BigTitle("내 기록") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatBox("연속", "${streak}일", Modifier.weight(1f))
                StatBox("최고 연속", "${best}일", Modifier.weight(1f))
                StatBox("정답률", if (solved == 0) "-" else "${ok * 100 / solved}%", Modifier.weight(1f))
            }
        }
        item {
            Text(
                if (progress.dailyScore(today) == null) "오늘의 퀴즈를 풀면 연속 기록이 이어져요." else "오늘의 퀴즈 완료. 내일 또 만나요.",
                color = Palette.dim, fontSize = 13.sp,
            )
        }
        item { SectionTitle("분야별 정답률") }
        item {
            Card {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Field.entries.forEach { f ->
                        val (o, a) = progress.stats.getValue(f)
                        Column {
                            Row {
                                Text(f.label, color = Palette.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text(if (a == 0) "아직 안 풂" else "$o / $a", color = Palette.dim, fontSize = 13.sp)
                            }
                            Spacer(Modifier.height(6.dp))
                            Meter(if (a == 0) 0f else o.toFloat() / a, f.brush())
                        }
                    }
                }
            }
        }
        item { SectionTitle("오답노트") }
        item {
            val items = progress.wrong.mapNotNull(lib::itemOf)
            GradientButton(
                if (items.isEmpty()) "틀린 문제가 없어요" else "틀린 문제 ${items.size}개 다시 풀기",
                enabled = items.isNotEmpty(),
            ) { nav.quiz(items, QuizMode.WRONG) }
        }
        val favs = lib.terms.filter { it.id in progress.favorites }
        if (favs.isNotEmpty()) {
            item { SectionTitle("즐겨찾기 용어") }
            items(favs, key = { it.id }) { t ->
                Card(onClick = { nav.term(t.id) }) {
                    Column {
                        Text(t.word, color = Palette.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(t.one, color = Palette.dim, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier) {
    Card(modifier) {
        Column {
            Text(label, color = Palette.dim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Palette.ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
