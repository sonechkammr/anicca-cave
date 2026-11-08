package com.lifeclock.widget.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lifeclock.widget.LifeMath
import com.lifeclock.widget.Prefs
import com.lifeclock.widget.widget.WidgetRefresh
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime

data class AppState(
    val birth: ZonedDateTime?,
    /** 0 follows the system, 1 light, 2 dark. */
    val themeMode: Int,
)

/** Holds app state over the existing [Prefs] (still SharedPreferences) and the single clock for the UI. */
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext

    private val _state = MutableStateFlow(AppState(Prefs.birth(ctx), Prefs.theme(ctx)))
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val _confetti = MutableStateFlow(0)
    /** Increments each time the confetti should play. */
    val confetti: StateFlow<Int> = _confetti.asStateFlow()

    /**
     * Wall-clock millis at 10 Hz, the one ticker for every screen. The last visible decimal of the age changes
     * only every 0.32 s, so more is wasted work. It runs only while a collector is lifecycle-active
     * (plus 5 s of grace for rotation).
     */
    val nowMs: StateFlow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(TICK_MS)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), System.currentTimeMillis())

    init {
        celebrateIfMilestone(_state.value.birth)
    }

    fun saveBirth(birth: ZonedDateTime) {
        Prefs.saveBirth(ctx, birth)
        _state.update { it.copy(birth = birth) }
        WidgetRefresh.all(ctx)
        celebrateIfMilestone(birth)
    }

    fun setTheme(mode: Int) {
        Prefs.setTheme(ctx, mode)
        _state.update { it.copy(themeMode = mode) }
    }

    /** Every 1000th day and every exact birthday, once per calendar day. */
    private fun celebrateIfMilestone(birth: ZonedDateTime?) {
        val b = birth ?: return
        val s = LifeMath.snapshot(b)
        val today = LocalDate.now().toString()
        val milestone = (s.totalDays > 0 && s.totalDays % 1000L == 0L) ||
            (s.years > 0 && s.months == 0 && s.weeks == 0 && s.days == 0)
        if (milestone && Prefs.celebrated(ctx) != today) {
            Prefs.setCelebrated(ctx, today)
            viewModelScope.launch {
                delay(900)
                _confetti.update { it + 1 }
            }
        }
    }

    private companion object {
        const val TICK_MS = 100L
    }
}
