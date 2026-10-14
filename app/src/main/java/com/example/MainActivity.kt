package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AlertCondition
import com.example.ui.MainViewModel
import com.example.ui.screens.AlertsScreen
import com.example.ui.screens.MarketOverviewScreen
import com.example.ui.screens.MarketSentimentScreen
import com.example.ui.screens.SectoralMoversScreen
import com.example.ui.screens.StockDetailScreen
import com.example.ui.screens.WatchlistScreen
import com.example.ui.theme.CardBorderDark
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

enum class MainTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    MARKETS("Markets", Icons.Filled.ShowChart, Icons.Outlined.ShowChart, "tab_markets"),
    SENTIMENT("Sentiment", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "tab_sentiment"),
    SECTORS("Sectors", Icons.Filled.BarChart, Icons.Outlined.BarChart, "tab_sectors"),
    WATCHLISTS("Watchlists", Icons.Filled.FormatListBulleted, Icons.Outlined.FormatListBulleted, "tab_watchlists"),
    ALERTS("Alerts", Icons.Filled.Notifications, Icons.Outlined.Notifications, "tab_alerts")
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialSymbol = intent?.getStringExtra("EXTRA_NAVIGATE_SYMBOL")

        setContent {
            MyApplicationTheme(darkTheme = true) {
                DalalPulseApp(
                    viewModel = viewModel,
                    initialSymbol = initialSymbol
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun DalalPulseApp(
    viewModel: MainViewModel,
    initialSymbol: String? = null
) {
    var currentTab by remember { mutableStateOf(MainTab.MARKETS) }
    var selectedStockSymbol by remember { mutableStateOf(initialSymbol) }
    var alertPrefillSymbol by remember { mutableStateOf<String?>(null) }

    val stocks by viewModel.stocks.collectAsStateWithLifecycle()
    val indices by viewModel.indices.collectAsStateWithLifecycle()
    val sectors by viewModel.sectors.collectAsStateWithLifecycle()
    val isLiveActive by viewModel.isLiveSimulationActive.collectAsStateWithLifecycle()
    val watchlists by viewModel.watchlists.collectAsStateWithLifecycle()
    val activeAlerts by viewModel.activeAlerts.collectAsStateWithLifecycle()
    val triggeredAlerts by viewModel.triggeredAlerts.collectAsStateWithLifecycle()
    val sentimentReport by viewModel.sentimentReport.collectAsStateWithLifecycle()
    val isNewsLoading by viewModel.isNewsLoading.collectAsStateWithLifecycle()
    val newsErrorMessage by viewModel.newsErrorMessage.collectAsStateWithLifecycle()
    val rateLimitResumeTime by viewModel.rateLimitResumeTime.collectAsStateWithLifecycle()
    val isInternetSyncing by viewModel.isInternetSyncing.collectAsStateWithLifecycle()
    val lastInternetSyncTime by viewModel.lastInternetSyncTime.collectAsStateWithLifecycle()
    val isInternetLiveConnected by viewModel.isInternetLiveConnected.collectAsStateWithLifecycle()

    val marketStatus = remember(isLiveActive) { viewModel.getMarketStatusInfo() }

    // Combine all watched symbols across watchlists
    val watchedSymbolsSet = remember(watchlists) {
        watchlists.flatMap { it.symbolsCsv.split(",") }
            .filter { it.isNotBlank() }
            .toSet()
    }

    // Detail Screen Navigation BackHandler
    BackHandler(enabled = selectedStockSymbol != null) {
        selectedStockSymbol = null
    }

    val selectedStock = remember(selectedStockSymbol, stocks) {
        if (selectedStockSymbol == null) null
        else stocks.firstOrNull { it.symbol == selectedStockSymbol }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (selectedStock == null) {
                NavigationBar(
                    containerColor = SurfaceDark,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .background(SurfaceDark)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    MainTab.values().forEach { tab ->
                        val isSelected = currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = {
                                if (tab == MainTab.ALERTS && activeAlerts.isNotEmpty()) {
                                    BadgedBox(badge = {
                                        Badge(
                                            containerColor = Color(0xFFEF4444),
                                            contentColor = Color.White
                                        ) {
                                            Text("${activeAlerts.size}")
                                        }
                                    }) {
                                        Icon(
                                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.title
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = TextSecondaryDark,
                                unselectedTextColor = TextSecondaryDark
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0A0E17))
        ) {
            if (selectedStock != null) {
                StockDetailScreen(
                    stock = selectedStock,
                    isInWatchlist = watchedSymbolsSet.contains(selectedStock.symbol),
                    onBackClick = { selectedStockSymbol = null },
                    onWatchlistToggle = {
                        viewModel.toggleStockInDefaultWatchlist(selectedStock.symbol)
                    },
                    onSetAlertClick = {
                        alertPrefillSymbol = selectedStock.symbol
                        currentTab = MainTab.ALERTS
                        selectedStockSymbol = null
                    }
                )
            } else {
                when (currentTab) {
                    MainTab.MARKETS -> {
                        MarketOverviewScreen(
                            indices = indices,
                            stocks = stocks,
                            sectors = sectors,
                            statusInfo = marketStatus,
                            isSimulationActive = isLiveActive,
                            watchlistSymbols = watchedSymbolsSet,
                            activeAlertsCount = activeAlerts.size,
                            sentimentReport = sentimentReport,
                            isInternetSyncing = isInternetSyncing,
                            lastSyncTime = lastInternetSyncTime,
                            isInternetLive = isInternetLiveConnected,
                            onToggleSimulation = { viewModel.toggleSimulation() },
                            onManualSync = { viewModel.syncLiveInternetMarketData() },
                            onStockClick = { symbol -> selectedStockSymbol = symbol },
                            onWatchlistToggle = { symbol -> viewModel.toggleStockInDefaultWatchlist(symbol) },
                            onSetAlertClick = { symbol ->
                                alertPrefillSymbol = symbol
                                currentTab = MainTab.ALERTS
                            },
                            onNavigateToSectors = { currentTab = MainTab.SECTORS },
                            onNavigateToAlerts = { currentTab = MainTab.ALERTS },
                            onNavigateToSentiment = { currentTab = MainTab.SENTIMENT }
                        )
                    }

                    MainTab.SENTIMENT -> {
                        MarketSentimentScreen(
                            report = sentimentReport,
                            isLoading = isNewsLoading,
                            onRefreshNews = { viewModel.refreshNewsAndSentiment() },
                            errorMessage = newsErrorMessage,
                            rateLimitResumeTime = rateLimitResumeTime,
                            onResumeNow = { viewModel.resumeSentimentAnalysis() }
                        )
                    }

                    MainTab.SECTORS -> {
                        SectoralMoversScreen(
                            sectors = sectors,
                            allStocks = stocks,
                            watchlistSymbols = watchedSymbolsSet,
                            onStockClick = { symbol -> selectedStockSymbol = symbol },
                            onWatchlistToggle = { symbol -> viewModel.toggleStockInDefaultWatchlist(symbol) },
                            onSetAlertClick = { symbol ->
                                alertPrefillSymbol = symbol
                                currentTab = MainTab.ALERTS
                            }
                        )
                    }

                    MainTab.WATCHLISTS -> {
                        WatchlistScreen(
                            watchlists = watchlists,
                            allStocks = stocks,
                            onCreateWatchlist = { name -> viewModel.createWatchlist(name) },
                            onDeleteWatchlist = { wl -> viewModel.deleteWatchlist(wl) },
                            onAddStockToWatchlist = { id, sym -> viewModel.addStockToWatchlist(id, sym) },
                            onRemoveStockFromWatchlist = { id, sym -> viewModel.removeStockFromWatchlist(id, sym) },
                            onStockClick = { symbol -> selectedStockSymbol = symbol },
                            onSetAlertClick = { symbol ->
                                alertPrefillSymbol = symbol
                                currentTab = MainTab.ALERTS
                            }
                        )
                    }

                    MainTab.ALERTS -> {
                        AlertsScreen(
                            activeAlerts = activeAlerts,
                            triggeredAlerts = triggeredAlerts,
                            allStocks = stocks,
                            prefillSymbol = alertPrefillSymbol,
                            onCreateAlert = { symbol, company, target, condition, note ->
                                viewModel.createAlert(symbol, company, target, condition, note)
                                alertPrefillSymbol = null
                            },
                            onDeleteAlert = { alert -> viewModel.deleteAlert(alert) },
                            onClearTriggeredAlerts = { viewModel.clearTriggeredAlerts() },
                            onSendTestNotification = { symbol, price -> viewModel.sendTestNotification(symbol, price) },
                            onStockClick = { symbol -> selectedStockSymbol = symbol }
                        )
                    }
                }
            }
        }
    }
}
