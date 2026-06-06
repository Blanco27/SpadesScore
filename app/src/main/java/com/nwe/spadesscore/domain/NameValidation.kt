package com.nwe.spadesscore.domain

sealed interface NameValidationResult {
    data object Ok : NameValidationResult
    data object Empty : NameValidationResult
    data object TooLong : NameValidationResult
}

/** Pure Validierung der Spielernamen. Reihenfolge: leer hat Vorrang vor zu lang. */
object NameValidation {
    fun validate(names: List<String>): NameValidationResult = when {
        names.any { it.trim().isEmpty() } -> NameValidationResult.Empty
        names.any { it.length > GameRules.MAX_NAME_LENGTH } -> NameValidationResult.TooLong
        else -> NameValidationResult.Ok
    }
}
