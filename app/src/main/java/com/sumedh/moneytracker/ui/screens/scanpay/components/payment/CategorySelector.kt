package com.sumedh.moneytracker.ui.screens.scanpay.components.payment

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumedh.moneytracker.domain.expense.PaymentPrimaryCategories
import com.sumedh.moneytracker.ui.theme.Charcoal900
import com.sumedh.moneytracker.ui.theme.ErrorRed
import com.sumedh.moneytracker.ui.theme.NeonTeal
import com.sumedh.moneytracker.ui.theme.SecondaryCard
import com.sumedh.moneytracker.ui.theme.TextPrimary
import com.sumedh.moneytracker.ui.theme.TextSecondary

private val ChipShape = RoundedCornerShape(16.dp)
private val ChipMinHeight = 48.dp
private const val ChipsPerRow = 2

@Composable
fun CategorySelector(
    selectedCategory: String?,
    customCategories: List<String>,
    onPrimarySelected: (String) -> Unit,
    onOtherSelected: () -> Unit,
    onCustomSelected: (String) -> Unit,
    shake: Boolean,
    modifier: Modifier = Modifier
) {
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(shake) {
        if (!shake) return@LaunchedEffect
        repeat(3) {
            shakeOffset.animateTo(8f, tween(36))
            shakeOffset.animateTo(-8f, tween(36))
        }
        shakeOffset.animateTo(0f, tween(36))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .offset(x = shakeOffset.value.dp)
            .semantics { contentDescription = "Category selector" }
    ) {
        Text(
            text = "Category",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        if (shake) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Select a category",
                style = MaterialTheme.typography.bodySmall,
                color = ErrorRed
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        val orderedChips = remember(customCategories) {
            PaymentPrimaryCategories.primaries + customCategories + PaymentPrimaryCategories.OTHER
        }
        orderedChips.chunked(ChipsPerRow).forEachIndexed { index, rowItems ->
            if (index > 0) Spacer(modifier = Modifier.height(8.dp))
            CategoryChipRow {
                rowItems.forEach { label ->
                    val isOther = label == PaymentPrimaryCategories.OTHER
                    EqualCategoryChip(
                        label = label,
                        selected = if (isOther) {
                            false
                        } else {
                            selectedCategory.equals(label, true)
                        },
                        onClick = {
                            when {
                                isOther -> onOtherSelected()
                                label in PaymentPrimaryCategories.primaries -> onPrimarySelected(label)
                                else -> onCustomSelected(label)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = ChipMinHeight)
                    )
                }
                repeat(ChipsPerRow - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryChipRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
private fun EqualCategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = {
            Text(
                text = label,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        },
        shape = ChipShape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = NeonTeal,
            selectedLabelColor = Charcoal900,
            containerColor = SecondaryCard,
            labelColor = TextSecondary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            selectedBorderColor = NeonTeal
        )
    )
}

@Composable
fun CustomCategoryDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Category Name") },
        text = {
            Column {
                Text(
                    text = "Examples: Books, Gym, Coffee, Medicines",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { if (it.length <= 24) input = it },
                    singleLine = true,
                    placeholder = { Text("Category name") },
                    shape = RoundedCornerShape(14.dp),
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
                enabled = input.trim().isNotEmpty()
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
