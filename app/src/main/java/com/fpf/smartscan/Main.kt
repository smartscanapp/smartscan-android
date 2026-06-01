package com.fpf.smartscan

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.navArgument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fpf.smartscan.media.MediaCollection
import com.fpf.smartscan.navigation.Routes
import com.fpf.smartscan.navigation.BottomNavigationBar
import com.fpf.smartscan.navigation.NavDataKeys
import com.fpf.smartscan.navigation.TopBarState
import com.fpf.smartscan.search.SearchQuery
import com.fpf.smartscan.ui.components.UpdatePopUp
import com.fpf.smartscan.ui.screens.collections.CollectionItemsScreen
import com.fpf.smartscan.ui.screens.collections.CollectionsScreen
import com.fpf.smartscan.ui.screens.donate.DonateScreen
import com.fpf.smartscan.ui.screens.search.SearchScreen
import com.fpf.smartscan.ui.screens.settings.SettingsDetailScreen
import com.fpf.smartscan.ui.screens.settings.SettingsScreen
import com.fpf.smartscan.ui.screens.settings.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Main(
    intentSearchQuery: SearchQuery?,
    onAppReady: () -> Unit,
    onRestartApp: () -> Unit
) {
    val topBarState = remember { mutableStateOf(TopBarState()) }
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = koinViewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val isUpdatePopUpVisible by mainViewModel.isUpdatePopUpVisible.collectAsState()

    LaunchedEffect(Unit) {
        mainViewModel.prepareApp{onAppReady() }
    }

        if (isUpdatePopUpVisible) {
            UpdatePopUp(
                isVisible = true,
                updates = mainViewModel.getUpdates(),
                onClose = { mainViewModel.closeUpdatePopUp() },
                notes = stringResource(R.string.update_notes)
            )
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(topBarState.value.title)
                        },
                        navigationIcon = {
                            topBarState.value.navigationIcon?.invoke()
                        },
                        actions = {
                            topBarState.value.actions?.invoke(this)
                        }
                    )
                },
                bottomBar = { BottomNavigationBar(navController) }
            ) { paddingValues ->
                NavHost(
                    navController = navController,
                    startDestination = Routes.SEARCH,
                    modifier = Modifier.padding(paddingValues)
                ) {
                    composable(Routes.SEARCH) {
                        SearchScreen(
                            appSettings = settingsViewModel.appSettings,
                            onTopBarChange = { topBarState.value = it },
                            intentSearchQuery = intentSearchQuery
                        )
                    }
                    composable(Routes.COLLECTIONS) {
                        CollectionsScreen(
                            onTopBarChange = { topBarState.value = it },
                            onViewCollection = { collection ->
                                navController.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.set(NavDataKeys.COLLECTION, collection)

                                navController.navigate(Routes.COLLECTION_ITEMS)
                            }
                        )
                    }
                    composable(
                        route = Routes.COLLECTION_ITEMS,
                    ) { _ ->
                        val collection = navController.previousBackStackEntry?.savedStateHandle?.get<MediaCollection>(NavDataKeys.COLLECTION)

                        CollectionItemsScreen(
                            onTopBarChange = { topBarState.value = it },
                            collection = collection,
                            appSettings = settingsViewModel.appSettings,
                            onBack = {navController.popBackStack()}
                        )
                    }
                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            onTopBarChange = { topBarState.value = it },
                            viewModel = settingsViewModel,
                            onNavigate = { route: String ->
                                navController.navigate(route)
                            },
                            onRestartApp = onRestartApp
                        )
                    }
                    composable(
                        route = Routes.SETTINGS_DETAIL,
                        arguments = listOf(navArgument("type") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val type = backStackEntry.arguments?.getString("type") ?: ""
                        SettingsDetailScreen(
                            onTopBarChange = { topBarState.value = it },
                            type = type,
                            viewModel = settingsViewModel,
                            onBack = {navController.popBackStack()},
                        )
                    }
                    composable(Routes.DONATE) {
                        DonateScreen(
                            onTopBarChange = { topBarState.value = it },
                            onBack = {navController.popBackStack()}
                            )
                    }
                }
            }
        }
    }
