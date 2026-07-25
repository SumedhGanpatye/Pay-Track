package com.sumedh.moneytracker.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumedh.moneytracker.domain.expense.PaymentPrimaryCategories
import com.sumedh.moneytracker.ui.components.pressableScale
import com.sumedh.moneytracker.ui.icons.AppIcons
import com.sumedh.moneytracker.ui.screens.QuickAddUiState
import com.sumedh.moneytracker.ui.screens.scanpay.components.payment.CustomCategoryDialog
import com.sumedh.moneytracker.ui.theme.BorderEmerald
import com.sumedh.moneytracker.ui.theme.CardBackground
import com.sumedh.moneytracker.ui.theme.Charcoal900
import com.sumedh.moneytracker.ui.theme.NeonTeal
import com.sumedh.moneytracker.ui.theme.SecondaryCard
import com.sumedh.moneytracker.ui.theme.TextPrimary
import com.sumedh.moneytracker.ui.theme.TextSecondary

@Composable
fun QuickAddCard(
    state: QuickAddUiState,
    onAmountChange: (String) -> Unit,
    onPrimaryCategorySelected: (String) -> Unit,
    onOtherCategoryClicked: () -> Unit,
    onCustomCategorySelected: (String) -> Unit,
    onCustomCategorySaved: (String) -> Unit,
    onDismissCustomCategoryDialog: () -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val shape = RoundedCornerShape(16.dp)

    Surface(
        onClick = { if (!expanded) expanded = true },
        modifier = modifier
            .fillMaxWidth()
            .pressableScale(pressedScale = 0.985f, enabled = !expanded),
        shape = shape,
        color = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        listOf(SecondaryCard, CardBackground)
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    shape = shape
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Quick Add",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Add expense manually",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) {
                            AppIcons.ExpandLess
                        } else {
                            AppIcons.ExpandMore
                        },
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = state.todayDisplay,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonTeal
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                            shape = RoundedCornerShape(16.dp),
                            colors = quickAddFieldColors()
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        IconButton(
                            onClick = onSave,
                            enabled = state.amountInput.toDoubleOrNull()?.let { it > 0 } == true,
                            modifier = Modifier
                                .size(52.dp)
                                .pressableScale(
                                    enabled = state.amountInput.toDoubleOrNull()?.let { it > 0 } == true
                                )
                                .clip(CircleShape),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = NeonTeal,
                                contentColor = Charcoal900,
                                disabledContainerColor = NeonTeal.copy(alpha = 0.25f),
                                disabledContentColor = TextPrimary.copy(alpha = 0.4f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Save expense"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = state.note,
                        onValueChange = onNoteChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Note (optional)") },
                        shape = RoundedCornerShape(16.dp),
                        colors = quickAddFieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val chipMinHeight = 44.dp
                    val orderedChips = remember(state.customCategories) {
                        PaymentPrimaryCategories.primaries +
                            state.customCategories +
                            PaymentPrimaryCategories.OTHER
                    }
                    orderedChips.chunked(2).forEachIndexed { index, rowItems ->
                        if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { category ->
                                val isOther = category == PaymentPrimaryCategories.OTHER
                                val selected = if (isOther) {
                                    false
                                } else {
                                    state.selectedCategory.equals(category, true)
                                }
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        when {
                                            isOther -> onOtherCategoryClicked()
                                            category in PaymentPrimaryCategories.primaries ->
                                                onPrimaryCategorySelected(category)
                                            else -> onCustomCategorySelected(category)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = chipMinHeight),
                                    label = {
                                        Text(
                                            text = category,
                                            modifier = Modifier.fillMaxWidth(),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            fontWeight = if (selected) {
                                                FontWeight.Bold
                                            } else {
                                                FontWeight.Medium
                                            }
                                        )
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeonTeal,
                                        selectedLabelColor = Charcoal900,
                                        containerColor = SecondaryCard,
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = MaterialTheme.colorScheme.outline.copy(
                                            alpha = 0.4f
                                        ),
                                        selectedBorderColor = NeonTeal
                                    )
                                )
                            }
                            repeat(2 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
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
}

@Composable
private fun quickAddFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NeonTeal,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
    focusedLabelColor = NeonTeal,
    cursorColor = NeonTeal
)
