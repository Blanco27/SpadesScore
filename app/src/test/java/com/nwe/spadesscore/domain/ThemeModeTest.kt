package com.nwe.spadesscore.domain

import com.nwe.spadesscore.domain.model.ThemeMode
import com.nwe.spadesscore.domain.model.themeModeFromStorage
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {
    @Test fun defaultsToSystemForNull() {
        assertEquals(ThemeMode.SYSTEM, themeModeFromStorage(null))
    }
    @Test fun defaultsToSystemForGarbage() {
        assertEquals(ThemeMode.SYSTEM, themeModeFromStorage("nope"))
    }
    @Test fun parsesEachKnownName() {
        assertEquals(ThemeMode.LIGHT, themeModeFromStorage("LIGHT"))
        assertEquals(ThemeMode.DARK, themeModeFromStorage("DARK"))
        assertEquals(ThemeMode.SYSTEM, themeModeFromStorage("SYSTEM"))
    }
    @Test fun roundTripsThroughName() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, themeModeFromStorage(mode.name))
        }
    }
}
