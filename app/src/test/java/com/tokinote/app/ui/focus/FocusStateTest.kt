package com.tokinote.app.ui.focus

import com.tokinote.app.data.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class FocusStateTest {

    private val settings = AppSettings(sessionsBeforeLongBreak = 4)

    @Test
    fun `early focus sessions lead to a short break`() {
        val state = FocusState(mode = FocusMode.Focus, completedInCycle = 0, settings = settings)

        assertEquals(FocusMode.ShortBreak, state.nextMode)
    }

    @Test
    fun `every fourth focus session leads to a long break`() {
        val state = FocusState(mode = FocusMode.Focus, completedInCycle = 3, settings = settings)

        assertEquals(FocusMode.LongBreak, state.nextMode)
    }

    @Test
    fun `breaks always lead back to focus`() {
        val shortBreak = FocusState(mode = FocusMode.ShortBreak, settings = settings)
        val longBreak = FocusState(mode = FocusMode.LongBreak, settings = settings)

        assertEquals(FocusMode.Focus, shortBreak.nextMode)
        assertEquals(FocusMode.Focus, longBreak.nextMode)
    }

    @Test
    fun `progress reflects elapsed time`() {
        val half = FocusState(remainingSeconds = 750, totalSeconds = 1500)
        val fresh = FocusState(remainingSeconds = 1500, totalSeconds = 1500)

        assertEquals(0.5f, half.progress, 0.001f)
        assertEquals(0f, fresh.progress, 0.001f)
    }

    @Test
    fun `session index wraps inside the cycle`() {
        assertEquals(1, FocusState(completedInCycle = 0, settings = settings).sessionIndex)
        assertEquals(4, FocusState(completedInCycle = 3, settings = settings).sessionIndex)
        assertEquals(1, FocusState(completedInCycle = 4, settings = settings).sessionIndex)
    }
}
