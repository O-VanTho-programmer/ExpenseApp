package com.antigravity.expensetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.expensetracker.data.parser.ParsedOutput
import com.antigravity.expensetracker.data.parser.TransactionParserEngine
import com.antigravity.expensetracker.ui.components.PresetNotification
import com.antigravity.expensetracker.ui.viewmodel.MainViewModel

@Composable
fun SimulationScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val parserEngine = remember { TransactionParserEngine() }

    var inputPackage by remember { mutableStateOf("com.chase.sig.android") }
    var inputTitle by remember { mutableStateOf("Chase Alert") }
    var inputContent by remember { mutableStateOf("Paid $25.50 to Starbucks") }
    var parsedOutput by remember { mutableStateOf<ParsedOutput?>(null) }
    var isNoiseDetected by remember { mutableStateOf(false) }
    var lastStatusMessage by remember { mutableStateOf<String?>(null) }

    val presets = remember {
        listOf(
            PresetNotification("Chase $25.50", "com.chase.sig.android", "Chase Alert", "Paid $25.50 to Starbucks"),
            PresetNotification("Wells Fargo $12", "com.wf.wellsfargomobile", "Wells Fargo", "Debited USD 12.00 at Uber"),
            PresetNotification("Deposit $1250", "com.chase.sig.android", "Chase Alert", "Deposit of $1,250.00 into account ...4242"),
            PresetNotification("VCB -50k VND", "com.VCB", "Vietcombank", "TK 4242 -50,000 VND tai Circle K"),
            PresetNotification("MoMo 65k", "com.mservice.momopay", "MoMo", "MoMo: Ban da thanh toan 65.000d tai HighLands Coffee"),
            PresetNotification("MB Bank +2M", "com.mbbank", "MB Bank", "TK 9876 +2.000.000 VND tu CONG TY ABC"),
            PresetNotification("P2P Sarah $50", "com.venmo", "Venmo", "Sarah sent you $50.00"),
            PresetNotification("OTP Noise", "com.google.android.apps.messaging", "Chase", "Your one-time passcode is 492019. Do not share.")
        )
    }

    // Run test parse on text change
    fun testParse() {
        val full = "$inputTitle $inputContent".trim()
        isNoiseDetected = parserEngine.isNoise(full)
        parsedOutput = parserEngine.parse(full, System.currentTimeMillis())
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ingestion & Parser Workbench",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Test notifications against regex classification, currency normalization, and noise filters.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        item {
            Text("Presets:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(presets) { preset ->
                    FilterChip(
                        selected = inputContent == preset.content,
                        onClick = {
                            inputPackage = preset.packageName
                            inputTitle = preset.title
                            inputContent = preset.content
                            testParse()
                        },
                        label = { Text(preset.label) }
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = inputPackage,
                onValueChange = { inputPackage = it },
                label = { Text("Package Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = inputTitle,
                onValueChange = {
                    inputTitle = it
                    testParse()
                },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = inputContent,
                onValueChange = {
                    inputContent = it
                    testParse()
                },
                label = { Text("Content / BigText") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Real-time Parser Output Box
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Real-time Parser Inspection:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (isNoiseDetected) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "⛔ NOISE FILTER TRIGGERED: Non-financial / OTP / Promo notice dropped.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else if (parsedOutput != null) {
                        val out = parsedOutput!!
                        Text(
                            text = """
                                • Status: MATCHED & NORMALIZED
                                • Amount: ${out.amount}
                                • Currency: ${out.currency}
                                • Direction: ${out.type}
                                • Counterparty: ${out.counterparty}
                                • Account Mask: ${out.accountIdentifier ?: "Default"}
                            """.trimIndent(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text(
                            text = "Pattern not recognized or no financial signal found.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    viewModel.simulateNotification(inputPackage, inputTitle, inputContent)
                    lastStatusMessage = "Ingestion event dispatched to repository!"
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = parsedOutput != null && !isNoiseDetected
            ) {
                Text("Ingest into Database & Test Deduplication")
            }

            if (lastStatusMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = lastStatusMessage!!,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }

        item {
            // Deduplication explanation card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "💡 Transfer Deduplication Test Guide",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "To test deduplication: Send a DEBIT of $100, then within 3 minutes send a CREDIT of $100. The engine automatically unifies both into a single TRANSFER transaction!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
