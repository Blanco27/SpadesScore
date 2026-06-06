package com.nwe.spadesscore.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NameValidationTest {

    @Test
    fun allValid_returnsOk() {
        assertEquals(NameValidationResult.Ok, NameValidation.validate(listOf("Anna", "Ben", "Cara")))
    }

    @Test
    fun blankName_returnsEmpty() {
        assertEquals(NameValidationResult.Empty, NameValidation.validate(listOf("Anna", "   ", "Cara")))
    }

    @Test
    fun emptyName_returnsEmpty() {
        assertEquals(NameValidationResult.Empty, NameValidation.validate(listOf("Anna", "", "Cara")))
    }

    @Test
    fun tooLongName_returnsTooLong() {
        assertEquals(NameValidationResult.TooLong, NameValidation.validate(listOf("Anna", "ElevenChars", "Cara")))
    }

    @Test
    fun emptyTakesPrecedenceOverTooLong() {
        assertEquals(NameValidationResult.Empty, NameValidation.validate(listOf("", "ElevenChars")))
    }
}
