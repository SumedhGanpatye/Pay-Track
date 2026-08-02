package com.sumedh.moneytracker.ui.screens.copypay

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sumedh.moneytracker.MoneyTrackerApp
import com.sumedh.moneytracker.data.ExpenseRepository
import com.sumedh.moneytracker.domain.expense.PaymentPrimaryCategories
import com.sumedh.moneytracker.ui.components.AppScreenBackground
import com.sumedh.moneytracker.ui.components.CustomCategoryDialog
import com.sumedh.moneytracker.ui.components.GlassCard
import com.sumedh.moneytracker.ui.components.UpiAppDropdown
import com.sumedh.moneytracker.ui.theme.BorderEmerald
import com.sumedh.moneytracker.ui.theme.CardBackground
import com.sumedh.moneytracker.ui.theme.Charcoal900
import com.sumedh.moneytracker.ui.theme.ErrorRed
import com.sumedh.moneytracker.ui.theme.NeonTeal
import com.sumedh.moneytracker.ui.theme.SecondaryCard
import com.sumedh.moneytracker.ui.theme.SoftMint
import com.sumedh.moneytracker.ui.theme.TextPrimary
import com.sumedh.moneytracker.ui.theme.TextSecondary
import com.sumedh.moneytracker.util.ExpenseAnalytics

