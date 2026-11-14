package com.hayoonjae.sciencetoday

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.ZoneId

/**
 * 오늘의 과학 위젯. 작은 것(2x1)은 제목만, 큰 것(4x2)은 짧은 설명까지 보여 준다.
 * 어떤 상식이 나올지는 날짜로만 정해지므로 위젯을 여러 개 놔도 같은 상식이 나온다.
 */
abstract class TodayWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        Widgets.updateAll(ctx)
        Widgets.scheduleMidnight(ctx)
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        when (intent.action) {
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            -> {
                Widgets.updateAll(ctx)
                Widgets.scheduleMidnight(ctx)
            }
        }
    }
}

class SmallWidget : TodayWidget()
class LargeWidget : TodayWidget()

/** 자정마다 깨어나 위젯을 다음 상식으로 바꾼다. */
class MidnightReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Widgets.updateAll(ctx)
        Widgets.scheduleMidnight(ctx)
    }
}

object Widgets {
    fun updateAll(ctx: Context) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val fact = Library.get(ctx).factOfDay(Today.day())
        for ((cls, layout) in listOf(SmallWidget::class.java to R.layout.widget_small, LargeWidget::class.java to R.layout.widget_large)) {
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, cls))
            if (ids.isEmpty()) continue
            val views = views(ctx, fact, layout)
            for (id in ids) mgr.updateAppWidget(id, views)
        }
    }

    private fun views(ctx: Context, f: Fact, layout: Int) = RemoteViews(ctx.packageName, layout).apply {
        setInt(R.id.widget_root, "setBackgroundResource", background(f.field))
        setTextViewText(R.id.widget_caption, "오늘의 과학 · ${f.field.label}")
        setTextViewText(R.id.widget_title, f.title)
        if (layout == R.layout.widget_large) setTextViewText(R.id.widget_short, f.short)
        setOnClickPendingIntent(
            R.id.widget_root,
            PendingIntent.getActivity(
                ctx, layout,
                Intent(ctx, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_FACT, f.id)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )
    }

    private fun background(f: Field) = when (f) {
        Field.CHEM -> R.drawable.widget_bg_chem
        Field.PHYS -> R.drawable.widget_bg_phys
        Field.BIO -> R.drawable.widget_bg_bio
        Field.EARTH -> R.drawable.widget_bg_earth
        Field.SPACE -> R.drawable.widget_bg_space
        Field.BODY -> R.drawable.widget_bg_body
    }

    /** 정확한 알람은 권한이 필요해서 느슨한 알람을 쓴다. 조금 늦게 깨도 날짜로 다시 고르므로 틀어지지 않는다. */
    fun scheduleMidnight(ctx: Context) {
        val zone = ZoneId.systemDefault()
        val next = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() + 5_000
        val pi = PendingIntent.getBroadcast(
            ctx, 1, Intent(ctx, MidnightReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi)
    }
}
