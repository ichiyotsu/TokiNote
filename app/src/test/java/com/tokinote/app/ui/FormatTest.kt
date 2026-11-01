package com.tokinote.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    @Test
    fun `clock formats minutes and seconds`() {
        assertEquals("25:00", formatClock(1500))
        assertEquals("00:09", formatClock(9))
        assertEquals("00:00", formatClock(0))
    }

    @Test
    fun `elapsed shows hours once it passes sixty minutes`() {
        assertEquals("25:00", formatElapsed(1500))
        assertEquals("59:59", formatElapsed(3599))
        assertEquals("1:00:00", formatElapsed(3600))
        assertEquals("1:01:01", formatElapsed(3661))
    }

    @Test
    fun `minutes formats hours and minutes`() {
        assertEquals("25 分钟", formatMinutes(25))
        assertEquals("1 小时 5 分钟", formatMinutes(65))
        assertEquals("2 小时", formatMinutes(120))
    }
}
