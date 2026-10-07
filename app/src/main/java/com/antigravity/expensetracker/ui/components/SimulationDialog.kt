package com.antigravity.expensetracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PresetNotification(
    val label: String,
    val packageName: String,
    val title: String,
    val content: String
)

@Composable
fun SimulationDialog(
    onDismiss: () -> Unit,
    onSimulate: (packageName: String, title: String, content: String) -> Unit
) {
    val presets = remember {
        listOf(
            PresetNotification(
                label = "Starbucks $25.50",
                packageName = "com.chase.sig.android",
                title = "Chase Alert",
                content = "Paid $25.50 to Starbucks"
            ),
            PresetNotification(
                label = "Uber $12.00",
                packageName = "com.wf.wellsfargomobile",
                title = "Wells Fargo",
                content = "Debited USD 12.00 at Uber"
            ),
            PresetNotification(
                label = "Deposit $500",
                packageName = "com.chase.sig.android",
                title = "Chase Alert",
                content = "Received $500.00 from John"
            ),
            PresetNotification(
                label = "Circle K 50k VND",
                packageName = "com.VCB",
                title = "Vietcombank",
                content = "TK 4242 -50,000 VND tai Circle K"
            ),
            PresetNotification(
                label = "Highlands 65k",
                packageName = "com.mservice.momopay",
                title = "MoMo",
                content = "MoMo: Ban da thanh toan 65.000d tai HighLands Coffee"
            ),
            PresetNotification(
                label = "Ignore Promo Noise",
                packageName = "com.grabtaxi.passenger",
                title = "Grab Promo",
                content = "Nhap ma PROMO50 de duoc discount 50% cho chuyen di tiep theo!"
            )
        )
    }

    var selectedPackage by remember { mutableStateOf("com.chase.sig.android") }
    var selectedTitle by remember { mutableStateOf("Chase Alert") }
    var selectedContent by remember { mutableStateOf("Paid $25.50 to Starbucks") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Simulate Bank Notification", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Quick Presets:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presets) { preset ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.clickable {
                                selectedPackage = preset.packageName
                                selectedTitle = preset.title
                                selectedContent = preset.content
                            }
                        ) {
                            Text(
                                text = preset.label,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = selectedPackage,
                    onValueChange = { selectedPackage = it },
                    label = { Text("App Package Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = selectedTitle,
                    onValueChange = { selectedTitle = it },
                    label = { Text("Notification Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = selectedContent,
                    onValueChange = { selectedContent = it },
                    label = { Text("Notification Content / BigText") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSimulate(selectedPackage, selectedTitle, selectedContent)
                    onDismiss()
                }
            ) {
                Text("Process & Ingest")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
