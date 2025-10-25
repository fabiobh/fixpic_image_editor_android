package com.uaialternativa.imageeditor.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uaialternativa.imageeditor.R

/**
 * Settings menu component that navigates to the settings screen
 */
@Composable
fun SettingsMenu(
    onSettingsClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settingsMenuDesc = stringResource(R.string.settings_menu)

    IconButton(
        onClick = onSettingsClicked,
        modifier = modifier.semantics {
            contentDescription = settingsMenuDesc
        }
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = settingsMenuDesc,
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}