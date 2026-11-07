package com.lifeclock.widget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.appwidget.AppWidgetProviderInfo
import android.os.Build
import com.lifeclock.widget.WidgetConfig
import java.time.ZoneId
import java.time.ZonedDateTime
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lifeclock.widget.Kind
import com.lifeclock.widget.Prefs
import java.util.concurrent.TimeUnit

abstract class BaseLifeProvider(private val def: Kind) : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        var next = Long.MAX_VALUE
        ids.forEach { next = minOf(next, update(context, manager, it, def)) }
        Tick.scheduleIn(context, next)
    }

    // Called repeatedly while a resize handle is dragged, so draw the cheap version
    // and let the tick bring back the animated one.
    override fun onAppWidgetOptionsChanged(
        context: Context, manager: AppWidgetManager, id: Int, newOptions: Bundle,
    ) {
        update(context, manager, id, def, light = true)
        Tick.scheduleIn(context, 1_500L)
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        ids.forEach { Prefs.clearWidget(context, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_DATE_CHANGED ->
                WidgetRefresh.all(context)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // survives reboots; the alarm tick handles the frequent refreshes
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WidgetRefresh.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<RefreshWorker>(15, TimeUnit.MINUTES).build(),
        )
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        if (!WidgetRefresh.hasWidgets(context)) {
            WorkManager.getInstance(context).cancelUniqueWork(WidgetRefresh.WORK_NAME)
            Tick.cancel(context)
        }
    }

    companion object {
        /** Redraws one widget. Returns how long until it needs drawing again. */
        fun update(context: Context, manager: AppWidgetManager, id: Int, def: Kind, light: Boolean = false): Long {
            val (w, h) = sizeOf(context, manager, id, def)
            val r = WidgetRenderer.render(context, id, w, h, def, light = light)
            manager.updateAppWidget(id, r.views)
            return r.refreshMs
        }

        /**
         * Size the launcher gives the widget right now, in dp. The options hold the portrait size as
         * min width x max height and the landscape size as max width x min height.
         */
        fun sizeOf(context: Context, manager: AppWidgetManager, id: Int, def: Kind): Pair<Int, Int> {
            val o = manager.getAppWidgetOptions(id)
            val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val w = o.getInt(if (landscape) AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val h = o.getInt(if (landscape) AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
            return (if (w > 0) w else if (def.wide) 300 else 160) to (if (h > 0) h else if (def.wide) 140 else 160)
        }
    }
}

class LifeMinimalProvider : BaseLifeProvider(Kind.NUMBER)
class LifeGridProvider : BaseLifeProvider(Kind.GRID)
class LifeDetailedProvider : BaseLifeProvider(Kind.RULER)
class LifeDotsProvider : BaseLifeProvider(Kind.DOTS)
class LifePlanetsProvider : BaseLifeProvider(Kind.PLANETS)
class LifePathProvider : BaseLifeProvider(Kind.PATH)
class LifeLightProvider : BaseLifeProvider(Kind.LIGHT)
class LifeMalaProvider : BaseLifeProvider(Kind.MALA)
class LifePulseProvider : BaseLifeProvider(Kind.PULSE)
class LifeGardenProvider : BaseLifeProvider(Kind.GARDEN)

/**
 * Refresh tick: one inexact alarm that doesn't wake the device (ELAPSED_REALTIME, no permission needed).
 * If the phone sleeps it stays pending and fires on wake-up, which is when new frames are needed anyway.
 */
object Tick {
    private const val ACTION = "com.lifeclock.widget.TICK"
    const val FLOW_DELAY_MS = 60_000L      // one set of frames lasts ~140 s
    const val STATIC_DELAY_MS = 240_000L

    private fun pending(ctx: Context): PendingIntent = PendingIntent.getBroadcast(
        ctx, 1, Intent(ctx, TickReceiver::class.java).setAction(ACTION),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun scheduleIn(ctx: Context, delayMs: Long) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + delayMs, pending(ctx))
    }

    fun cancel(ctx: Context) {
        (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending(ctx))
    }
}

class TickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WidgetRefresh.all(context)
    }
}

object WidgetRefresh {
    const val WORK_NAME = "life_widget_refresh"

    val PROVIDERS get() = WidgetRegistry.all.map { it.provider to it.kind }

    /** Redraws every placed widget and re-arms the tick, or cancels it when none are left. */
    fun all(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        var next = Long.MAX_VALUE
        for ((cls, def) in PROVIDERS) {
            mgr.getAppWidgetIds(ComponentName(context, cls)).forEach {
                next = minOf(next, BaseLifeProvider.update(context, mgr, it, def))
            }
        }
        if (next != Long.MAX_VALUE) Tick.scheduleIn(context, next) else Tick.cancel(context)
    }

    fun setOpacityAll(context: Context, percent: Int) {
        Prefs.setOpacityDefault(context, percent)
        val mgr = AppWidgetManager.getInstance(context)
        for ((cls, _) in PROVIDERS) {
            mgr.getAppWidgetIds(ComponentName(context, cls)).forEach { Prefs.setOpacity(context, it, percent) }
        }
        all(context)
    }

    /** Returns false when the launcher can't pin widgets. */
    fun requestPin(context: Context, kind: Kind): Boolean {
        val mgr = AppWidgetManager.getInstance(context)
        if (!mgr.isRequestPinAppWidgetSupported) return false
        val cls = PROVIDERS.first { it.second == kind }.first
        return mgr.requestPinAppWidget(ComponentName(context, cls), null, null)
    }

    fun hasWidgets(context: Context): Boolean {
        val mgr = AppWidgetManager.getInstance(context)
        return PROVIDERS.any { (cls, _) -> mgr.getAppWidgetIds(ComponentName(context, cls)).isNotEmpty() }
    }
}

/**
 * On Android 15+ the picker preview is rendered by the real renderer (setWidgetPreview).
 * Older systems use the static previewLayout from the provider XML.
 */
object WidgetPreviews {
    fun update(context: Context) {
        if (Build.VERSION.SDK_INT < 35) return
        val sp = context.getSharedPreferences("life_clock", Context.MODE_PRIVATE)
        val stamp = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime }.getOrDefault(0L)
        val key = "$stamp:${Prefs.birth(context)?.toInstant()?.toEpochMilli()}"
        if (sp.getString("preview_key", null) == key) return
        val sample = ZonedDateTime.of(2000, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault())
        val mgr = AppWidgetManager.getInstance(context)
        var ok = true
        for (spec in WidgetRegistry.all) {
            val (cls, kind) = spec.provider to spec.kind
            val (w, h) = spec.previewWidthDp to spec.previewHeightDp
            runCatching {
                val cfg = WidgetConfig(kind = kind, opacity = if (kind.wide) 60 else 0, scrim = 1)
                val r = WidgetRenderer.render(context, 0, w, h, kind, override = cfg, light = true, birthOverride = Prefs.birth(context) ?: sample)
                mgr.setWidgetPreview(ComponentName(context, cls), AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN, r.views)
            }.onFailure {
                Log.e("Anicca", "picker preview failed for $kind", it)
                ok = false
            }
        }
        if (ok) sp.edit().putString("preview_key", key).apply()
    }
}

class RefreshWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        WidgetRefresh.all(applicationContext)
        return Result.success()
    }
}
