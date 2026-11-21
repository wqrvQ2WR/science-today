package com.hayoonjae.sciencetoday

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FactScreen(f: Fact, lib: Library, nav: Nav) {
    val ctx = LocalContext.current
    val uri = LocalUriHandler.current
    Column(Modifier.fillMaxSize()) {
        TopBar(f.field.label, onBack = nav::back) {
            IconButton(onClick = { ShareCard.share(ctx, f) }) {
                Icon(Icons.Filled.Share, contentDescription = "공유", tint = Palette.ink)
            }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Gap,
        ) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(f.field.brush()).padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FieldChip(f.field, onColor = true, extra = if (f.brag) "자랑하기 좋은" else "알아두면 쓸모 있는")
                Text(f.title, color = Color.White, fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold)
                Text(f.short, color = Color.White.copy(alpha = 0.92f), fontSize = 15.sp, lineHeight = 22.sp)
            }

            f.safety?.let { s ->
                Row(
                    Modifier.fillMaxWidth().clip(CardShape).background(Palette.warnBg).padding(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(Icons.Filled.Warning, null, tint = Palette.warnInk, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(s, color = Palette.warnInk, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            SectionTitle("왜 그럴까?")
            Card {
                val words = f.terms.mapNotNull { id -> lib.termById[id]?.let { id to it.word } }
                val ranges = linkRanges(f.detail, words)
                val text = buildAnnotatedString {
                    var at = 0
                    for ((s, e, id) in ranges) {
                        append(f.detail.substring(at, s))
                        withLink(
                            LinkAnnotation.Clickable(
                                tag = id,
                                styles = TextLinkStyles(
                                    SpanStyle(color = f.field.main(), fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline),
                                ),
                            ) { nav.term(id) },
                        ) { append(f.detail.substring(s, e)) }
                        at = e
                    }
                    append(f.detail.substring(at))
                }
                Text(text, color = Palette.ink, fontSize = 16.sp, lineHeight = 27.sp)
            }

            if (f.terms.isNotEmpty()) {
                SectionTitle("관련 용어")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    f.terms.mapNotNull { lib.termById[it] }.forEach { t ->
                        Text(
                            t.word,
                            Modifier.clip(RoundedCornerShape(50)).background(Palette.card)
                                .border(1.dp, Palette.line, RoundedCornerShape(50))
                                .clickable { nav.term(t.id) }.padding(horizontal = 14.dp, vertical = 8.dp),
                            color = Palette.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            GradientButton("이 상식으로 퀴즈 풀기", brush = f.field.brush()) { nav.quiz(lib.factQuiz(f), QuizMode.FACT) }

            SectionTitle("출처")
            f.sources.forEach { s ->
                Card(onClick = if (s.url.isNotEmpty()) ({ runCatching { uri.openUri(s.url) } }) else null) {
                    Column {
                        Text(s.title, color = if (s.url.isNotEmpty()) Palette.purple else Palette.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (s.year > 0) "${s.publisher} · ${s.year}" else s.publisher,
                            color = Palette.dim, fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TermScreen(t: Term, lib: Library, progress: Progress, nav: Nav) {
    val fav = t.id in progress.favorites
    Column(Modifier.fillMaxSize()) {
        TopBar("사전", onBack = nav::back) {
            IconButton(onClick = { progress.toggleFavorite(t.id) }) {
                Icon(
                    if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (fav) "즐겨찾기 빼기" else "즐겨찾기",
                    tint = if (fav) Palette.pink else Palette.ink,
                )
            }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Gap,
        ) {
            Column {
                FieldChip(t.field, onColor = false)
                Spacer(Modifier.height(8.dp))
                Text(t.word, color = Palette.ink, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Text(t.en, color = Palette.dim, fontSize = 15.sp)
            }
            Column(
                Modifier.fillMaxWidth().clip(CardShape).background(t.field.brush()).padding(18.dp),
            ) {
                Text(t.one, color = Color.White, fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold)
            }
            Card { Text(t.detail, color = Palette.ink, fontSize = 16.sp, lineHeight = 27.sp) }
            val used = lib.factsUsing(t.id)
            if (used.isNotEmpty()) {
                SectionTitle("이 용어가 나오는 상식")
                used.forEach { f -> FactRow(f) { nav.fact(f.id) } }
            }
        }
    }
}
