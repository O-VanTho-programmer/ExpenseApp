package com.antigravity.expensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CATEGORY_COLORS = listOf(
    "#E53935", // Red
    "#FB8C00", // Orange
    "#FBC02D", // Yellow / Amber
    "#43A047", // Green
    "#00897B", // Teal
    "#00ACC1", // Cyan
    "#1E88E5", // Blue
    "#3949AB", // Indigo
    "#8E24AA", // Purple
    "#D81B60", // Pink
    "#6D4C41", // Brown
    "#607D8B"  // Slate / Blue Grey
)

@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onAddCategory: (name: String, type: String, iconKey: String?, colorHex: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("EXPENSE") }
    var selectedColor by remember { mutableStateOf(CATEGORY_COLORS[0]) }
    var showErrors by remember { mutableStateOf(false) }

    val isNameValid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Category", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Category Type Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == "EXPENSE",
                        onClick = { selectedType = "EXPENSE" },
                        label = { Text("Expense") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedType == "INCOME",
                        onClick = { selectedType = "INCOME" },
                        label = { Text("Income") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name *") },
                    placeholder = { Text("e.g. Health, Coffee, Gym, Freelance") },
                    singleLine = true,
                    isError = showErrors && !isNameValid,
                    supportingText = {
                        if (showErrors && !isNameValid) {
                            Text("Category name is required", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Color Selection
                Text(
                    text = "Category Color",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(CATEGORY_COLORS) { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = selectedColor == hex

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else Modifier
                                )
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isNameValid) {
                        onAddCategory(
                            name.trim(),
                            selectedType,
                            null,
                            selectedColor
                        )
                        onDismiss()
                    } else {
                        showErrors = true
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
