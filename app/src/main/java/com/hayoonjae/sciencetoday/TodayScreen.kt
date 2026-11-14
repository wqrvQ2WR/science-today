package com.hayoonjae.sciencetoday

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TodayScreen(lib: Library, progress: Progress, nav: Nav) {
    val day = Today.day()
    val fact = lib.factOfDay(day)
    var filter by rememberSaveable { mutableStateOf<Field?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Gap,
    ) {
        item {
            Column {
                Text(Today.label(), color = Palette.dim, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                BigTitle("오늘의 과학")
            }
        }
        item { HeroCard(fact) { nav.fact(fact.id) } }
        item {
            val score = progress.dailyScore(day)
            Card(onClick = { nav.quiz(lib.dailyQuiz(day), QuizMode.DAILY, day) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(BrandBrush),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.CheckCircle, null, tint = Color.White) }
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("오늘의 퀴즈", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                        Text(
                            if (score == null) "오늘 상식으로 ${fact.quiz.size}문제" else "${fact.quiz.size}문제 중 ${score}개 맞힘 · 다시 풀기",
                            fontSize = 13.sp, color = Palette.dim,
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Palette.dim)
                }
            }
        }
        item { SectionTitle("모든 상식") }
        item { FieldFilter(filter) { filter = it } }
        items(lib.facts.filter { filter == null || it.field == filter }, key = { it.id }) { f ->
            FactRow(f) { nav.fact(f.id) }
        }
    }
}

@Composable
private fun HeroCard(f: Fact, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(f.field.brush())
            .clickable(onClick = onClick)
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FieldChip(f.field, onColor = true, extra = if (f.brag) "자랑하기 좋은" else "알아두면 쓸모 있는")
        Text(f.title, color = Color.White, fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold)
        Text(f.short, color = Color.White.copy(alpha = 0.92f), fontSize = 15.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("자세히 보기", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White)
        }
    }
}

@Composable
fun FieldFilter(selected: Field?, onSelect: (Field?) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterPill("전체", selected == null, BrandBrushColor) { onSelect(null) }
        Field.entries.forEach { f -> FilterPill(f.label, selected == f, f) { onSelect(f) } }
    }
}

private object BrandBrushColor

@Composable
private fun FilterPill(text: String, on: Boolean, look: Any, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    val bg = when {
        !on -> Modifier.background(Palette.card).border(1.dp, Palette.line, shape)
        look is Field -> Modifier.background(look.brush())
        else -> Modifier.background(BrandBrush)
    }
    Text(
        text,
        Modifier.clip(shape).then(bg).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
        color = if (on) Color.White else Palette.ink,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
    )
}
