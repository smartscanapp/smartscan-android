package com.fpf.smartscan.ui.components.models

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fpf.smartscansdk.ml.models.ModelInfo

@Composable
fun ModelCard(
    modelInfo: ModelInfo,
    onDownload: (ModelInfo) -> Unit,
    onDelete: (model: ModelInfo) -> Unit,
    onImport: ((uri: Uri, modelInfo: ModelInfo) -> Unit)? = null,
    isInstalled: Boolean,
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { selectedUri ->
            context.contentResolver.takePersistableUriPermission(
                selectedUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            onImport?.invoke(uri, modelInfo)
        }
    }

    Card(
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = "Model icon",
                    modifier = Modifier.padding(end = 8.dp).size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = modelInfo.name.name.lowercase(),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Type: ${modelInfo.type.name.lowercase()}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.alpha(0.8f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if(isInstalled){
                            Button(
                                modifier = Modifier.padding(horizontal = 0.dp),
                                onClick = { onDelete(modelInfo) }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete icon",
                                    modifier = Modifier.padding(end = 4.dp).size(16.dp)
                                )
                                Text(text = "Delete", fontSize = 12.sp)
                            }
                        }else {
                            Button(
                                modifier = Modifier.padding(horizontal = 0.dp),
                                onClick = { onDownload(modelInfo) }
                            ) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = "Download icon",
                                    modifier = Modifier.padding(end = 4.dp).size(16.dp)
                                )
                                Text(text = "Download", fontSize = 12.sp)
                            }

                            if (onImport != null) {
                                Button(
                                    modifier = Modifier.padding(horizontal = 0.dp),
                                    onClick = {
                                        launcher.launch(
                                            arrayOf(
                                                "application/zip",
                                                "application/octet-stream",
                                                "application/x-tflite"
                                            )
                                        )
                                    },
                                ) {
                                    Icon(
                                        Icons.Default.FileUpload,
                                        contentDescription = "Import icon",
                                        modifier = Modifier.padding(end = 4.dp).size(16.dp)
                                    )
                                    Text(text = "Import", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}