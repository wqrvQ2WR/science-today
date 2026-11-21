package com.hayoonjae.sciencetoday

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

/** 퀴즈 한 판의 진행 상태. 화면을 떠나면 버린다. */
class QuizSession(val items: List<QItem>, val mode: QuizMode, val day: Long) {
    var pos by mutableIntStateOf(0)
    var picked by mutableStateOf<Int?>(null)
    var score by mutableIntStateOf(0)
    val missed = mutableStateListOf<QItem>()
    val done get() = pos >= items.size
}

@Composable
fun QuizHomeScreen(lib: Library, progress: Progress, nav: Nav) {
    val day = Today.day()
    val score = progress.dailyScore(day)
    val todayFact = lib.factOfDay(day)
    val wrongItems = progress.wrong.mapNotNull(lib::itemOf)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Gap,
    ) {
        item {
            Column {
                Text("연속 ${Streak.current(progress.quizDays, day)}일째", color = Palette.dim, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                BigTitle("퀴즈")
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(BrandBrush)
                    .clickable { nav.quiz(lib.dailyQuiz(day), QuizMode.DAILY, day) }.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("오늘의 퀴즈", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text(todayFact.title, color = Color.White.copy(alpha = 0.92f), fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (score == null) "${todayFact.quiz.size}문제 풀고 연속 기록 잇기" else "${todayFact.quiz.size}문제 중 ${score}개 맞힘 · 다시 풀기",
                    color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                )
            }
        }
        item { SectionTitle("분야별 10문제") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Field.entries.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { f ->
                            Box(
                                Modifier.weight(1f).height(72.dp).clip(RoundedCornerShape(18.dp)).background(f.brush())
                                    .clickable { nav.quiz(lib.fieldQuiz(f, Random.Default), QuizMode.FIELD) }.padding(14.dp),
                                contentAlignment = Alignment.BottomStart,
                            ) { Text(f.label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold) }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(4.dp)) }
        item { GradientButton("전체 섞어서 10문제") { nav.quiz(lib.randomQuiz(Random.Default), QuizMode.RANDOM) } }
        item {
            GradientButton(
                if (wrongItems.isEmpty()) "오답노트가 비어 있어요" else "오답노트 ${wrongItems.size}문제",
                enabled = wrongItems.isNotEmpty(),
                brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(Palette.bad, Palette.pink)),
            ) { nav.quiz(wrongItems, QuizMode.WRONG) }
        }
    }
}

@Composable
fun QuizScreen(s: QuizSession, progress: Progress, nav: Nav) {
    Column(Modifier.fillMaxSize()) {
        TopBar(
            when (s.mode) {
                QuizMode.DAILY -> "오늘의 퀴즈"
                QuizMode.FACT -> "상식 퀴즈"
                QuizMode.FIELD -> "${s.items.first().fact.field.label} 퀴즈"
                QuizMode.RANDOM -> "섞어서 퀴즈"
                QuizMode.WRONG -> "오답노트"
            },
            onBack = nav::back,
        )
        if (s.done) Result(s, nav) else Asking(s, progress, nav)
    }
}

@Composable
private fun Asking(s: QuizSession, progress: Progress, nav: Nav) {
    val item = s.items[s.pos]
    val q = item.q
    val picked = s.picked

    fun pick(i: Int) {
        if (s.picked != null) return
        s.picked = i
        val ok = q.isCorrect(i)
        if (ok) s.score++ else s.missed += item
        progress.record(item, ok)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Gap,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${s.pos + 1} / ${s.items.size}", color = Palette.dim, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(12.dp))
            Meter((s.pos + (if (picked != null) 1 else 0)).toFloat() / s.items.size, BrandBrush, Modifier.weight(1f))
        }
        Card {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FieldChip(item.fact.field, onColor = false, extra = if (q is OxQ) "OX" else "4지선다")
                Text(q.q, color = Palette.ink, fontSize = 20.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold)
            }
        }

        when (q) {
            is OxQ -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("O", "X").forEachIndexed { i, label ->
                    Option(label, i, q, picked, Modifier.weight(1f).height(110.dp), big = true) { pick(i) }
                }
            }
            is ChoiceQ -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                q.options.forEachIndexed { i, label ->
                    Option("${i + 1}.  $label", i, q, picked, Modifier.fillMaxWidth()) { pick(i) }
                }
            }
        }

        if (picked != null) {
            val ok = q.isCorrect(picked)
            Column(
                Modifier.fillMaxWidth().clip(CardShape).background(if (ok) Palette.goodBg else Palette.badBg).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(if (ok) "정답" else "아쉽다, 오답", color = if (ok) Palette.good else Palette.bad, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                Text(q.explain, color = Palette.ink, fontSize = 15.sp, lineHeight = 22.sp)
                Text(
                    "상식 다시 보기",
                    Modifier.clickable { nav.fact(item.fact.id) }.padding(top = 4.dp),
                    color = Palette.purple, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                )
            }
            val last = s.pos == s.items.lastIndex
            GradientButton(if (last) "결과 보기" else "다음 문제") {
                if (last && s.mode == QuizMode.DAILY) progress.finishDaily(s.day, s.score)
                s.picked = null
                s.pos++
            }
        }
    }
}

@Composable
private fun Option(text: String, index: Int, q: Question, picked: Int?, modifier: Modifier, big: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val answer = q.correctPick()
    val (bg, border, ink) = when {
        picked == null -> Triple(Palette.card, Palette.line, Palette.ink)
        index == answer -> Triple(Palette.goodBg, Palette.good, Palette.good)
        index == picked -> Triple(Palette.badBg, Palette.bad, Palette.bad)
        else -> Triple(Palette.card, Palette.line, Palette.dim)
    }
    Box(
        modifier.clip(shape).background(bg).border(if (picked != null && (index == answer || index == picked)) 2.dp else 1.dp, border, shape)
            .clickable(enabled = picked == null, onClick = onClick).padding(16.dp),
        contentAlignment = if (big) Alignment.Center else Alignment.CenterStart,
    ) {
        Text(
            text, color = ink,
            fontSize = if (big) 44.sp else 16.sp,
            lineHeight = if (big) 50.sp else 22.sp,
            fontWeight = if (big) FontWeight.ExtraBold else FontWeight.Bold,
        )
    }
}

@Composable
private fun Result(s: QuizSession, nav: Nav) {
    val all = s.items.size
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Gap,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier.size(170.dp).clip(RoundedCornerShape(85.dp)).background(BrandBrush),
            contentAlignment = Alignment.Center,
        ) {
            Text("${s.score} / $all", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
        }
        Text(
            when {
                s.score == all -> "전부 맞혔다!"
                s.score == 0 -> "하나도 못 맞혔다. 상식부터 다시 보자"
                else -> "${s.score}개 맞혔다"
            },
            color = Palette.ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center,
        )
        if (s.mode == QuizMode.DAILY) Text("오늘의 퀴즈 완료. 연속 기록이 이어졌어요.", color = Palette.dim, fontSize = 14.sp)
        if (s.missed.isNotEmpty()) {
            SectionTitle("틀린 문제", Modifier.fillMaxWidth())
            s.missed.distinctBy { it.fact.id }.forEach { m -> FactRow(m.fact) { nav.fact(m.fact.id) } }
            Text("틀린 문제는 오답노트에 모아 뒀어요.", color = Palette.dim, fontSize = 13.sp)
        }
        Spacer(Modifier.height(4.dp))
        GradientButton("돌아가기") { nav.back() }
    }
}
