package com.fpf.smartscan.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fpf.smartscan.R
import androidx.core.net.toUri
import com.fpf.smartscan.navigation.Routes
import com.fpf.smartscan.constants.colorSchemeDisplayNames
import com.fpf.smartscan.constants.themeModeDisplayNames
import com.fpf.smartscan.events.BackupEventType
import com.fpf.smartscan.navigation.SettingsRoutes
import com.fpf.smartscan.navigation.TopBarState
import com.fpf.smartscan.ui.action.SettingActionConfig
import com.fpf.smartscan.ui.components.pickers.OptionPicker
import com.fpf.smartscan.ui.components.settings.SettingSection
import com.fpf.smartscan.ui.screens.settings.SettingsViewModel.Companion.BACKUP_FILENAME
import com.fpf.smartscan.ui.theme.ColorSchemeType
import com.fpf.smartscan.ui.theme.ThemeMode

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onNavigate: (String) -> Unit,
    onTopBarChange: (TopBarState) -> Unit,
    onRestartApp: () -> Unit
) {
    val appSettings by viewModel.appSettings.collectAsState()
    val isBackupLoading by viewModel.isBackupLoading.collectAsState()
    val isRestoreLoading by viewModel.isRestoreLoading.collectAsState()

    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val sourceCodeUrl = stringResource(R.string.source_code_url)
    val redditUrl = stringResource(R.string.reddit_url)
    val versionName: String? = try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName
    } catch (_: Exception) {
        null
    }

    val screenTitle = stringResource(R.string.title_settings)

    LaunchedEffect(Unit) {
        onTopBarChange(
            TopBarState(title = screenTitle)
        )
    }

    LaunchedEffect(Unit) {
        viewModel.backupEvent.collect { event ->
            Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            when(event.type){
                BackupEventType.RESTORE -> if(event.success) onRestartApp()
                else -> {}
            }
        }
    }

    // Actions
    var isSelectingTheme by remember { mutableStateOf(false) }
    var isSelectingColor by remember { mutableStateOf(false) }
    var isSelectingRowsPerRow by remember { mutableStateOf(false) }

    val generalSettingActions: List<SettingActionConfig> = listOf(
        SettingActionConfig.Button(
            label = stringResource(id = R.string.setting_theme),
            onClick = { isSelectingTheme = true},
            description = themeModeDisplayNames[appSettings.theme]!!
            ),
        SettingActionConfig.Button(
            label = stringResource(id = R.string.setting_color),
            onClick = { isSelectingColor = true},
            description = colorSchemeDisplayNames[appSettings.color]!!
        ),
    )

    val searchSettingActions: List<SettingActionConfig> = listOf(
        SettingActionConfig.Button(
            label = stringResource(id = R.string.setting_similarity_threshold),
            onClick = { onNavigate(Routes.settingsDetail(SettingsRoutes.THRESHOLD)) },
            description = stringResource(R.string.setting_similarity_threshold_description)
        ),
        SettingActionConfig.Button(
            label = stringResource(id = R.string.setting_allowed_folders),
            onClick = { onNavigate(Routes.settingsDetail(SettingsRoutes.ALLOWED_FOLDERS)) },
            description = stringResource(R.string.setting_searchable_folders_description)
        ),
        SettingActionConfig.Button(
            label = stringResource(id = R.string.setting_search_result_columns),
            onClick = { isSelectingRowsPerRow = true},
            description = appSettings.resultsPerRow.toString()
        ),
        SettingActionConfig.Switch(
            label=stringResource(R.string.setting_auto_open_gallery),
            checked = appSettings.enableDirectGalleryOpen,
            onCheckedChange = viewModel::updateEnableDirectionGalleryOpen,
        ),
        SettingActionConfig.Switch(
            label=stringResource(R.string.setting_hide_duplicates),
            checked = appSettings.enableDedupe,
            onCheckedChange = viewModel::updateEnableDedupe,
            description = stringResource(R.string.setting_hide_duplicates_description)
        ),
    )

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { selectedUri ->
            context.contentResolver.takePersistableUriPermission(selectedUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewModel.restore(selectedUri)
        }
    }
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri?.let { fileUri ->
            context.contentResolver.takePersistableUriPermission(
                fileUri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.backup(fileUri)
        }
    }

    val backupSettingActions: List<SettingActionConfig> = listOf(
        SettingActionConfig.Button(
            enabled = !isBackupLoading && !isRestoreLoading,
            label = stringResource(id = R.string.setting_backup),
            description = stringResource(R.string.setting_backup_restore_description, "Export"),
            onClick = { backupLauncher.launch(BACKUP_FILENAME) },
        ),
        SettingActionConfig.Button(
            enabled = !isBackupLoading && !isRestoreLoading,
            label = stringResource(id = R.string.setting_restore),
            description = stringResource(R.string.setting_backup_restore_description, "Import"),
            onClick = {
                restoreLauncher.launch(
                    arrayOf(
                        "application/zip",
                        "application/octet-stream"
                    )
                )
            },
        )
    )

    val otherSettingActions: List<SettingActionConfig> = listOf(
        SettingActionConfig.Button(
            label = stringResource(id = R.string.title_donate),
            onClick = { onNavigate(Routes.DONATE) }
        ),
        SettingActionConfig.Button(
            label = stringResource(id = R.string.setting_source_code),
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, sourceCodeUrl.toUri())
                context.startActivity(intent)
            },
        ),
        SettingActionConfig.Button(
            label = stringResource(id = R.string.setting_social_reddit),
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, redditUrl.toUri())
                context.startActivity(intent)
            },
        )
    )


    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SettingSection(
                    stringResource(id = R.string.general_settings),
                    settingActionConfigs = generalSettingActions
                )

                SettingSection(
                    stringResource(id = R.string.search_settings),
                    settingActionConfigs = searchSettingActions
                )

                SettingSection(
                    stringResource(id = R.string.setting_backup_restore),
                    settingActionConfigs = backupSettingActions
                )

                SettingSection(
                    stringResource(id = R.string.other_settings),
                    settingActionConfigs = otherSettingActions
                )
            }

