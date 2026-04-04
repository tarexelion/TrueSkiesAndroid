package com.trueskies.android.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trueskies.android.domain.models.Country
import com.trueskies.android.ui.components.LiquidGlassCard
import com.trueskies.android.ui.theme.*
import com.trueskies.android.ui.viewmodels.AccountSettingsViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    onBack: () -> Unit,
    viewModel: AccountSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var showCountryPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var editingName by remember { mutableStateOf(false) }
    var nameValue by remember(state.displayName) { mutableStateOf(state.displayName) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueSkiesColors.SurfacePrimary)
            .statusBarsPadding()
    ) {
        TopAppBar(
            title = {
                Text(
                    "Account Settings",
                    style = TrueSkiesTypography.headlineMedium,
                    color = TrueSkiesColors.TextPrimary
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TrueSkiesColors.AccentBlue
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = TrueSkiesColors.SurfacePrimary)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TrueSkiesSpacing.md),
            verticalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.sm)
        ) {
            Spacer(Modifier.height(TrueSkiesSpacing.xs))

            // ── PROFILE ──
            SectionHeader("Profile")
            LiquidGlassCard {
                Column {
                    // Display Name
                    if (editingName) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(TrueSkiesSpacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconBox(Icons.Default.Person, TrueSkiesColors.AccentBlue)
                            Spacer(Modifier.width(TrueSkiesSpacing.md))
                            OutlinedTextField(
                                value = nameValue,
                                onValueChange = { nameValue = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("Display Name") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    viewModel.setDisplayName(nameValue)
                                    editingName = false
                                    focusManager.clearFocus()
                                }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TrueSkiesColors.TextPrimary,
                                    unfocusedTextColor = TrueSkiesColors.TextPrimary,
                                    focusedBorderColor = TrueSkiesColors.AccentBlue,
                                    unfocusedBorderColor = TrueSkiesColors.TextMuted,
                                    cursorColor = TrueSkiesColors.AccentBlue,
                                    focusedLabelColor = TrueSkiesColors.AccentBlue,
                                    unfocusedLabelColor = TrueSkiesColors.TextMuted
                                )
                            )
                            Spacer(Modifier.width(TrueSkiesSpacing.xs))
                            IconButton(onClick = {
                                viewModel.setDisplayName(nameValue)
                                editingName = false
                                focusManager.clearFocus()
                            }) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Save",
                                    tint = TrueSkiesColors.StatusOnTime
                                )
                            }
                        }
                    } else {
                        AccountRow(
                            icon = Icons.Default.Person,
                            iconTint = TrueSkiesColors.AccentBlue,
                            title = "Display Name",
                            value = state.displayName.ifBlank { "Not set" },
                            valueColor = if (state.displayName.isBlank()) TrueSkiesColors.TextMuted else TrueSkiesColors.TextSecondary,
                            onClick = { editingName = true }
                        )
                    }

                    AccountDivider()

                    // Citizenship Country
                    val countryDisplay = if (state.citizenshipCountry.isNotBlank()) {
                        val country = Country.all.find { it.name == state.citizenshipCountry }
                        if (country != null) "${country.flagEmoji} ${country.name}" else state.citizenshipCountry
                    } else {
                        "Not set"
                    }
                    AccountRow(
                        icon = Icons.Default.Flag,
                        iconTint = TrueSkiesColors.AccentCyan,
                        title = "Citizenship Country",
                        value = countryDisplay,
                        valueColor = if (state.citizenshipCountry.isBlank()) TrueSkiesColors.TextMuted else TrueSkiesColors.TextSecondary,
                        onClick = { showCountryPicker = true }
                    )
                }
            }

            Spacer(Modifier.height(TrueSkiesSpacing.xs))

            // ── STORAGE ──
            SectionHeader("Storage")
            LiquidGlassCard {
                Column {
                    // Storage usage display
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(TrueSkiesSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconBox(Icons.Default.Storage, TrueSkiesColors.AccentBlue)
                        Spacer(Modifier.width(TrueSkiesSpacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Total Storage Used",
                                style = TrueSkiesTypography.titleMedium,
                                color = TrueSkiesColors.TextPrimary
                            )
                            Text(
                                state.totalStorageFormatted,
                                style = TrueSkiesTypography.bodySmall,
                                color = TrueSkiesColors.TextTertiary
                            )
                        }
                    }

                    AccountDivider()

                    // Storage breakdown
                    StorageBreakdownRow("Database", state.databaseSizeFormatted)
                    AccountDivider()
                    StorageBreakdownRow("Cache", state.cacheSizeFormatted)
                    AccountDivider()
                    StorageBreakdownRow("Preferences", state.preferencesSizeFormatted)

                    AccountDivider()

                    // Clear cache
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.clearCache(context)
                                Toast
                                    .makeText(context, "Cache cleared", Toast.LENGTH_SHORT)
                                    .show()
                            }
                            .padding(TrueSkiesSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconBox(Icons.Default.CleaningServices, TrueSkiesColors.Warning)
                        Spacer(Modifier.width(TrueSkiesSpacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Clear Cache",
                                style = TrueSkiesTypography.titleMedium,
                                color = TrueSkiesColors.TextPrimary
                            )
                            Text(
                                "Free up space by clearing cached data",
                                style = TrueSkiesTypography.bodySmall,
                                color = TrueSkiesColors.TextTertiary
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TrueSkiesColors.TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(TrueSkiesSpacing.xs))

            // ── DATA ──
            SectionHeader("Data")
            LiquidGlassCard {
                Column {
                    // Export data
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.exportDataAsJson(context) { file ->
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, "Export TrueSkies Data")
                                    )
                                }
                            }
                            .padding(TrueSkiesSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconBox(Icons.Default.FileDownload, TrueSkiesColors.StatusOnTime)
                        Spacer(Modifier.width(TrueSkiesSpacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Export Data as JSON",
                                style = TrueSkiesTypography.titleMedium,
                                color = TrueSkiesColors.TextPrimary
                            )
                            Text(
                                "Download all your flights and preferences",
                                style = TrueSkiesTypography.bodySmall,
                                color = TrueSkiesColors.TextTertiary
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TrueSkiesColors.TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(TrueSkiesSpacing.xs))

            // ── DANGER ZONE ──
            SectionHeader("Danger Zone")
            LiquidGlassCard {
                Column {
                    // Delete all data
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDeleteConfirm = true }
                            .padding(TrueSkiesSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(TrueSkiesCornerRadius.sm))
                                .background(TrueSkiesColors.Error.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.DeleteForever,
                                contentDescription = null,
                                tint = TrueSkiesColors.Error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(TrueSkiesSpacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Delete All Data",
                                style = TrueSkiesTypography.titleMedium,
                                color = TrueSkiesColors.Error,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Permanently remove all flights, preferences, and cached data",
                                style = TrueSkiesTypography.bodySmall,
                                color = TrueSkiesColors.TextTertiary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(TrueSkiesSpacing.xxl))
        }
    }

    // ─── Dialogs ───

    if (showCountryPicker) {
        CountryPickerDialog(
            selectedCountry = state.citizenshipCountry,
            onSelect = {
                viewModel.setCitizenshipCountry(it)
                showCountryPicker = false
            },
            onDismiss = { showCountryPicker = false }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = TrueSkiesColors.SurfaceSecondary,
            title = {
                Text(
                    "Delete All Data?",
                    color = TrueSkiesColors.TextPrimary
                )
            },
            text = {
                Text(
                    "This will permanently delete all your flights, preferences, and cached data. This action cannot be undone.",
                    color = TrueSkiesColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllData()
                        showDeleteConfirm = false
                        Toast.makeText(context, "All data deleted", Toast.LENGTH_SHORT).show()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TrueSkiesColors.Error,
                        contentColor = Color.White
                    )
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TrueSkiesColors.TextMuted)
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Country Picker Dialog
// ─────────────────────────────────────────────────────────────

@Composable
private fun CountryPickerDialog(
    selectedCountry: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCountries = remember(searchQuery) {
        if (searchQuery.isBlank()) Country.all
        else Country.all.filter { it.name.contains(searchQuery, ignoreCase = true) || it.code.contains(searchQuery, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .clip(RoundedCornerShape(TrueSkiesCornerRadius.xl))
                .background(TrueSkiesColors.SurfaceSecondary)
        ) {
            // Header
            Text(
                "Citizenship Country",
                style = TrueSkiesTypography.headlineSmall,
                color = TrueSkiesColors.TextPrimary,
                modifier = Modifier.padding(
                    start = TrueSkiesSpacing.xl,
                    end = TrueSkiesSpacing.xl,
                    top = TrueSkiesSpacing.lg,
                    bottom = TrueSkiesSpacing.sm
                )
            )

            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TrueSkiesSpacing.lg),
                placeholder = { Text("Search countries...") },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = TrueSkiesColors.TextMuted
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TrueSkiesColors.TextPrimary,
                    unfocusedTextColor = TrueSkiesColors.TextPrimary,
                    focusedBorderColor = TrueSkiesColors.AccentBlue,
                    unfocusedBorderColor = TrueSkiesColors.TextMuted,
                    cursorColor = TrueSkiesColors.AccentBlue,
                    focusedPlaceholderColor = TrueSkiesColors.TextMuted,
                    unfocusedPlaceholderColor = TrueSkiesColors.TextMuted
                )
            )

            Spacer(Modifier.height(TrueSkiesSpacing.sm))

            // Country list
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filteredCountries) { country ->
                    val isSelected = country.name == selectedCountry
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(country.name) }
                            .background(
                                if (isSelected) TrueSkiesColors.AccentBlue.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .padding(
                                horizontal = TrueSkiesSpacing.xl,
                                vertical = TrueSkiesSpacing.md
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            country.flagEmoji,
                            fontSize = 24.sp
                        )
                        Spacer(Modifier.width(TrueSkiesSpacing.md))
                        Text(
                            country.name,
                            style = TrueSkiesTypography.bodyLarge,
                            color = if (isSelected) TrueSkiesColors.AccentBlue else TrueSkiesColors.TextPrimary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = TrueSkiesColors.AccentBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Clear button
            TextButton(
                onClick = { onSelect("") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(TrueSkiesSpacing.sm)
            ) {
                Text("Clear Selection", color = TrueSkiesColors.TextMuted)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Helper components
// ─────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = TrueSkiesTypography.labelMedium,
        color = TrueSkiesColors.TextMuted,
        modifier = Modifier.padding(start = TrueSkiesSpacing.xs, bottom = TrueSkiesSpacing.xxs)
    )
}

@Composable
private fun AccountDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 60.dp),
        color = TrueSkiesColors.GlassHighlight,
        thickness = 0.5.dp
    )
}

@Composable
private fun IconBox(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(TrueSkiesCornerRadius.sm))
            .background(tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun AccountRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    valueColor: Color = TrueSkiesColors.TextSecondary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(TrueSkiesSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBox(icon, iconTint)
        Spacer(Modifier.width(TrueSkiesSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = TrueSkiesTypography.titleMedium, color = TrueSkiesColors.TextPrimary)
            Text(value, style = TrueSkiesTypography.bodySmall, color = valueColor)
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TrueSkiesColors.TextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun StorageBreakdownRow(label: String, size: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 60.dp + TrueSkiesSpacing.md, end = TrueSkiesSpacing.md, top = TrueSkiesSpacing.sm, bottom = TrueSkiesSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = TrueSkiesTypography.bodyMedium,
            color = TrueSkiesColors.TextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            size,
            style = TrueSkiesTypography.bodyMedium,
            color = TrueSkiesColors.TextTertiary
        )
    }
}
