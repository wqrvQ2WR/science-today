package com.hayoonjae.sciencetoday

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import java.io.File

/** 상식을 인스타·카톡에 올리기 좋은 4:5 카드 이미지로 그려서 공유한다. */
object ShareCard {
    private const val W = 1080
    private const val H = 1350
    private const val PAD = 96

    fun share(ctx: Context, f: Fact) {
        val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "today-science.png")
        val bmp = render(ctx, f)
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TEXT, "오늘의 과학: ${f.title}")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri("", uri)
        ctx.startActivity(Intent.createChooser(send, "상식 공유하기"))
    }

    fun render(ctx: Context, f: Fact): Bitmap {
        val bold = ResourcesCompat.getFont(ctx, R.font.pretendard_extrabold) ?: Typeface.DEFAULT_BOLD
        val regular = ResourcesCompat.getFont(ctx, R.font.pretendard_regular) ?: Typeface.DEFAULT
        val (a, b) = f.field.colors()

        val out = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, W.toFloat(), H.toFloat(), a.toArgb(), b.toArgb(), Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), bg)

        // 배경에 흐릿한 동그라미 두 개.
        val soft = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x1FFFFFFF }
        c.drawCircle(W * 0.92f, H * 0.12f, 300f, soft)
        c.drawCircle(W * 0.05f, H * 0.95f, 260f, soft)

        // 위쪽 분야 이름표.
        val chip = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = bold; textSize = 38f; color = 0xFFFFFFFF.toInt() }
        val chipText = "오늘의 과학 · ${f.field.label}"
        val chipW = chip.measureText(chipText)
        val chipBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x38FFFFFF }
        c.drawRoundRect(RectF(PAD.toFloat(), PAD.toFloat(), PAD + chipW + 64f, PAD + 84f), 42f, 42f, chipBg)
        c.drawText(chipText, PAD + 32f, PAD + 56f, chip)

        val width = W - PAD * 2
        val title = layout(f.title, TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = bold; textSize = 92f; color = 0xFFFFFFFF.toInt() }, width, 1.12f)
        val short = layout(f.short, TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = regular; textSize = 46f; color = 0xEBFFFFFF.toInt() }, width, 1.4f)

        val top = PAD + 84f + 110f
        c.save(); c.translate(PAD.toFloat(), top); title.draw(c); c.restore()
        c.save(); c.translate(PAD.toFloat(), top + title.height + 56f); short.draw(c); c.restore()

        val foot = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = bold; textSize = 36f; color = 0xCCFFFFFF.toInt() }
        c.drawText("오늘의 과학", PAD.toFloat(), H - PAD.toFloat(), foot)
        return out
    }

    private fun layout(text: String, paint: TextPaint, width: Int, spacing: Float): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, spacing)
            .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
            .build()
}
