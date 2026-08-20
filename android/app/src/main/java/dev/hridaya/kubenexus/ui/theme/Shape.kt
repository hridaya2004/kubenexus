package dev.hridaya.kubenexus.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Material Design 3 Expressive Shape System.
 * Reference: https://m3.material.io/styles/shape/overview
 */
@Immutable
data class ExpressiveShapes(
    val none: Shape = RoundedCornerShape(0.dp),
    val extraSmall: Shape = RoundedCornerShape(4.dp),
    val small: Shape = RoundedCornerShape(8.dp),
    val medium: Shape = RoundedCornerShape(12.dp),
    val large: Shape = RoundedCornerShape(16.dp),
    val largeIncreased: Shape = RoundedCornerShape(20.dp),
    val extraLarge: Shape = RoundedCornerShape(28.dp),
    val extraLargeIncreased: Shape = RoundedCornerShape(32.dp),
    val extraExtraLarge: Shape = RoundedCornerShape(48.dp),
    val full: Shape = CircleShape,
)

val LocalExpressiveShapes = staticCompositionLocalOf { ExpressiveShapes() }

val MaterialTheme.expressiveShapes: ExpressiveShapes
    @Composable
    @ReadOnlyComposable
    get() = LocalExpressiveShapes.current
