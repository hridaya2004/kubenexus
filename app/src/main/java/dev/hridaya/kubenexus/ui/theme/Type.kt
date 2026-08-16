package dev.hridaya.kubenexus.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

val GoogleSansFlex = FontFamily(
    Font(DeviceFontFamilyName("google-sans-flex"), FontWeight.Normal),
    Font(DeviceFontFamilyName("google-sans-flex"), FontWeight.Medium),
    Font(DeviceFontFamilyName("google-sans-flex"), FontWeight.SemiBold),
    Font(DeviceFontFamilyName("google-sans-flex"), FontWeight.Bold),
    Font(DeviceFontFamilyName("roboto"), FontWeight.Normal),
    Font(DeviceFontFamilyName("roboto"), FontWeight.Medium),
    Font(DeviceFontFamilyName("roboto"), FontWeight.SemiBold),
    Font(DeviceFontFamilyName("roboto"), FontWeight.Bold),
    Font(DeviceFontFamilyName("sans-serif"), FontWeight.Normal),
    Font(DeviceFontFamilyName("sans-serif"), FontWeight.Medium),
    Font(DeviceFontFamilyName("sans-serif"), FontWeight.SemiBold),
    Font(DeviceFontFamilyName("sans-serif"), FontWeight.Bold)
)

private val defaultTypography = Typography()

val Typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(
        fontFamily = GoogleSansFlex,
        fontFeatureSettings = "ss03"
    ),
    displayMedium = defaultTypography.displayMedium.copy(
        fontFamily = GoogleSansFlex,
        fontFeatureSettings = "ss03"
    ),
    displaySmall = defaultTypography.displaySmall.copy(
        fontFamily = GoogleSansFlex,
        fontFeatureSettings = "ss03"
    ),
    headlineLarge = defaultTypography.headlineLarge.copy(
        fontFamily = GoogleSansFlex,
        fontFeatureSettings = "ss03"
    ),
    headlineMedium = defaultTypography.headlineMedium.copy(
        fontFamily = GoogleSansFlex,
        fontFeatureSettings = "ss03"
    ),
    headlineSmall = defaultTypography.headlineSmall.copy(
        fontFamily = GoogleSansFlex,
        fontFeatureSettings = "ss03"
    ),
    titleLarge = defaultTypography.titleLarge.copy(
        fontFamily = GoogleSansFlex,
        fontFeatureSettings = "ss03"
    ),
    titleMedium = defaultTypography.titleMedium.copy(
        fontFamily = GoogleSansFlex,
        fontFeatureSettings = "ss03"
    ),
    titleSmall = defaultTypography.titleSmall.copy(
        fontFamily = GoogleSansFlex,
        fontFeatureSettings = "ss03"
    ),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = GoogleSansFlex),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = GoogleSansFlex),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = GoogleSansFlex),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = GoogleSansFlex),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = GoogleSansFlex),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = GoogleSansFlex)
)