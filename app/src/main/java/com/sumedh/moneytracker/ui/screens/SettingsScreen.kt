package com.sumedh.moneytracker.ui.screens

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sumedh.moneytracker.MoneyTrackerApp
import com.sumedh.moneytracker.data.ExpenseRepository
import com.sumedh.moneytracker.domain.upi.UpiApp
import com.sumedh.moneytracker.domain.upi.UpiPaymentLauncher
import com.sumedh.moneytracker.ui.components.AppScreenBackground
import com.sumedh.moneytracker.ui.components.GlassCard
import com.sumedh.moneytracker.ui.screens.scanpay.components.payment.PaymentMethodBottomSheet
import com.sumedh.moneytracker.ui.theme.Divider
import com.sumedh.moneytracker.ui.theme.ErrorRed
import com.sumedh.moneytracker.ui.theme.NeonTeal
import com.sumedh.moneytracker.ui.theme.TextPrimary
import com.sumedh.moneytracker.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    repository: ExpenseRepository,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(
            repository = repository,
            customCategoryStore = (LocalContext.current.applicationContext as MoneyTrackerApp)
                .customCategoryStore
        )
    )
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneyTrackerApp
    val paymentPrefs by app.paymentPreferences.state.collectAsStateWithLifecycle()
    val profile by app.userProfileStore.profile.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<ResetType?>(null) }
    var showUpiSheet by remember { mutableStateOf(false) }
    var showEditName by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var saveAsDefault by remember { mutableStateOf(true) }
    val upiApps = remember { UpiPaymentLauncher.installedApps(context) }

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
            }
        }
    }

    AppScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Manage your data",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(18.dp))

            GlassCard {
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonTeal
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEditName = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Display name",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = profile.username.ifBlank { "Not set" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit name",
                        tint = NeonTeal,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            GlassCard {
                Text(
                    text = "Payments",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonTeal
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showUpiSheet = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Default UPI App",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = paymentPrefs.defaultUpiApp.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = "Choose default UPI app",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Divider)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Ask before every payment",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = if (paymentPrefs.askBeforeEveryPayment) {
                                "Always show UPI app picker before paying"
                            } else {
                                "Use default UPI app directly"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = paymentPrefs.askBeforeEveryPayment,
                        onCheckedChange = {
                            app.paymentPreferences.setAskBeforeEveryPayment(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeonTeal,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            GlassCard(contentPadding = 0.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Delete data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ErrorRed
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Removes expenses from this device only.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                HorizontalDivider(color = Divider)

                DeleteRow(
                    label = "Delete todays data",
                    destructive = false,
                    showDivider = true,
                    onClick = { pendingDelete = ResetType.THIS_DAY }
                )
                DeleteRow(
                    label = "Delete this week data",
                    destructive = false,
                    showDivider = true,
                    onClick = { pendingDelete = ResetType.THIS_WEEK }
                )
                DeleteRow(
                    label = "Delete this month data",
                    destructive = false,
                    showDivider = true,
                    onClick = { pendingDelete = ResetType.THIS_MONTH }
                )
                DeleteRow(
                    label = "Clear all data",
                    destructive = true,
                    showDivider = false,
                    onClick = { pendingDelete = ResetType.COMPLETE }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            GlassCard {
                Text(
                    text = "Export",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonTeal
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Share a CSV of every expense stored on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = viewModel::exportCsv,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonTeal,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export CSV", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            GlassCard {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonTeal
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Built with ❤️ by Sumedh Ganpatye",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Just a developer who enjoys building cool things. Pay&Track is one of my projects, created to make tracking everyday expenses easy and effortless.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "© 2026 Sumedh Ganpatye",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary.copy(alpha = 0.85f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            GlassCard {
                Text(
                    text = "Account",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonTeal
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sign out to return to the signup screen. Your expenses stay on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { showLogoutConfirm = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed.copy(alpha = 0.18f),
                        contentColor = ErrorRed
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
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
            text = {
                Text(
                    "You’ll return to the signup screen. Expenses and settings on this device are kept.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        app.userProfileStore.clear()
                    }
                ) {
                    Text("Log out", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showEditName) {
        EditDisplayNameDialog(
            currentName = profile.username,
            onDismiss = { showEditName = false },
            onSave = { name ->
                val saved = app.userProfileStore.setUsername(name)
                if (saved != null) {
                    showEditName = false
                    Toast.makeText(context, "Name updated", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showUpiSheet) {
        PaymentMethodBottomSheet(
            apps = upiApps.ifEmpty { UpiApp.entries },
            selectedApp = paymentPrefs.defaultUpiApp,
            saveAsDefault = saveAsDefault,
            onSaveAsDefaultChange = { saveAsDefault = it },
            onAppSelected = { appSelected ->
                if (saveAsDefault) {
                    app.paymentPreferences.setDefaultUpiApp(appSelected)
                } else {
                    app.paymentPreferences.setDefaultUpiApp(appSelected)
                }
                showUpiSheet = false
            },
            onDismiss = { showUpiSheet = false }
        )
    }

    pendingDelete?.let { type ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(deleteTitle(type)) },
            text = { Text(deleteMessage(type)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.performReset(type)
                        pendingDelete = null
                    }
                ) {
                    Text("Delete", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
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
            Column {
                Text(
                    text = "This name appears in your Home greeting.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { if (it.length <= 24) input = it },
                    singleLine = true,
                    label = { Text("Username") },
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (canSave) onSave(input) }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonTeal,
                        cursorColor = NeonTeal,
                        focusedLabelColor = NeonTeal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(input) },
                enabled = canSave
            ) {
                Text("Save", color = NeonTeal, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun DeleteRow(
    label: String,
    destructive: Boolean,
    showDivider: Boolean,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (destructive) FontWeight.SemiBold else FontWeight.Medium,
                color = if (destructive) ErrorRed else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = if (destructive) ErrorRed.copy(alpha = 0.8f) else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Divider
            )
        }
    }
}

private fun deleteTitle(type: ResetType): String = when (type) {
    ResetType.COMPLETE -> "Clear all data?"
    ResetType.THIS_WEEK -> "Delete this week data?"
    ResetType.THIS_MONTH -> "Delete this month data?"
    ResetType.THIS_DAY -> "Delete todays data?"
}

private fun deleteMessage(type: ResetType): String = when (type) {
    ResetType.COMPLETE -> "This permanently deletes every expense and custom category on this device."
    ResetType.THIS_WEEK -> "Deletes all expenses from Monday through today."
    ResetType.THIS_MONTH -> "Deletes all expenses from the 1st through today."
    ResetType.THIS_DAY -> "Deletes all expenses logged today."
}
