package com.sumedh.moneytracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sumedh.moneytracker.MoneyTrackerApp
import com.sumedh.moneytracker.data.ParsedNotificationEntity
import com.sumedh.moneytracker.ui.components.AppScreenBackground
import com.sumedh.moneytracker.ui.components.GlassCard
import com.sumedh.moneytracker.ui.theme.NeonTeal
import com.sumedh.moneytracker.ui.theme.TextPrimary
import com.sumedh.moneytracker.ui.theme.TextSecondary
import com.sumedh.moneytracker.util.ExpenseAnalytics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDebugScreen(
    onBack: () -> Unit,
    viewModel: NotificationDebugViewModel = viewModel(
        factory = NotificationDebugViewModel.factory(
            (LocalContext.current.applicationContext as MoneyTrackerApp).notificationDebugRepository
        )
    )
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()

    AppScreenBackground {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Notification Debug",
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        titleContentColor = TextPrimary,
                        navigationIconContentColor = NeonTeal
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "Last ${entries.size} parsed notifications",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(entries, key = { it.id }) { entry ->
                    DebugEntryCard(entry = entry)
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun DebugEntryCard(entry: ParsedNotificationEntity) {
    val timeLabel = rememberTimestamp(entry.timestamp)
    GlassCard(cornerRadius = 16.dp, contentPadding = 14.dp) {
        Text(
            text = timeLabel,
            style = MaterialTheme.typography.labelMedium,
            color = NeonTeal,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        DebugField("Original", entry.originalText)
        DebugField("Amount", entry.extractedAmount?.let { ExpenseAnalytics.formatInr(it) } ?: "—")
        DebugField("Person", entry.extractedPerson ?: "—")
        DebugField("Group", entry.extractedGroup ?: "—")
        DebugField("Parser", entry.parserUsed)
    }
}

@Composable
private fun DebugField(label: String, value: String) {
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = TextSecondary
    )
    Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        color = TextPrimary,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun rememberTimestamp(millis: Long): String {
    val fmt = SimpleDateFormat("d MMM yyyy, HH:mm:ss", Locale.ENGLISH)
    return fmt.format(Date(millis))
}
