package dev.hridaya.kubenexus.presentation.navigation

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.hridaya.kubenexus.ui.theme.KubeNexusTheme

@Composable
fun AppNavigationBar(
    destinations: List<Destination>,
    currentDestination: Destination,
    onDestinationSelected: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 3.dp,
    ) {
        destinations.forEach { destination ->
            val isSelected = currentDestination == destination

            NavigationBarItem(
                selected = isSelected,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    BadgedBox(
                        badge = {
                            destination.badgeCount?.let { count ->
                                Badge {
                                    Text(text = "$count")
                                }
                            }
                        },
                    ) {
                        Icon(
                            imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = destination.title,
                        )
                    }
                },
                label = {
                    Text(
                        text = destination.title,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppNavigationBarPreview() {
    KubeNexusTheme {
        AppNavigationBar(
            destinations = Destination.topLevelDestinations,
            currentDestination = Destination.Home,
            onDestinationSelected = {},
        )
    }
}
