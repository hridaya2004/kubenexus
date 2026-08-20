package dev.hridaya.kubenexus.presentation.common.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material Design 3 Expressive Connected Button Group.
 * Replaces legacy Segmented Buttons with connected button items and adaptive shapes.
 * Reference: https://m3.material.io/components/button-groups/overview
 */
object ConnectedButtonGroupDefaults {
    val Height: Dp = 44.dp
    val Spacing: Dp = 2.dp
    val OuterCornerRadius: Dp = 16.dp
    val InnerCornerRadius: Dp = 4.dp

    fun itemShape(index: Int, count: Int): Shape {
        return when {
            count <= 1 -> RoundedCornerShape(OuterCornerRadius)
            index == 0 -> RoundedCornerShape(
                topStart = OuterCornerRadius,
                bottomStart = OuterCornerRadius,
                topEnd = InnerCornerRadius,
                bottomEnd = InnerCornerRadius,
            )
            index == count - 1 -> RoundedCornerShape(
                topStart = InnerCornerRadius,
                bottomStart = InnerCornerRadius,
                topEnd = OuterCornerRadius,
                bottomEnd = OuterCornerRadius,
            )
            else -> RoundedCornerShape(InnerCornerRadius)
        }
    }

    @Composable
    fun buttonColors(isSelected: Boolean): ButtonColors {
        return if (isSelected) {
            ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        } else {
            ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    @Composable
    fun border(isSelected: Boolean): BorderStroke? {
        return if (isSelected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun ConnectedButtonGroup(
    modifier: Modifier = Modifier,
    spacing: Dp = ConnectedButtonGroupDefaults.Spacing,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