@Composable
fun CopyPayScreen(
    repository: ExpenseRepository,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: CopyPayViewModel = viewModel(
        factory = CopyPayViewModel.factory(
            repository = repository,
            customCategoryStore = (LocalContext.current.applicationContext as MoneyTrackerApp)
                .customCategoryStore,
            upiPreferences = (LocalContext.current.applicationContext as MoneyTrackerApp)
                .upiPreferences
        )
    )
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    var categoryToRemove by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onReturnedFromUpi()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AppScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Copy & Pay",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Copy amount · scan QR in UPI app",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₹",
                    fontWeight = FontWeight.Bold,
                    color = NeonTeal,
                    fontSize = 42.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                BasicTextField(
                    value = state.amountInput,
                    onValueChange = viewModel::onAmountChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 48.sp,
                        textAlign = TextAlign.Start
                    ),
                    cursorBrush = SolidColor(NeonTeal),
                    decorationBox = { inner ->
                        Box {
                            if (state.amountInput.isEmpty()) {
                                Text(
                                    text = "0",
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary.copy(alpha = 0.35f),
                                    fontSize = 48.sp
                                )
                            }
                            inner()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Note (optional)") },
                placeholder = { Text("e.g. coffee, maggi") },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonTeal,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                    focusedLabelColor = NeonTeal,
                    cursorColor = NeonTeal
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Long-press a custom category to remove it",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            state.categoryChips.chunked(2).forEachIndexed { index, rowItems ->
                if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { category ->
                        val isOther = category == PaymentPrimaryCategories.OTHER
                        val isCustom = category in state.customCategories
                        val selected = !isOther &&
                            state.selectedCategory.equals(category, ignoreCase = true)
                        CategoryChip(
                            label = category,
                            selected = selected,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                when {
                                    isOther -> viewModel.onOtherCategoryClicked()
                                    category in PaymentPrimaryCategories.primaries ->
                                        viewModel.onCategorySelected(category)
                                    else -> viewModel.onCustomCategorySelected(category)
                                }
                            },
                            onLongClick = if (isCustom) {
                                { categoryToRemove = category }
                            } else {
                                null
                            }
                        )
                    }
                    repeat(2 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Copy & Pay above Pay with
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = viewModel::copyAndPay,
                    enabled = state.canCopyAndPay,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonTeal,
                        contentColor = Charcoal900,
                        disabledContainerColor = NeonTeal.copy(alpha = 0.25f)
                    )
                ) {
                    Text(
                        text = "₹",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Copy & Pay",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            UpiAppDropdown(
                selected = state.selectedApp,
                onSelect = viewModel::onAppSelected,
                label = "Pay with"
            )

            state.launchError?.let { error ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            GlassCard(cornerRadius = 16.dp, contentPadding = 14.dp, elevated = false) {
                Text(
                    text = "How it works",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonTeal
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "1. Enter amount, note and category\n" +
                        "2. Choose UPI app from the dropdown\n" +
                        "3. Tap Copy & Pay — amount is copied and the app opens\n" +
                        "4. Confirm when you return to add the expense",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (state.showCustomCategoryDialog) {
        CustomCategoryDialog(
            onDismiss = viewModel::dismissCustomCategoryDialog,
            onSave = viewModel::onCustomCategorySaved
        )
    }

    categoryToRemove?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryToRemove = null },
            title = { Text("Remove category?") },
            text = {
                Text(
                    "“$category” will be removed from your custom categories.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeCustomCategory(category)
                        categoryToRemove = null
                    }
                ) {
                    Text("Remove", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToRemove = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (state.showConfirmDialog && state.amount != null) {
        PaymentDoneCard(
            amount = state.amount!!,
            noteLabel = state.note.trim().ifBlank { state.selectedCategory },
            category = state.selectedCategory,
            onNotNow = viewModel::dismissConfirmDialog,
            onSplit = viewModel::onSplitExpenseClicked,
            onConfirm = { viewModel.confirmPaymentSaved(onDone = onSaved) }
        )
    }

    if (state.showSplitOptions && state.amount != null) {
        val amount = state.amount!!
        val half = ((amount / 2.0) * 100.0).toLong() / 100.0
        val third = ((amount / 3.0) * 100.0).toLong() / 100.0
        SplitExpenseCard(
            fullAmount = amount,
            halfAmount = half,
            thirdAmount = third,
            onBack = viewModel::dismissSplitOptions,
            onSplitIn2 = {
                viewModel.confirmPaymentSaved(onDone = onSaved, splitAmong = 2)
            },
            onSplitIn3 = {
                viewModel.confirmPaymentSaved(onDone = onSaved, splitAmong = 3)
            }
        )
    }
}

@Composable
private fun PaymentDoneCard(
    amount: Double,
    noteLabel: String,
    category: String,
    onNotNow: () -> Unit,
    onSplit: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = onNotNow,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(28.dp),
                        spotColor = NeonTeal.copy(alpha = 0.18f),
                        ambientColor = Charcoal900.copy(alpha = 0.4f)
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                SecondaryCard,
                                CardBackground,
                                CardBackground
                            )
                        )
                    )
                    .border(1.dp, BorderEmerald, RoundedCornerShape(28.dp))
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(NeonTeal.copy(alpha = 0.16f))
                        .border(1.5.dp, NeonTeal.copy(alpha = 0.55f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = SoftMint,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Payment done?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Track this spend in your expenses",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(NeonTeal.copy(alpha = 0.08f))
                        .border(1.dp, NeonTeal.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = ExpenseAnalytics.formatInr(amount),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeonTeal
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = noteLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                TextButton(
                    onClick = onNotNow,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Not now", color = TextSecondary, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = onSplit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonTeal.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonTeal)
                ) {
                    Text("Split expense", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonTeal,
                        contentColor = Charcoal900
                    )
                ) {
                    Text("Yes, add it", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SplitExpenseCard(
    fullAmount: Double,
    halfAmount: Double,
    thirdAmount: Double,
    onBack: () -> Unit,
    onSplitIn2: () -> Unit,
    onSplitIn3: () -> Unit
) {
    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(28.dp),
                        spotColor = NeonTeal.copy(alpha = 0.18f),
                        ambientColor = Charcoal900.copy(alpha = 0.4f)
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(SecondaryCard, CardBackground, CardBackground)
                        )
                    )
                    .border(1.dp, BorderEmerald, RoundedCornerShape(28.dp))
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Split expense",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Add only your share of ${ExpenseAnalytics.formatInr(fullAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                SplitOptionRow(
                    title = "Split in 2",
                    subtitle = "Your share",
                    amount = halfAmount,
                    onClick = onSplitIn2
                )
                Spacer(modifier = Modifier.height(10.dp))
                SplitOptionRow(
                    title = "Split in 3",
                    subtitle = "Your share",
                    amount = thirdAmount,
                    onClick = onSplitIn3
                )

                Spacer(modifier = Modifier.height(14.dp))

                TextButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back", color = TextSecondary, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun SplitOptionRow(
    title: String,
    subtitle: String,
    amount: Double,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SecondaryCard)
            .border(1.dp, NeonTeal.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Text(
            text = ExpenseAnalytics.formatInr(amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = NeonTeal
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    val background = if (selected) NeonTeal else SecondaryCard
    val contentColor = if (selected) Charcoal900 else TextSecondary
    val borderColor = if (selected) {
        NeonTeal
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    }

    Box(
        modifier = modifier
            .heightIn(min = 42.dp)
            .clip(shape)
            .background(background)
            .border(1.dp, borderColor, shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor
        )
    }
}
