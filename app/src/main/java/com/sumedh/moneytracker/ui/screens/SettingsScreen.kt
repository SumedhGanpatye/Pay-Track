package com.sumedh.moneytracker.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sumedh.moneytracker.BuildConfig
import com.sumedh.moneytracker.MoneyTrackerApp
import com.sumedh.moneytracker.data.ExpenseRepository
import com.sumedh.moneytracker.domain.notification.NotificationPermissionManager
import com.sumedh.moneytracker.ui.components.AppScreenBackground
import com.sumedh.moneytracker.ui.components.GlassCard
import com.sumedh.moneytracker.ui.components.ScreenHeader
import com.sumedh.moneytracker.ui.components.UpiAppDropdown
import com.sumedh.moneytracker.ui.theme.Divider
import com.sumedh.moneytracker.ui.theme.ErrorRed
import com.sumedh.moneytracker.ui.theme.NeonTeal
import com.sumedh.moneytracker.ui.theme.TextPrimary
import com.sumedh.moneytracker.ui.theme.TextSecondary
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    repository: ExpenseRepository,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(
            repository = repository,
            customCategoryStore = (LocalContext.current.applicationContext as MoneyTrackerApp)
                .customCategoryStore,
            debugRepository = (LocalContext.current.applicationContext as MoneyTrackerApp)
                .notificationDebugRepository
        )
    )
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneyTrackerApp
    val profile by app.userProfileStore.profile.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<ResetType?>(null) }
    var showEditName by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var notificationEnabled by remember {
        mutableStateOf(NotificationPermissionManager.isNotificationListenerEnabled(context))
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val csv = BufferedReader(InputStreamReader(stream)).readText()
                viewModel.importCsv(csv)
            }
        }.onFailure {
            Toast.makeText(context, "Import failed", Toast.LENGTH_SHORT).show()
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            val dbFile = context.getDatabasePath("money_tracker.db")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dbFile.outputStream().use { output -> input.copyTo(output) }
            }
            Toast.makeText(context, "Database restored. Restart the app.", Toast.LENGTH_LONG).show()
        }.onFailure {
            Toast.makeText(context, "Restore failed", Toast.LENGTH_SHORT).show()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationEnabled =
                    NotificationPermissionManager.isNotificationListenerEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.Message -> {
                    Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
                }
                is SettingsEvent.ExportReady -> {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_SUBJECT, "Pay&Track export")
                        putExtra(Intent.EXTRA_TEXT, event.csv)
                    }
                    context.startActivity(Intent.createChooser(share, "Export expenses"))
                }
                is SettingsEvent.BackupReady -> { /* handled inline */ }
            }
        }
    }

    AppScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            ScreenHeader(
                title = "Settings",
                subtitle = "Manage your data & notifications"
            )

            Spacer(modifier = Modifier.height(18.dp))

            GlassCard {
                Text("Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = NeonTeal)
                Spacer(modifier = Modifier.height(14.dp))
                SettingsRow(
                    title = "Display name",
                    subtitle = profile.username.ifBlank { "Not set" },
                    onClick = { showEditName = true },
                    trailing = {
                        Icon(Icons.Outlined.Edit, "Edit", tint = NeonTeal, modifier = Modifier.size(22.dp))
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            GlassCard {
                Text("Notification Monitoring", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = NeonTeal)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Listens only to Google Pay notifications for split expense detection.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                SettingsRow(
                    title = "Notification Permission",
                    subtitle = if (notificationEnabled) "Enabled" else "Not enabled",
                    subtitleColor = if (notificationEnabled) NeonTeal else TextSecondary,
                    onClick = {
                        NotificationPermissionManager.openNotificationAccessSettings(context)
                    }
                )
                HorizontalDivider(color = Divider, modifier = Modifier.padding(vertical = 8.dp))
                SettingsRow(
                    title = "Enable Notification Access",
                    subtitle = "Open system notification listener settings",
                    onClick = {
                        NotificationPermissionManager.openNotificationAccessSettings(context)
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            GlassCard {
                Text(
                    "Payments",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonTeal
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Default UPI app for Copy & Pay. Icons are loaded from apps installed on your phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                val defaultUpi by app.upiPreferences.defaultApp.collectAsStateWithLifecycle()
                UpiAppDropdown(
                    selected = defaultUpi,
                    onSelect = { app.upiPreferences.setDefault(it) },
                    label = "Default UPI app"
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            GlassCard {
                Text("Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = NeonTeal)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = viewModel::exportCsv, modifier = Modifier.fillMaxWidth(), colors = primaryButtonColors(), shape = RoundedCornerShape(16.dp)) {
                    Text("Export Expenses", fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { importLauncher.launch(arrayOf("text/*", "text/csv")) }, modifier = Modifier.fillMaxWidth(), colors = secondaryButtonColors(), shape = RoundedCornerShape(16.dp)) {
                    Text("Import Expenses", fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { backupDatabase(context) }, modifier = Modifier.fillMaxWidth(), colors = secondaryButtonColors(), shape = RoundedCornerShape(16.dp)) {
                    Text("Backup Database", fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { restoreLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth(), colors = secondaryButtonColors(), shape = RoundedCornerShape(16.dp)) {
                    Text("Restore Database", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            GlassCard(contentPadding = 0.dp) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                    Text("Delete data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = ErrorRed)
                    Text("Removes expenses from this device only.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                HorizontalDivider(color = Divider)
                DeleteRow("Delete todays data", false, true) { pendingDelete = ResetType.THIS_DAY }
                DeleteRow("Delete this week data", false, true) { pendingDelete = ResetType.THIS_WEEK }
                DeleteRow("Delete this month data", false, true) { pendingDelete = ResetType.THIS_MONTH }
                DeleteRow("Clear all data", true, false) { pendingDelete = ResetType.COMPLETE }
            }

            Spacer(modifier = Modifier.height(14.dp))

            GlassCard {
                Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = NeonTeal)
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(title = "Privacy Policy", subtitle = "How your data is handled", onClick = { showPrivacy = true })
                HorizontalDivider(color = Divider, modifier = Modifier.padding(vertical = 8.dp))
                SettingsRow(title = "Version", subtitle = BuildConfig.VERSION_NAME)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Built with ❤️ by Sumedh Ganpatye", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("Pay&Track — automatic Google Pay split expense detection.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }

            Spacer(modifier = Modifier.height(14.dp))

            GlassCard {
                Text("Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = NeonTeal)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Sign out to return to the signup screen.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(14.dp))
                Button(onClick = { showLogoutConfirm = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = ErrorRed.copy(alpha = 0.18f), contentColor = ErrorRed), shape = RoundedCornerShape(16.dp)) {
                    Text("Log out", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Log out?") },
            text = { Text("You'll return to the signup screen.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showLogoutConfirm = false; app.userProfileStore.clear() }) {
                    Text("Log out", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancel") } }
        )
    }

    if (showEditName) {
        EditDisplayNameDialog(
            currentName = profile.username,
            onDismiss = { showEditName = false },
            onSave = { name ->
                if (app.userProfileStore.setUsername(name) != null) {
                    showEditName = false
                    Toast.makeText(context, "Name updated", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = { Text("Privacy Policy") },
            text = {
                Text(
                    "Pay&Track stores all expense data locally on your device. " +
                        "Notification access is used only to read Google Pay split request notifications. " +
                        "No data is sent to external servers.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacy = false }) {
                    Text("Close", color = NeonTeal)
                }
            }
        )
    }

    pendingDelete?.let { type ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(deleteTitle(type)) },
            text = { Text(deleteMessage(type)) },
            confirmButton = {
                TextButton(onClick = { viewModel.performReset(type); pendingDelete = null }) {
                    Text("Delete", color = ErrorRed)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    subtitleColor: Color = TextSecondary,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = subtitleColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun EditDisplayNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var input by remember(currentName) { mutableStateOf(currentName) }
    val canSave = input.trim().length >= 2
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit display name") },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { if (it.length <= 24) input = it },
                singleLine = true,
                label = { Text("Username") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (canSave) onSave(input) }),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonTeal, cursorColor = NeonTeal),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(input) }, enabled = canSave) {
                Text("Save", color = NeonTeal, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DeleteRow(label: String, destructive: Boolean, showDivider: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = if (destructive) FontWeight.SemiBold else FontWeight.Medium, color = if (destructive) ErrorRed else TextPrimary, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = if (destructive) ErrorRed.copy(0.8f) else TextSecondary, modifier = Modifier.size(22.dp))
        }
        if (showDivider) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = Divider)
    }
}

@Composable
private fun primaryButtonColors() = ButtonDefaults.buttonColors(containerColor = NeonTeal, contentColor = TextPrimary)

@Composable
private fun secondaryButtonColors() = ButtonDefaults.buttonColors(containerColor = NeonTeal.copy(alpha = 0.14f), contentColor = NeonTeal)

private fun deleteTitle(type: ResetType) = when (type) {
    ResetType.COMPLETE -> "Clear all data?"
    ResetType.THIS_WEEK -> "Delete this week data?"
    ResetType.THIS_MONTH -> "Delete this month data?"
    ResetType.THIS_DAY -> "Delete todays data?"
}

private fun deleteMessage(type: ResetType) = when (type) {
    ResetType.COMPLETE -> "Permanently deletes every expense and custom category on this device."
    ResetType.THIS_WEEK -> "Deletes all expenses from Monday through today."
    ResetType.THIS_MONTH -> "Deletes all expenses from the 1st through today."
    ResetType.THIS_DAY -> "Deletes all expenses logged today."
}

private fun backupDatabase(context: android.content.Context) {
    val dbFile = context.getDatabasePath("money_tracker.db")
    if (!dbFile.exists()) {
        Toast.makeText(context, "No database to backup", Toast.LENGTH_SHORT).show()
        return
    }
    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val bytes = dbFile.readBytes()
    val share = Intent(Intent.ACTION_SEND).apply {
        type = "application/octet-stream"
        putExtra(Intent.EXTRA_STREAM, uriFromBytes(context, bytes, "money_tracker_$stamp.db"))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(share, "Backup database"))
}

private fun uriFromBytes(context: android.content.Context, bytes: ByteArray, fileName: String): Uri {
    val cacheFile = File(context.cacheDir, fileName)
    cacheFile.writeBytes(bytes)
    return androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        cacheFile
    )
}
