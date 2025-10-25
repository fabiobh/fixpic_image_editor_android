package com.uaialternativa.imageeditor.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uaialternativa.imageeditor.R

/**
 * Settings screen with language selector, theme selector, and app information
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLanguageSelected: (String) -> Unit,
    onThemeSelected: (Boolean) -> Unit,
    currentIsDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Calculate current language state
    val isPortuguese = remember(context) {
        val currentLocale = context.resources.configuration.locales[0]
        currentLocale.language == "pt" && currentLocale.country == "BR"
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Language Selection Section
            SettingsSection(
                title = stringResource(R.string.language_settings),
                icon = Icons.Default.Settings
            ) {
                LanguageSelector(
                    isPortuguese = isPortuguese,
                    onLanguageSelected = onLanguageSelected
                )
            }
            
            // Theme Selection Section
            SettingsSection(
                title = stringResource(R.string.theme_settings),
                icon = Icons.Default.Edit
            ) {
                ThemeSelector(
                    currentIsDarkTheme = currentIsDarkTheme,
                    onThemeSelected = onThemeSelected
                )
            }
            
            // App Information Section
            SettingsSection(
                title = stringResource(R.string.app_information),
                icon = Icons.Default.Info
            ) {
                AppInformation()
            }
        }
    }
}

/**
 * Reusable settings section component
 */
@Composable
private fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            content()
        }
    }
}

/**
 * Language selector component
 */
@Composable
private fun LanguageSelector(
    isPortuguese: Boolean,
    onLanguageSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.selectableGroup()
    ) {
        // English option
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = !isPortuguese,
                    onClick = { onLanguageSelected("en") },
                    role = Role.RadioButton
                )
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = !isPortuguese,
                onClick = null
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "🌐 ${stringResource(R.string.language_english)}",
                style = MaterialTheme.typography.bodyLarge
            )
            if (!isPortuguese) {
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        // Portuguese option
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = isPortuguese,
                    onClick = { onLanguageSelected("pt-BR") },
                    role = Role.RadioButton
                )
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isPortuguese,
                onClick = null
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "🇧🇷 ${stringResource(R.string.language_portuguese)}",
                style = MaterialTheme.typography.bodyLarge
            )
            if (isPortuguese) {
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Theme selector component
 */
@Composable
private fun ThemeSelector(
    currentIsDarkTheme: Boolean,
    onThemeSelected: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.selectableGroup()
    ) {
        // Light theme option
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = !currentIsDarkTheme,
                    onClick = { onThemeSelected(false) },
                    role = Role.RadioButton
                )
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = !currentIsDarkTheme,
                onClick = null
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "☀️ ${stringResource(R.string.light_theme)}",
                style = MaterialTheme.typography.bodyLarge
            )
            if (!currentIsDarkTheme) {
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        // Dark theme option
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = currentIsDarkTheme,
                    onClick = { onThemeSelected(true) },
                    role = Role.RadioButton
                )
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = currentIsDarkTheme,
                onClick = null
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "🌙 ${stringResource(R.string.dark_theme)}",
                style = MaterialTheme.typography.bodyLarge
            )
            if (currentIsDarkTheme) {
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * App information component
 */
@Composable
private fun AppInformation() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        InfoRow(
            label = stringResource(R.string.app_name_label),
            value = stringResource(R.string.app_name)
        )
        
        InfoRow(
            label = stringResource(R.string.version_label),
            value = "1.0"
        )
        
        InfoRow(
            label = stringResource(R.string.build_number_label),
            value = "1"
        )
        
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Text(
            text = stringResource(R.string.app_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Information row component
 */
@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}