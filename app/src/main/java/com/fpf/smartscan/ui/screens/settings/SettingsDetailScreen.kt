package com.fpf.smartscan.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fpf.smartscan.ui.components.pickers.DirectoryPicker
import com.fpf.smartscan.R
import com.fpf.smartscan.navigation.SettingsRoutes
import com.fpf.smartscan.navigation.TopBarState
import com.fpf.smartscan.ui.components.common.TextInput
import com.fpf.smartscan.ui.components.models.ModelsList


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDetailScreen(
    type: String,
    viewModel: SettingsViewModel,
    onTopBarChange: (TopBarState) -> Unit,
    onBack: () -> Unit,
) {
    val appSettings by viewModel.appSettings.collectAsState()
    val installedModels by viewModel.installedModels.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.modelEvent.collect { event ->
            Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
        }
    }

    val screenTitle = when (type) {
        SettingsRoutes.MODELS -> stringResource(R.string.setting_models)
        SettingsRoutes.MANAGE_MODELS -> stringResource(R.string.setting_manage_models)
        SettingsRoutes.ALLOWED_FOLDERS -> stringResource(R.string.setting_allowed_folders)
        SettingsRoutes.API -> stringResource(R.string.setting_api_keys)
        else -> ""
    }

    LaunchedEffect(Unit) {
        onTopBarChange(
            TopBarState(
                title = screenTitle,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        )
    }


    Box(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        Column {
            when (type) {
                SettingsRoutes.MODELS -> {
                    ModelsList(
                        installedModels = installedModels,
                        modelList = viewModel.availableModelRegistry.values.toList(),
                        onDownload= { viewModel.downloadModel(it) },
                        onDelete = viewModel::deleteModel,
                    )
                }

                SettingsRoutes.ALLOWED_FOLDERS -> {
                    var isImageFolders by remember { mutableStateOf(true) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isImageFolders)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceContainer
                                )
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    isImageFolders = true
                                }
                                .padding(12.dp)
                        ) {
                            Text(
                                "Images",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isImageFolders)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (!isImageFolders)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceContainer
                                )
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    isImageFolders = false
                                }
                                .padding(12.dp)
                        ) {
                            Text(
                                "Videos",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (!isImageFolders)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    if(isImageFolders){
                        DirectoryPicker(
                            directories = appSettings.searchableImageDirectories,
                            addDirectory = { newDir -> viewModel.addSearchableImageDirectory(newDir) },
                            deleteDirectory = { newDir -> viewModel.deleteSearchableImageDirectory(newDir) },
                        )
                    }else{
                        DirectoryPicker(
                            directories = appSettings.searchableVideoDirectories,
                            addDirectory = { newDir -> viewModel.addSearchableVideoDirectory(newDir) },
                            deleteDirectory = { newDir -> viewModel.deleteSearchableVideoDirectory(newDir) },
                        )
                    }

                }
                SettingsRoutes.API-> {
                    TextInput(
                        label = stringResource(R.string.setting_openai_api_key),
                        value = appSettings.openaiApiKey?:"",
                        onValueChange = viewModel::updateOpenaiApiKey,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.placeholders_api_key),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                    )
                }
                else -> {}
            }
        }
    }
}
