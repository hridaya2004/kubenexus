package dev.hridaya.kubenexus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
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

val TitleLargeEmphasized = createDeviceFontFamily(
    "variable-title-large-emphasized",
    "variable-title-large",
    "google-sans",
    "google-sans-flex",
)

val DisplayLargeFamily = createDeviceFontFamily(
    "variable-display-large",
    "google-sans",
    "roboto",
    "sans-serif",
)
val DisplayMediumFamily = createDeviceFontFamily(
    "variable-display-medium",
    "google-sans",
    "roboto",
    "sans-serif",
)
val DisplaySmallFamily = createDeviceFontFamily(
    "variable-display-small",
    "google-sans",
    "roboto",
    "sans-serif",
)

val HeadlineLargeFamily = createDeviceFontFamily(
    "variable-headline-large",
    "google-sans",
    "roboto",
    "sans-serif",
)
val HeadlineMediumFamily = createDeviceFontFamily(
    "variable-headline-medium",
    "google-sans",
    "roboto",
    "sans-serif",
)
val HeadlineSmallFamily = createDeviceFontFamily(
    "variable-headline-small",
    "google-sans",
    "roboto",
    "sans-serif",
)

val TitleMediumFamily = createDeviceFontFamily(
    "variable-title-medium",
    "google-sans-text",
    "google-sans",
    "roboto",
    "sans-serif",
)
val TitleSmallFamily = createDeviceFontFamily(
    "variable-title-small",
    "google-sans-text",
    "google-sans",
    "roboto",
    "sans-serif",
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

val GoogleSansFlex = TitleLargeEmphasized

private val defaultTypography = Typography()

val Typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(
        fontFamily = DisplayLargeFamily,
        fontWeight = FontWeight.Normal,
    ),
    displayMedium = defaultTypography.displayMedium.copy(
        fontFamily = DisplayMediumFamily,
        fontWeight = FontWeight.Normal,
    ),
    displaySmall = defaultTypography.displaySmall.copy(
        fontFamily = DisplaySmallFamily,
        fontWeight = FontWeight.Normal,
    ),
    headlineLarge = defaultTypography.headlineLarge.copy(
        fontFamily = HeadlineLargeFamily,
        fontWeight = FontWeight.Normal,
    ),
    headlineMedium = defaultTypography.headlineMedium.copy(
        fontFamily = HeadlineMediumFamily,
        fontWeight = FontWeight.Normal,
    ),
    headlineSmall = defaultTypography.headlineSmall.copy(
        fontFamily = HeadlineSmallFamily,
        fontWeight = FontWeight.Normal,
    ),
    titleLarge = defaultTypography.titleLarge.copy(
        fontFamily = TitleLargeEmphasized,
        fontWeight = FontWeight.SemiBold,
    ),
    titleMedium = defaultTypography.titleMedium.copy(
        fontFamily = TitleMediumFamily,
        fontWeight = FontWeight.Medium,
    ),
    titleSmall = defaultTypography.titleSmall.copy(
        fontFamily = TitleSmallFamily,
        fontWeight = FontWeight.Medium,
    ),
    bodyLarge = defaultTypography.bodyLarge.copy(
        fontFamily = BodyLargeFamily,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = defaultTypography.bodyMedium.copy(
        fontFamily = BodyMediumFamily,
        fontWeight = FontWeight.Normal,
    ),
    bodySmall = defaultTypography.bodySmall.copy(
        fontFamily = BodySmallFamily,
        fontWeight = FontWeight.Normal,
    ),
    labelLarge = defaultTypography.labelLarge.copy(
        fontFamily = LabelLargeFamily,
        fontWeight = FontWeight.Medium,
    ),
    labelMedium = defaultTypography.labelMedium.copy(
        fontFamily = LabelMediumFamily,
        fontWeight = FontWeight.Medium,
    ),
    labelSmall = defaultTypography.labelSmall.copy(
        fontFamily = LabelSmallFamily,
        fontWeight = FontWeight.Medium,
    ),
)

/**
 * Material Design 3 Expressive Emphasized Typography Scale.
 * Reference: https://m3.material.io/styles/typography/overview
 */
@Immutable
data class ExpressiveTypography(
    val displayLargeEmphasized: TextStyle = Typography.displayLarge.copy(fontWeight = FontWeight.Bold),
    val displayMediumEmphasized: TextStyle = Typography.displayMedium.copy(fontWeight = FontWeight.Bold),
    val displaySmallEmphasized: TextStyle = Typography.displaySmall.copy(fontWeight = FontWeight.Bold),
    val headlineLargeEmphasized: TextStyle = Typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
    val headlineMediumEmphasized: TextStyle = Typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
    val headlineSmallEmphasized: TextStyle = Typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
    val titleLargeEmphasized: TextStyle = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
    val titleMediumEmphasized: TextStyle = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
    val titleSmallEmphasized: TextStyle = Typography.titleSmall.copy(fontWeight = FontWeight.Bold),
    val bodyLargeEmphasized: TextStyle = Typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
    val bodyMediumEmphasized: TextStyle = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
    val bodySmallEmphasized: TextStyle = Typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
    val labelLargeEmphasized: TextStyle = Typography.labelLarge.copy(fontWeight = FontWeight.Bold),
    val labelMediumEmphasized: TextStyle = Typography.labelMedium.copy(fontWeight = FontWeight.Bold),
    val labelSmallEmphasized: TextStyle = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
)

val LocalExpressiveTypography = staticCompositionLocalOf { ExpressiveTypography() }

val MaterialTheme.expressiveTypography: ExpressiveTypography
    @Composable
    @ReadOnlyComposable
    get() = LocalExpressiveTypography.current

