package com.fpf.smartscan.ui.components.media

import android.content.ClipData
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.R
import com.fpf.smartscan.core.media.MediaItem
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.core.media.openImageInGallery
import com.fpf.smartscan.core.media.openVideoInGallery
import com.fpf.smartscan.core.media.shareMedia
import com.fpf.smartscan.ui.action.MenuActionConfig
import com.fpf.smartscan.ui.components.common.ActionRowWithFade
import com.fpf.smartscan.ui.components.common.DropDownMenuWrapper
import com.fpf.smartscan.core.utils.canOpenUri


@Composable
fun MediaViewerActionRow(
    item: MediaItem,
    showMenu: Boolean,
    isVisible: Boolean,
    actionsEnabled: Boolean = true,
    toggleMenu: () -> Unit,
    onClose: () -> Unit,
    onViewDescription: () -> Unit,
    onUpdateSearchImage: ((uri: Uri) -> Unit)?,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val isUriAccessible = canOpenUri(context, item.uri)

    val menuActions: List<MenuActionConfig> = listOf(
        MenuActionConfig.Button(
            label = stringResource(R.string.view_details_action),
            onClick = { onViewDescription() },
        ),
        MenuActionConfig.Button(
            label = stringResource(R.string.share_action),
            onClick = { shareMedia(context, item.uri) },
            enabled = isUriAccessible && actionsEnabled,
            hideIfDisabled = true
        ),
        MenuActionConfig.Button(
            label = stringResource(R.string.open_in_gallery_action),
            onClick ={
                if (item.type == MediaType.IMAGE) {
                    openImageInGallery(context, item.uri)
                } else {
                    openVideoInGallery(context, item.uri)
                }
                     },
            enabled = isUriAccessible && actionsEnabled,
            hideIfDisabled = true
        ),
        MenuActionConfig.Button(
            label = stringResource(R.string.copy_to_clipboard_action),
            onClick = {
                clipboard.nativeClipboard.setPrimaryClip(
                    ClipData.newUri(context.contentResolver, "smartscan_media", item.uri)
                )
            },
            enabled = isUriAccessible && item.type == MediaType.IMAGE&& actionsEnabled,
            hideIfDisabled = true
        ),
        MenuActionConfig.Button(
            label = stringResource(R.string.search_action),
            onClick = { onUpdateSearchImage?.invoke(item.uri) },
            enabled = isUriAccessible && item.type == MediaType.IMAGE && onUpdateSearchImage != null && actionsEnabled,
            hideIfDisabled = true
        ),
    )

    ActionRowWithFade(
        visible = isVisible,
        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
    ) {
        IconButton(onClick = { onClose() }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Close Image",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        Box{
            IconButton (onClick = { toggleMenu() }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "menu"
                )
            }
            DropDownMenuWrapper(
                modifier = Modifier.widthIn(min = 144.dp),
                expanded = showMenu,
                actions = menuActions,
                onClose = {toggleMenu()}
            )
        }
    }
}

