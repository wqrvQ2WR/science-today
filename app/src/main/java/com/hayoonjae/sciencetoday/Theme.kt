package com.hayoonjae.sciencetoday

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

object Palette {
    val bg = Color(0xFFF6F5FB)
    val card = Color(0xFFFFFFFF)
    val ink = Color(0xFF1E1B2E)
    val dim = Color(0xFF6E6A80)
    val line = Color(0xFFE8E6F0)
    val pink = Color(0xFFFF4D9D)
    val purple = Color(0xFF9B5CFF)
    val sky = Color(0xFF3DA9FF)
    val good = Color(0xFF16A34A)
    val goodBg = Color(0xFFE7F8EE)
    val bad = Color(0xFFE11D48)
    val badBg = Color(0xFFFDECEF)
    val warnBg = Color(0xFFFFF3E6)
    val warnInk = Color(0xFFB45309)
}

val BrandBrush = Brush.linearGradient(listOf(Palette.pink, Palette.purple, Palette.sky))

/** 분야마다 다른 그라디언트. 위젯 배경(res/drawable/widget_bg_*.xml)도 같은 색을 쓴다. */
fun Field.colors(): Pair<Color, Color> = when (this) {
    Field.CHEM -> Color(0xFFFF4D9D) to Color(0xFFB44DFF)
    Field.PHYS -> Color(0xFF3DA9FF) to Color(0xFF5B5CFF)
    Field.BIO -> Color(0xFF22C55E) to Color(0xFF0EA5A0)
    Field.EARTH -> Color(0xFFF59E0B) to Color(0xFFEA580C)
    Field.SPACE -> Color(0xFF7C3AED) to Color(0xFF2563EB)
    Field.BODY -> Color(0xFFFF5A6E) to Color(0xFFFF8A4C)
}

fun Field.brush(): Brush = colors().let { (a, b) -> Brush.linearGradient(listOf(a, b)) }
fun Field.main(): Color = colors().first

val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_bold, FontWeight.Bold),
    Font(R.font.pretendard_extrabold, FontWeight.ExtraBold),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val b = Typography()
    fun TextStyle.p() = copy(fontFamily = Pretendard)
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Palette.purple,
            secondary = Palette.pink,
            tertiary = Palette.sky,
            background = Palette.bg,
            surface = Palette.card,
            onBackground = Palette.ink,
            onSurface = Palette.ink,
            surfaceContainer = Palette.card,
        ),
        typography = Typography(
            displayLarge = b.displayLarge.p(), displayMedium = b.displayMedium.p(), displaySmall = b.displaySmall.p(),
            headlineLarge = b.headlineLarge.p(), headlineMedium = b.headlineMedium.p(), headlineSmall = b.headlineSmall.p(),
            titleLarge = b.titleLarge.p(), titleMedium = b.titleMedium.p(), titleSmall = b.titleSmall.p(),
            bodyLarge = b.bodyLarge.p(), bodyMedium = b.bodyMedium.p(), bodySmall = b.bodySmall.p(),
            labelLarge = b.labelLarge.p(), labelMedium = b.labelMedium.p(), labelSmall = b.labelSmall.p(),
        ),
        content = content,
    )
}
