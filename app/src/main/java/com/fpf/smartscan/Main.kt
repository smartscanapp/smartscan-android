package com.fpf.smartscan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fpf.smartscan.core.concepts.Concept
import com.fpf.smartscan.core.models.ModelDownloadStatus
import com.fpf.smartscan.core.index.IndexingStatus
import com.fpf.smartscan.core.media.MediaCollection
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.navigation.Routes
import com.fpf.smartscan.navigation.BottomNavigationBar
import com.fpf.smartscan.navigation.NavDataKeys
import com.fpf.smartscan.navigation.TopBarState
import com.fpf.smartscan.core.search.SearchQuery
import com.fpf.smartscan.ui.components.ScanLoadingView
import com.fpf.smartscan.ui.components.ScanModal
import com.fpf.smartscan.ui.components.UpdatePopUp
import com.fpf.smartscan.ui.components.modals.ProgressModal
import com.fpf.smartscan.ui.permissions.RequestPermissions
import com.fpf.smartscan.ui.permissions.StorageAccess
import com.fpf.smartscan.ui.permissions.getStorageAccess
import com.fpf.smartscan.ui.screens.bin.BinScreen
import com.fpf.smartscan.ui.screens.collections.CollectionItemsScreen
import com.fpf.smartscan.ui.screens.collections.CollectionsScreen
import com.fpf.smartscan.ui.screens.concepts.ConceptItemsScreen
import com.fpf.smartscan.ui.screens.concepts.ConceptsScreen
import com.fpf.smartscan.ui.screens.donate.DonateScreen
import com.fpf.smartscan.ui.screens.models.DownloadModelScreen
import com.fpf.smartscan.ui.screens.search.SearchScreen
import com.fpf.smartscan.ui.screens.settings.SettingsDetailScreen
import com.fpf.smartscan.ui.screens.settings.SettingsScreen
import com.fpf.smartscan.ui.screens.settings.SettingsViewModel
import com.fpf.smartscan.ui.shared.MediaViewModel
import com.fpf.smartscan.utils.showNotification
import com.fpf.smartscansdk.ml.models.ModelName
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Main(
    intentSearchQuery: SearchQuery?,
    onAppReady: () -> Unit,
    onRestartApp: () -> Unit,
) {
    val context = LocalContext.current
    val topBarState = remember { mutableStateOf(TopBarState()) }
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val mainRoute = Routes.EXPLORE
    val mainViewModel: MainViewModel = koinViewModel()
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val mediaViewModel: MediaViewModel = koinViewModel()
    val isUpdatePopUpVisible by mainViewModel.isUpdatePopUpVisible.collectAsState()

    // Indexing
    val imageIndexProgress by mainViewModel.imageIndexProgress.collectAsState()
    val videoIndexProgress by mainViewModel.videoIndexProgress.collectAsState()
    val imageIndexStatus by mainViewModel.imageIndexStatus.collectAsState()
    val videoIndexStatus by mainViewModel.videoIndexStatus.collectAsState()
    val cloudImageIndexProgress by mainViewModel.cloudImageIndexProgress.collectAsState()
    val cloudImageIndexStatus by mainViewModel.cloudImageIndexStatus.collectAsState()
    val hasIndexedImages by mainViewModel.hasIndexedImages.collectAsState()
    val hasIndexedVideos by mainViewModel.hasIndexedVideos.collectAsState()
    val runningMediaTypes by mainViewModel.runningMediaTypes.collectAsState()
    val isIndexing = imageIndexStatus == IndexingStatus.ACTIVE ||
            videoIndexStatus == IndexingStatus.ACTIVE ||
            cloudImageIndexStatus == IndexingStatus.ACTIVE ||
            runningMediaTypes.isNotEmpty()
    val indexCompleteTitle = stringResource(R.string.notif_title_index_complete)
    var showFirstScanModal by remember { mutableStateOf(false) }
    var showScanAndRebuildModal by remember { mutableStateOf(false) }
    var showRefreshScanModel by remember { mutableStateOf(false) }
    var requiredMediaTypeToIndex by remember { mutableStateOf<MediaType?>(null) }

    // Model downloading
    val modelDownloadStatus by mainViewModel.modelDownloadStatus.collectAsState()
    val modelDownloadProgress by mainViewModel.modelDownloadProgress.collectAsState()
    val installedModels by mainViewModel.installedModels.collectAsState()
    val modelDownloadRequired =  ModelName.ALL_MINILM_L6_V2 !in installedModels

    var hasStoragePermission by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        hasStoragePermission = getStorageAccess(context) != StorageAccess.Denied
        mainViewModel.prepareApp { onAppReady() }
    }

    LaunchedEffect(imageIndexStatus) {
        if (imageIndexStatus in listOf(IndexingStatus.COMPLETE, IndexingStatus.FAILED)) {
            when(imageIndexStatus){
                IndexingStatus.COMPLETE -> {
                    val content = mainViewModel.getIndexCompleteNotification(MediaType.IMAGE)
                    content?.let {
                        showNotification(context, title = indexCompleteTitle, text = it, 100)
                    }
                    mediaViewModel.findAndMarkDuplicates(MediaType.IMAGE)
                }
                IndexingStatus.FAILED -> {
                    val (indexFailTitle, indexFailContent) = mainViewModel.getIndexFailNotification(MediaType.IMAGE)
                    showNotification(context, indexFailTitle, indexFailContent, 100)
                }
                else -> {}
            }
            mainViewModel.onIndexingFinished(MediaType.IMAGE)
        }
    }

    LaunchedEffect(videoIndexStatus) {
        if (videoIndexStatus in listOf(IndexingStatus.COMPLETE, IndexingStatus.FAILED)) {
            when(videoIndexStatus){
                IndexingStatus.COMPLETE -> {
                    val content = mainViewModel.getIndexCompleteNotification(MediaType.VIDEO)
                    content?.let {
                        showNotification(context, title = indexCompleteTitle, text = it, 100)
                    }
                }
                IndexingStatus.FAILED -> {
                    val (indexFailTitle, indexFailContent) = mainViewModel.getIndexFailNotification(MediaType.VIDEO)
                    showNotification(context, indexFailTitle, indexFailContent, 100)
                }
                else -> {}
            }
            mainViewModel.onIndexingFinished(MediaType.VIDEO)
        }
    }

    LaunchedEffect(cloudImageIndexStatus) {
        if (cloudImageIndexStatus in listOf(IndexingStatus.COMPLETE, IndexingStatus.FAILED)) {
            mainViewModel.onConceptIndexingFinished(MediaType.IMAGE)
        }
    }

    LaunchedEffect(modelDownloadStatus) {
        if (modelDownloadStatus in listOf(ModelDownloadStatus.COMPLETE, ModelDownloadStatus.FAILED)) {
            mainViewModel.resetModelProgress()
        }
    }

    if(isUpdatePopUpVisible || modelDownloadRequired){
        when {
            isUpdatePopUpVisible -> UpdatePopUp(
                isVisible = true,
                updates = mainViewModel.getUpdates(),
                onClose = { mainViewModel.closeUpdatePopUp() },
            )
            else -> DownloadModelScreen(
                message = stringResource(R.string.download_model_message),
                onDownload = { mainViewModel.downloadModel() }
            )
        }
    }else {
        if (currentRoute == mainRoute) {
            RequestPermissions { _, storageGranted -> hasStoragePermission = storageGranted }
        }

        Scaffold(
            topBar = {
                if(!isIndexing) {
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
                }
            },
            bottomBar = { if(!isIndexing){
                BottomNavigationBar(navController)
            } }
        ) { paddingValues ->
            Box(
                modifier = Modifier.padding(paddingValues)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = mainRoute,
                ) {
                    composable(Routes.EXPLORE) {
                        SearchScreen(
                            mediaViewModel = mediaViewModel,
                            appSettings = settingsViewModel.appSettings,
                            onTopBarChange = { topBarState.value = it },
                            intentSearchQuery = intentSearchQuery,
                            hasIndexedImages = hasIndexedImages,
                            hasIndexedVideos = hasIndexedVideos,
                            hasStoragePermission = hasStoragePermission,
                            onIndex = {
                                if(!isIndexing) {
                                    requiredMediaTypeToIndex = it
                                    showFirstScanModal = true
                                }
                            },
                            onViewCollection = { collection ->
                                navController.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.set(NavDataKeys.COLLECTION, collection)

                                navController.navigate(Routes.COLLECTION_ITEMS)
                            },
                            onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                            onNavigateToRecyclingBin = { navController.navigate(Routes.BIN) },
                            )
                    }
                    composable(Routes.COLLECTIONS) {
                        CollectionsScreen(
                            isMainScanRequired = hasIndexedImages == false && hasIndexedVideos == false,
                            hasStoragePermission = hasStoragePermission,
                            onTopBarChange = { topBarState.value = it },
                            onViewCollection = { collection ->
                                navController.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.set(NavDataKeys.COLLECTION, collection)

                                navController.navigate(Routes.COLLECTION_ITEMS)
                            },
                            onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                            onNavigateToRecyclingBin = { navController.navigate(Routes.BIN) },
                            )
                    }
                    composable(
                        route = Routes.COLLECTION_ITEMS,
                    ) { _ ->
                        val collection =
                            navController.previousBackStackEntry?.savedStateHandle?.get<MediaCollection>(
                                NavDataKeys.COLLECTION
                            )

                        CollectionItemsScreen(
                            mediaViewModel = mediaViewModel,
                            onTopBarChange = { topBarState.value = it },
                            collection = collection,
                            appSettings = settingsViewModel.appSettings,
                            onBack = { navController.popBackStack() },
                            onViewCollection = { collection ->
                                navController.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.set(NavDataKeys.COLLECTION, collection)

                                navController.navigate(Routes.COLLECTION_ITEMS)
                            },
                        )
                    }
                    composable(Routes.CONCEPTS) {
                        ConceptsScreen(
                            appSettings = settingsViewModel.appSettings,
                            isMainScanRequired = hasIndexedImages == false && hasIndexedVideos == false,
                            hasStoragePermission = hasStoragePermission,
                            onTopBarChange = { topBarState.value = it },
                            onGenerateSummaries = {
                                mainViewModel.startConceptIndexing(listOf(MediaType.IMAGE))
                            },
                            onViewConcept = { concept ->
                                navController.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.set(NavDataKeys.CONCEPT, concept)

                                navController.navigate(Routes.CONCEPT_ITEMS)
                            },
                            onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
                        )
                    }
                    composable(
                        route = Routes.CONCEPT_ITEMS,
                    ) { _ ->
                        val concept =
                            navController.previousBackStackEntry?.savedStateHandle?.get<Concept>(
                                NavDataKeys.CONCEPT
                            )

                        ConceptItemsScreen(
                            mediaViewModel = mediaViewModel,
                            onTopBarChange = { topBarState.value = it },
                            concept = concept,
                            onBack = { navController.popBackStack() },
                            onViewCollection = { collection ->
                                navController.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.set(NavDataKeys.COLLECTION, collection)

                                navController.navigate(Routes.COLLECTION_ITEMS)
                            },
                        )
                    }
                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            onTopBarChange = { topBarState.value = it },
                            viewModel = settingsViewModel,
                            onNavigate = { route: String ->
                                navController.navigate(route)
                            },
                            onRestartApp = onRestartApp,
                            onScanRebuild = { showScanAndRebuildModal = true },
                            onScanRefresh = { showRefreshScanModel = true },
                            onBack = { navController.popBackStack() },
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
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable(Routes.DONATE) {
                        DonateScreen(
                            onTopBarChange = { topBarState.value = it },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.BIN) {
                        BinScreen(
                            appSettings = settingsViewModel.appSettings,
                            mediaViewModel = mediaViewModel,
                            onTopBarChange = { topBarState.value = it },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
                if(isIndexing){
                    ScanLoadingView(
                        isIndexing = true,
                        imageIndexStatus = if (cloudImageIndexStatus == IndexingStatus.ACTIVE) cloudImageIndexStatus else imageIndexStatus,
                        videoIndexStatus = videoIndexStatus,
                        videoIndexProgress = videoIndexProgress,
                        imageIndexProgress = if (cloudImageIndexStatus == IndexingStatus.ACTIVE) cloudImageIndexProgress else imageIndexProgress,
                        title = stringResource(R.string.scan_in_progress_title),
                        message = stringResource(R.string.scan_in_progress_content)
                    )
                }
            }
        }
    }



    ProgressModal(
        isVisible = modelDownloadStatus == ModelDownloadStatus.ACTIVE,
        progress = modelDownloadProgress,
        title = "Downloading model"
    )

    ScanModal(
        showFirstScanModal,
        onClose = {
            showFirstScanModal = false
            requiredMediaTypeToIndex = null
        },
        onConfirm = {
            mainViewModel.refreshMediaIndex(it)
            showFirstScanModal = false
            requiredMediaTypeToIndex = null
        },
        title = requiredMediaTypeToIndex?.let {
            stringResource(
                R.string.scan_media_action,
                it.name.lowercase() + "s"
            )
        } ?: stringResource(R.string.scan_action),
        description = requiredMediaTypeToIndex?.let {
            stringResource(
                R.string.alert_first_media_scan_content,
                it.name.lowercase()
            )
        } ?: stringResource(R.string.alert_first_scan_content),
        mediaType = requiredMediaTypeToIndex
    )

    ScanModal(
        showRefreshScanModel,
        onClose = {
            showRefreshScanModel = false
        },
        onConfirm = {
            mainViewModel.refreshMediaIndex(it)
            showRefreshScanModel = false
        },
        title = stringResource(R.string.scan_action),
        description = stringResource(R.string.alert_scan_refresh_description),
    )

    ScanModal(
        showScanAndRebuildModal,
        onClose = {
            showScanAndRebuildModal = false
        },
        onConfirm = {
            mainViewModel.rebuildMediaIndex(it)
            showScanAndRebuildModal = false
        },
        title = stringResource(R.string.scan_rebuild_action),
        description = stringResource(R.string.alert_scan_rebuild_description),
    )
}
