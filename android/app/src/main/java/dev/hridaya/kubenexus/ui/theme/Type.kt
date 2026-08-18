package dev.hridaya.kubenexus.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

private fun createDeviceFontFamily(vararg primaryNames: String): FontFamily {
    val weights = listOf(
        FontWeight.W100,
        FontWeight.W200,
        FontWeight.W300,
        FontWeight.W400,
        FontWeight.W500,
        FontWeight.W600,
        FontWeight.W700,
        FontWeight.W800,
        FontWeight.W900,
    )
    val fontList = mutableListOf<Font>()

    for (name in primaryNames) {
        for (w in weights) {
            fontList.add(Font(DeviceFontFamilyName(name), w, FontStyle.Normal))
            fontList.add(Font(DeviceFontFamilyName(name), w, FontStyle.Italic))
        }
    }

    val standardFallbacks = listOf("google-sans-flex", "google-sans", "roboto", "sans-serif")
    for (fb in standardFallbacks) {
        if (fb !in primaryNames) {
            for (w in weights) {
                fontList.add(Font(DeviceFontFamilyName(fb), w, FontStyle.Normal))
                fontList.add(Font(DeviceFontFamilyName(fb), w, FontStyle.Italic))
            }
        }
    }

    return FontFamily(fontList)
}

val DisplayLargeEmphasized = createDeviceFontFamily(
    "variable-display-large-emphasized",
    "variable-display-large",
    "google-sans",
    "google-sans-flex",
)
val DisplayMediumEmphasized = createDeviceFontFamily(
    "variable-display-medium-emphasized",
    "variable-display-medium",
    "google-sans",
    "google-sans-flex",
)
val DisplaySmallEmphasized = createDeviceFontFamily(
    "variable-display-small-emphasized",
    "variable-display-small",
    "google-sans",
    "google-sans-flex",
)

val HeadlineLargeEmphasized = createDeviceFontFamily(
    "variable-headline-large-emphasized",
    "variable-headline-large",
    "google-sans",
    "google-sans-flex",
)
val HeadlineMediumEmphasized = createDeviceFontFamily(
    "variable-headline-medium-emphasized",
    "variable-headline-medium",
    "google-sans",
    "google-sans-flex",
)
val HeadlineSmallEmphasized = createDeviceFontFamily(
    "variable-headline-small-emphasized",
    "variable-headline-small",
    "google-sans",
    "google-sans-flex",
)

val TitleLargeEmphasized = createDeviceFontFamily(
    "variable-title-large-emphasized",
    "variable-title-large",
    "google-sans",
    "google-sans-flex",
)
val TitleMediumEmphasized = createDeviceFontFamily(
    "variable-title-medium-emphasized",
    "variable-title-medium",
    "google-sans-text",
    "google-sans-flex",
    "google-sans",
)
val TitleSmallEmphasized = createDeviceFontFamily(
    "variable-title-small-emphasized",
    "variable-title-small",
    "google-sans-text",
    "google-sans-flex",
    "google-sans",
)

val BodyLargeFamily = createDeviceFontFamily(
    "variable-body-large",
    "google-sans-text",
    "google-sans-flex",
    "google-sans",
)
val BodyMediumFamily = createDeviceFontFamily(
    "variable-body-medium",
    "google-sans-text",
    "google-sans-flex",
    "google-sans",
)
val BodySmallFamily = createDeviceFontFamily(
    "variable-body-small",
    "google-sans-text",
    "google-sans-flex",
    "google-sans",
)

val LabelLargeFamily = createDeviceFontFamily(
    "variable-label-large",
    "google-sans-text",
    "google-sans-flex",
    "google-sans",
)
val LabelMediumFamily = createDeviceFontFamily(
    "variable-label-medium",
    "google-sans-text",
    "google-sans-flex",
    "google-sans",
)
val LabelSmallFamily = createDeviceFontFamily(
    "variable-label-small",
    "google-sans-text",
    "google-sans-flex",
    "google-sans",
)

val GoogleSansFlex = HeadlineLargeEmphasized

private val defaultTypography = Typography()

val Typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = DisplayLargeEmphasized, fontWeight = FontWeight.SemiBold),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = DisplayMediumEmphasized, fontWeight = FontWeight.SemiBold),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = DisplaySmallEmphasized, fontWeight = FontWeight.SemiBold),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = HeadlineLargeEmphasized, fontWeight = FontWeight.SemiBold),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = HeadlineMediumEmphasized, fontWeight = FontWeight.SemiBold),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = HeadlineSmallEmphasized, fontWeight = FontWeight.SemiBold),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = TitleLargeEmphasized, fontWeight = FontWeight.SemiBold),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = TitleMediumEmphasized, fontWeight = FontWeight.SemiBold),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = TitleSmallEmphasized, fontWeight = FontWeight.SemiBold),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = BodyLargeFamily, fontWeight = FontWeight.Normal),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = BodyMediumFamily, fontWeight = FontWeight.Normal),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = BodySmallFamily, fontWeight = FontWeight.Normal),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = LabelLargeFamily, fontWeight = FontWeight.Medium),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = LabelMediumFamily, fontWeight = FontWeight.Medium),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = LabelSmallFamily, fontWeight = FontWeight.Medium),
)