//                Text(
//                    text = stringResource(id = R.string.advanced_settings),
//                    style = MaterialTheme.typography.titleMedium,
//                    modifier = Modifier.padding(vertical = 8.dp),
//                    color = MaterialTheme.colorScheme.primary
//                )
//                ActionItem(
//                    text = stringResource(id = R.string.setting_models),
//                    onClick = { onNavigate(Routes.settingsDetail(SettingTypes.MODELS)) },
//                )
//                Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Image(
                    painter = painterResource(id = R.drawable.smartscan_logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(132.dp)
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                versionName?.let {
                    Text(
                        text = "Version $it",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = stringResource(R.string.copyright),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    OptionPicker(
        isVisible = isSelectingTheme,
        title = stringResource(id = R.string.setting_theme),
        selectedOption = themeModeDisplayNames[appSettings.theme]!!,
        options = themeModeDisplayNames.values.toList(),
        onClose = { isSelectingTheme = false },
        onSelect = { selected ->
            val theme = themeModeDisplayNames.entries.find { it.value == selected }?.key ?: ThemeMode.SYSTEM
            viewModel.updateTheme(theme)
            isSelectingTheme = false
        },
    )

    OptionPicker(
        isVisible = isSelectingColor,
        title = stringResource(id = R.string.setting_color),
        selectedOption = colorSchemeDisplayNames[appSettings.color]!!,
        options = colorSchemeDisplayNames.values.toList(),
        onClose = { isSelectingColor = false },
        onSelect = { selected ->
            val color = colorSchemeDisplayNames.entries.find { it.value == selected }?.key ?: ColorSchemeType.SMARTSCAN
            viewModel.updateColorScheme(color)
            isSelectingColor = false
        },
    )

    OptionPicker(
        isVisible = isSelectingRowsPerRow,
        title = stringResource(id = R.string.setting_search_result_columns),
        selectedOption = appSettings.resultsPerRow.toString(),
        options = (3 until 6).map { it.toString() },
        onClose = { isSelectingRowsPerRow = false },
        onSelect = { selected ->
            viewModel.updateResultsPerRow(selected.toInt())
            isSelectingRowsPerRow = false
        }
    )
}