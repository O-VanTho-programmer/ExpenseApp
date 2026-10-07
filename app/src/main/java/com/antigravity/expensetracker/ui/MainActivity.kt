package com.antigravity.expensetracker.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModelProvider
import com.antigravity.expensetracker.ExpenseTrackerApp
import com.antigravity.expensetracker.ui.screens.AccountsScreen
import com.antigravity.expensetracker.ui.screens.CategoriesScreen
import com.antigravity.expensetracker.ui.screens.DashboardScreen
import com.antigravity.expensetracker.ui.screens.SimulationScreen
import com.antigravity.expensetracker.ui.theme.ExpenseTrackerTheme
import com.antigravity.expensetracker.ui.viewmodel.MainViewModel
import com.antigravity.expensetracker.ui.viewmodel.ViewModelFactory

sealed class NavTab(val title: String, val icon: ImageVector) {
    object Dashboard : NavTab("Dashboard", Icons.Default.Dashboard)
    object Accounts : NavTab("Accounts", Icons.Default.AccountBalance)
    object Categories : NavTab("Categories", Icons.Default.Category)
    object Simulator : NavTab("Simulator", Icons.Default.Science)
}

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as ExpenseTrackerApp
        viewModel = ViewModelProvider(this, ViewModelFactory(app))[MainViewModel::class.java]

        setContent {
            ExpenseTrackerTheme {
                var currentTab by remember { mutableStateOf<NavTab>(NavTab.Dashboard) }
                var showSettingsDialog by remember { mutableStateOf(false) }
                val defaultCurrency by viewModel.defaultCurrency.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = currentTab.title,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                actions = {
                                    androidx.compose.material3.IconButton(
                                        onClick = { showSettingsDialog = true }
                                    ) {
                                        Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                                            contentDescription = "General Settings"
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        },
                        bottomBar = {
                            val items = listOf(
                                NavTab.Dashboard,
                                NavTab.Accounts,
                                NavTab.Categories,
                                NavTab.Simulator
                            )
                            NavigationBar {
                                items.forEach { tab ->
                                    NavigationBarItem(
                                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                                        label = { Text(tab.title) },
                                        selected = currentTab == tab,
                                        onClick = { currentTab = tab }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        val modifier = Modifier.padding(innerPadding)
                        when (currentTab) {
                            NavTab.Dashboard -> DashboardScreen(viewModel = viewModel, modifier = modifier)
                            NavTab.Accounts -> AccountsScreen(viewModel = viewModel, modifier = modifier)
                            NavTab.Categories -> CategoriesScreen(viewModel = viewModel, modifier = modifier)
                            NavTab.Simulator -> SimulationScreen(viewModel = viewModel, modifier = modifier)
                        }
                    }

                    if (showSettingsDialog) {
                        com.antigravity.expensetracker.ui.components.GeneralSettingsDialog(
                            currentCurrency = defaultCurrency,
                            onDismiss = { showSettingsDialog = false },
                            onSaveCurrency = { viewModel.setDefaultCurrency(it) }
                        )
                    }
                }
            }
        }
    }
}
