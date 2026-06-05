package com.nwe.spadesscore.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.nwe.spadesscore.R

private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val patrickHand = GoogleFont("Patrick Hand")

/** Playful brand font used for display/headline styles; body stays on the M3 default. */
val PatrickHandFamily = FontFamily(
    Font(googleFont = patrickHand, fontProvider = googleFontProvider, weight = FontWeight.Normal)
)

private val default = Typography()

val AppTypography = default.copy(
    displayLarge = default.displayLarge.copy(fontFamily = PatrickHandFamily),
    displayMedium = default.displayMedium.copy(fontFamily = PatrickHandFamily),
    displaySmall = default.displaySmall.copy(fontFamily = PatrickHandFamily),
    headlineLarge = default.headlineLarge.copy(fontFamily = PatrickHandFamily),
    headlineMedium = default.headlineMedium.copy(fontFamily = PatrickHandFamily),
    headlineSmall = default.headlineSmall.copy(fontFamily = PatrickHandFamily),
    titleLarge = default.titleLarge.copy(fontFamily = PatrickHandFamily)
)
