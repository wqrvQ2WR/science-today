package com.hayoonjae.sciencetoday

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val CardShape = RoundedCornerShape(20.dp)

@Composable
fun Card(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Palette.card)
            .border(1.dp, Palette.line, CardShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
    ) { content() }
}

/** 분야 이름표. 그라디언트 위에서는 반투명 흰색, 흰 바탕에서는 분야 색. */
@Composable
fun FieldChip(field: Field, onColor: Boolean, extra: String? = null) {
    val text = if (extra != null) "${field.label} · $extra" else field.label
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (onColor) Color.White.copy(alpha = 0.22f) else field.main().copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        color = if (onColor) Color.White else field.main(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.padding(top = 8.dp), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Palette.ink)
}

@Composable
fun FactRow(f: Fact, onClick: () -> Unit) {
    Card(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(6.dp).height(40.dp).clip(RoundedCornerShape(3.dp)).background(f.field.brush()))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(f.field.label, color = f.field.main(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    f.title, color = Palette.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 화면 위 막대: 뒤로 버튼 + 제목 + 오른쪽 버튼들. */
@Composable
fun TopBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = Palette.ink) }
        Text(
            title, Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Palette.ink,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        actions()
    }
}

/** 그라디언트 큰 버튼. */
@Composable
fun GradientButton(text: String, modifier: Modifier = Modifier, brush: Brush = BrandBrush, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) brush else Brush.linearGradient(listOf(Palette.line, Palette.line)))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (enabled) Color.White else Palette.dim, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

/** 0~1 막대. */
@Composable
fun Meter(ratio: Float, brush: Brush, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Palette.line)) {
        if (ratio > 0f) Box(Modifier.fillMaxWidth(ratio.coerceIn(0f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(brush))
    }
}

@Composable
fun BigTitle(text: String) {
    Text(text, style = TextStyle(brush = BrandBrush, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, fontFamily = Pretendard))
}

@Composable
fun Dot(color: Color) = Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))

val Gap = Arrangement.spacedBy(12.dp)
