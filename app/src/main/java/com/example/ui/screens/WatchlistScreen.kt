package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.WatchlistEntity
import com.example.data.model.Stock
import com.example.ui.components.StockRowCard
import com.example.ui.theme.CardBorderDark
import com.example.ui.theme.LossRed
import com.example.ui.theme.LossRedSubtle
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenSubtle
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

enum class SortMode(val label: String) {
    DEFAULT("Default"),
    GAINERS_FIRST("% Change (High-Low)"),
    LOSERS_FIRST("% Change (Low-High)"),
    PRICE_HIGH("Price (High-Low)"),
    NAME_AZ("Name (A-Z)")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    watchlists: List<WatchlistEntity>,
    allStocks: List<Stock>,
    onCreateWatchlist: (String) -> Unit,
    onDeleteWatchlist: (WatchlistEntity) -> Unit,
    onAddStockToWatchlist: (Long, String) -> Unit,
    onRemoveStockFromWatchlist: (Long, String) -> Unit,
    onStockClick: (String) -> Unit,
    onSetAlertClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedWatchlistIndex by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAddStocksSheet by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf(SortMode.DEFAULT) }
    var showSortMenu by remember { mutableStateOf(false) }

    val activeWatchlist = watchlists.getOrNull(selectedWatchlistIndex) ?: watchlists.firstOrNull()

    val currentSymbols = remember(activeWatchlist) {
        activeWatchlist?.symbolsCsv?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    val stockMap = remember(allStocks) { allStocks.associateBy { it.symbol } }

    val watchlistStocks = remember(currentSymbols, stockMap, sortMode) {
        val list = currentSymbols.mapNotNull { stockMap[it] }
        when (sortMode) {
            SortMode.DEFAULT -> list
            SortMode.GAINERS_FIRST -> list.sortedByDescending { it.changePercent }
            SortMode.LOSERS_FIRST -> list.sortedBy { it.changePercent }
            SortMode.PRICE_HIGH -> list.sortedByDescending { it.currentPrice }
            SortMode.NAME_AZ -> list.sortedBy { it.symbol }
        }
    }

    val avgChangePercent = remember(watchlistStocks) {
        if (watchlistStocks.isEmpty()) 0.0 else watchlistStocks.map { it.changePercent }.average()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
    ) {
        // Screen Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "My Watchlists",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "Custom tracked portfolios and baskets",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Add stock button
                    if (activeWatchlist != null) {
                        Surface(
                            onClick = { showAddStocksSheet = true },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.height(34.dp).testTag("add_stocks_to_watchlist_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Stocks",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Add Stock",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Watchlists Tabs Bar
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(watchlists.indices.toList()) { index ->
                    val wl = watchlists[index]
                    val isSelected = index == selectedWatchlistIndex
                    Surface(
                        onClick = { selectedWatchlistIndex = index },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else SurfaceDark,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else CardBorderDark
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = wl.name,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextSecondaryDark
                            )
                            if (wl.isDefault) {
                                Text("★", fontSize = 10.sp, color = if (isSelected) Color.White else Color(0xFFFBBF24))
                            }
                        }
                    }
                }

                // "+ New Watchlist" Button
                item {
                    Surface(
                        onClick = { showCreateDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark),
                        modifier = Modifier.height(36.dp).testTag("create_watchlist_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Create",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "New",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Active Watchlist Summary Card
        if (activeWatchlist != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = activeWatchlist.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = "${watchlistStocks.size} stocks tracked",
                                    fontSize = 11.sp,
                                    color = TextSecondaryDark
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val isAvgUp = avgChangePercent >= 0
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isAvgUp) ProfitGreenSubtle else LossRedSubtle
                                ) {
                                    Text(
                                        text = String.format("Avg %+.2f%%", avgChangePercent),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAvgUp) ProfitGreen else LossRed,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                if (!activeWatchlist.isDefault) {
                                    IconButton(
                                        onClick = { onDeleteWatchlist(activeWatchlist) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Watchlist",
                                            tint = LossRed.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Sort filter row
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sort: ${sortMode.label}",
                                fontSize = 11.sp,
                                color = TextSecondaryDark
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                SortMode.values().forEach { mode ->
                                    val isSelected = sortMode == mode
                                    Surface(
                                        onClick = { sortMode = mode },
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    ) {
                                        Text(
                                            text = when (mode) {
                                                SortMode.DEFAULT -> "All"
                                                SortMode.GAINERS_FIRST -> "Gainers"
                                                SortMode.LOSERS_FIRST -> "Losers"
                                                SortMode.PRICE_HIGH -> "Price"
                                                SortMode.NAME_AZ -> "A-Z"
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else TextSecondaryDark,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Empty state or Stocks list
            if (watchlistStocks.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = TextSecondaryDark,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Stocks in this Watchlist",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap 'Add Stock' to track your favorite NSE & BSE companies here.",
                                fontSize = 12.sp,
                                color = TextSecondaryDark,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showAddStocksSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Add Stocks Now")
                            }
                        }
                    }
                }
            } else {
                items(watchlistStocks, key = { it.symbol }) { stock ->
                    StockRowCard(
                        stock = stock,
                        isInWatchlist = true,
                        onStockClick = { onStockClick(stock.symbol) },
                        onWatchlistToggle = {
                            activeWatchlist?.let { onRemoveStockFromWatchlist(it.id, stock.symbol) }
                        },
                        onSetAlertClick = { onSetAlertClick(stock.symbol) }
                    )
                }
            }
        }
    }

    // CREATE WATCHLIST DIALOG
    if (showCreateDialog) {
        var newWatchlistName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(
                    text = "Create New Watchlist",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            },
            text = {
                Column {
                    Text(
                        text = "Organize stocks by themes, sectors, or trading strategies.",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newWatchlistName,
                        onValueChange = { newWatchlistName = it },
                        placeholder = { Text("e.g. Dividend Kings, EV & Green Energy") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = CardBorderDark,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("new_watchlist_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newWatchlistName.isNotBlank()) {
                            onCreateWatchlist(newWatchlistName.trim())
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // ADD STOCKS BOTTOM SHEET
    if (showAddStocksSheet && activeWatchlist != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var stockSearch by remember { mutableStateOf("") }

        val candidateStocks = remember(allStocks, stockSearch, currentSymbols) {
            allStocks.filter { s ->
                val matchesSearch = stockSearch.isBlank() ||
                        s.symbol.contains(stockSearch, ignoreCase = true) ||
                        s.companyName.contains(stockSearch, ignoreCase = true) ||
                        s.sector.contains(stockSearch, ignoreCase = true)
                matchesSearch
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showAddStocksSheet = false },
            sheetState = sheetState,
            containerColor = SurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Add Stocks to '${activeWatchlist.name}'",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = stockSearch,
                    onValueChange = { stockSearch = it },
                    placeholder = { Text("Search NSE/BSE stocks...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextSecondaryDark)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = CardBorderDark,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(candidateStocks, key = { it.symbol }) { s ->
                        val isAlreadyIn = currentSymbols.contains(s.symbol)
                        Surface(
                            onClick = {
                                if (isAlreadyIn) {
                                    onRemoveStockFromWatchlist(activeWatchlist.id, s.symbol)
                                } else {
                                    onAddStockToWatchlist(activeWatchlist.id, s.symbol)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isAlreadyIn) Color(0xFF1E293B) else Color(0xFF0F172A),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(s.symbol, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimaryDark)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF1E293B)
                                        ) {
                                            Text(s.exchange.name, fontSize = 9.sp, color = Color(0xFF93C5FD), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }
                                    Text(s.companyName, fontSize = 11.sp, color = TextSecondaryDark)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(String.format("₹%.2f", s.currentPrice), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimaryDark)
                                        Text(
                                            text = String.format("%+.2f%%", s.changePercent),
                                            fontSize = 11.sp,
                                            color = if (s.changePercent >= 0) ProfitGreen else LossRed
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Icon(
                                        imageVector = if (isAlreadyIn) Icons.Default.Star else Icons.Default.Add,
                                        contentDescription = null,
                                        tint = if (isAlreadyIn) Color(0xFFFBBF24) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
