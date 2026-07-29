package com.sumedh.moneytracker.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumedh.moneytracker.domain.expense.PaymentPrimaryCategories
import com.sumedh.moneytracker.ui.components.CustomCategoryDialog
import com.sumedh.moneytracker.ui.components.GlassCard
import com.sumedh.moneytracker.ui.screens.QuickAddUiState
import com.sumedh.moneytracker.ui.theme.Charcoal900
import com.sumedh.moneytracker.ui.theme.ErrorRed
import com.sumedh.moneytracker.ui.theme.NeonTeal
import com.sumedh.moneytracker.ui.theme.SecondaryCard
import com.sumedh.moneytracker.ui.theme.TextPrimary
import com.sumedh.moneytracker.ui.theme.TextSecondary

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickAddCard(
    state: QuickAddUiState,
    onAmountChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onPrimaryCategorySelected: (String) -> Unit,
    onOtherCategoryClicked: () -> Unit,
    onCustomCategorySelected: (String) -> Unit,
    onCustomCategorySaved: (String) -> Unit,
    onDismissCustomCategoryDialog: () -> Unit,
    onRemoveCustomCategory: (String) -> Unit,
    onToggleExpanded: () -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val canSave = state.amountInput.toDoubleOrNull()?.let { it > 0 } == true
    var categoryToRemove by remember { mutableStateOf<String?>(null) }

    GlassCard(
        modifier = modifier,
        cornerRadius = 20.dp,
        contentPadding = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onToggleExpanded),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (state.isEditing) "Edit Expense" else "Add Expense",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = state.todayDisplay,
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonTeal
                )
            }
            Icon(
                imageVector = if (state.isExpanded) {
                    Icons.Outlined.KeyboardArrowUp
                } else {
                    Icons.Outlined.KeyboardArrowDown
                },
                contentDescription = if (state.isExpanded) "Collapse" else "Expand",
                tint = TextSecondary
            )
        }

        AnimatedVisibility(
            visible = state.isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.isEditing) {
                        Button(
                            onClick = onCancelEdit,
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SecondaryCard,
                                contentColor = TextSecondary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Cancel edit",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    OutlinedTextField(
                        value = state.amountInput,
                        onValueChange = onAmountChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Amount") },
                        prefix = {
                            Text(
                                text = "₹ ",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = quickAddFieldColors()
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = onSave,
                        enabled = canSave,
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonTeal,
                            contentColor = Charcoal900,
                            disabledContainerColor = NeonTeal.copy(alpha = 0.25f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = if (state.isEditing) "Update" else "Save",
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = state.notes,
                    onValueChange = onNotesChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Notes (optional)") },
                    shape = RoundedCornerShape(14.dp),
                    colors = quickAddFieldColors()
                )

                Spacer(modifier = Modifier.height(12.dp))

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

                val orderedChips = PaymentPrimaryCategories.primaries +
                    state.customCategories +
                    PaymentPrimaryCategories.OTHER
                orderedChips.chunked(2).forEachIndexed { index, rowItems ->
                    if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { category ->
                            val isOther = category == PaymentPrimaryCategories.OTHER
                            val isCustom = category in state.customCategories
                            val selected =
                                !isOther && state.selectedCategory.equals(category, true)
                            CategoryChip(
                                label = category,
                                selected = selected,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    when {
                                        isOther -> onOtherCategoryClicked()
                                        category in PaymentPrimaryCategories.primaries ->
                                            onPrimaryCategorySelected(category)
                                        else -> onCustomCategorySelected(category)
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
            }
        }
    }

    if (state.showCustomCategoryDialog) {
        CustomCategoryDialog(
            onDismiss = onDismissCustomCategoryDialog,
            onSave = onCustomCategorySaved
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
                        onRemoveCustomCategory(category)
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

@Composable
private fun quickAddFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NeonTeal,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
    focusedLabelColor = NeonTeal,
    cursorColor = NeonTeal
)
