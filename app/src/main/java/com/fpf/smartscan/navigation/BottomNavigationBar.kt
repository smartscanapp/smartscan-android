package com.fpf.smartscan.navigation

import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.fpf.smartscan.R

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        BottomNavItem(Routes.SEARCH, stringResource(R.string.title_search), Icons.Filled.ImageSearch),
        BottomNavItem(Routes.COLLECTIONS, stringResource(R.string.title_collections), Icons.Filled.PhotoLibrary),
        BottomNavItem(Routes.CONCEPTS, stringResource(R.string.title_concepts), Icons.Filled.Lightbulb),
        BottomNavItem(Routes.SETTINGS, stringResource(R.string.title_settings), Icons.Filled.Settings)
    )

    NavigationBar() {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                ),
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = { navController.navigate(item.route) }
            )
        }
    }
}

data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)
