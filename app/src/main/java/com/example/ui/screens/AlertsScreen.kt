package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.AlertEntity
import com.example.data.model.AlertCondition
import com.example.data.model.Stock
import com.example.ui.theme.CardBorderDark
import com.example.ui.theme.LossRed
import com.example.ui.theme.LossRedSubtle
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenSubtle
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    activeAlerts: List<AlertEntity>,
    triggeredAlerts: List<AlertEntity>,
    allStocks: List<Stock>,
    onCreateAlert: (String, String, Double, AlertCondition, String) -> Unit,
    onDeleteAlert: (AlertEntity) -> Unit,
    onClearTriggeredAlerts: () -> Unit,
    onSendTestNotification: (String, Double) -> Unit,
    onStockClick: (String) -> Unit,
    prefillSymbol: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Active, 1: Triggered History
    var showCreateAlertSheet by remember { mutableStateOf(prefillSymbol != null) }

    val stockMap = remember(allStocks) { allStocks.associateBy { it.symbol } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
    ) {
        // Screen Header & Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Stock Price Alerts",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "Personalized push notifications for target prices",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                }

                Surface(
                    onClick = { showCreateAlertSheet = true },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.height(34.dp).testTag("create_stock_alert_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAlert,
                            contentDescription = "New Alert",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Set Alert",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Notification Permission Prompt (if needed)
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Enable Push Notifications",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = "Receive instant alerts when stock targets are hit.",
                                    fontSize = 11.sp,
                                    color = TextSecondaryDark
                                )
                            }
                        }
                        Button(
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.height(32.dp).testTag("enable_notifications_button")
                        ) {
                            Text("Enable", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Test Push Notification Card
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Test Real-Time Alert System",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Dispatches a simulated notification to your system tray",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            val sample = allStocks.firstOrNull() ?: Stock(
                                symbol = "RELIANCE",
                                companyName = "Reliance Industries",
                                exchange = com.example.data.model.Exchange.NSE,
                                sector = "Energy",
                                currentPrice = 2984.70,
                                previousClose = 2962.30,
                                openPrice = 2965.0,
                                dayHigh = 2998.0,
                                dayLow = 2955.0,
                                yearHigh = 3217.9,
                                yearLow = 2221.0,
                                volume = 14500000,
                                marketCapCrores = 2018000.0,
                                peRatio = 28.2,
                                beta = 1.02,
                                dividendYield = 0.34
                            )
                            onSendTestNotification(sample.symbol, sample.currentPrice)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp).testTag("send_test_notification_button")
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Send Test", fontSize = 11.sp)
                    }
                }
            }
        }

        // Tabs: Active vs Triggered
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark, RoundedCornerShape(10.dp))
                    .border(1.dp, CardBorderDark, RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    onClick = { selectedTab = 0 },
                    shape = RoundedCornerShape(8.dp),
                    color = if (selectedTab == 0) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Active Alerts (${activeAlerts.size})",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 0) Color.White else TextSecondaryDark
                        )
                    }
                }

                Surface(
                    onClick = { selectedTab = 1 },
                    shape = RoundedCornerShape(8.dp),
                    color = if (selectedTab == 1) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Triggered (${triggeredAlerts.size})",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 1) Color.White else TextSecondaryDark
                        )
                    }
                }
            }
        }

        // TAB CONTENT
        if (selectedTab == 0) {
            // ACTIVE ALERTS
            if (activeAlerts.isEmpty()) {
                item {
                    EmptyAlertsCard(
                        title = "No Active Stock Alerts",
                        description = "Set price triggers on your favorite stocks to be notified instantly when they breakout or dip.",
                        buttonText = "Create Price Alert",
                        onButtonClick = { showCreateAlertSheet = true }
                    )
                }
            } else {
                items(activeAlerts, key = { it.id }) { alert ->
                    val stock = stockMap[alert.symbol]
                    ActiveAlertCard(
                        alert = alert,
                        currentStockPrice = stock?.currentPrice,
                        onDelete = { onDeleteAlert(alert) },
                        onClick = { onStockClick(alert.symbol) }
                    )
                }
            }
        } else {
            // TRIGGERED HISTORY
            if (triggeredAlerts.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onClearTriggeredAlerts) {
                            Text("Clear History", color = LossRed, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (triggeredAlerts.isEmpty()) {
                item {
                    EmptyAlertsCard(
                        title = "No Triggered Alerts Yet",
                        description = "When a stock touches your price target, it will be logged here and sent as a push notification.",
                        buttonText = "Set New Alert",
                        onButtonClick = { showCreateAlertSheet = true }
                    )
                }
            } else {
                items(triggeredAlerts, key = { it.id }) { alert ->
                    TriggeredAlertCard(
                        alert = alert,
                        onDelete = { onDeleteAlert(alert) },
                        onClick = { onStockClick(alert.symbol) }
                    )
                }
            }
        }
    }

    // CREATE ALERT MODAL SHEET
    if (showCreateAlertSheet) {
        CreateAlertDialog(
            allStocks = allStocks,
            prefillSymbol = prefillSymbol,
            onDismiss = { showCreateAlertSheet = false },
            onConfirm = { symbol, company, target, condition, note ->
                onCreateAlert(symbol, company, target, condition, note)
                showCreateAlertSheet = false
            }
        )
    }
}

@Composable
private fun ActiveAlertCard(
    alert: AlertEntity,
    currentStockPrice: Double?,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val conditionDesc = when (alert.condition) {
        AlertCondition.PRICE_ABOVE.name -> "Rises Above"
        AlertCondition.PRICE_BELOW.name -> "Drops Below"
        AlertCondition.PERCENT_CHANGE_UP.name -> "Up by"
        else -> "Down by"
    }

    val conditionColor = if (alert.condition.contains("ABOVE") || alert.condition.contains("UP")) ProfitGreen else LossRed

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(alert.symbol, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimaryDark)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = conditionColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "$conditionDesc ₹${String.format("%.2f", alert.targetPrice)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = conditionColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = TextSecondaryDark, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(alert.companyName, fontSize = 11.sp, color = TextSecondaryDark)

            if (alert.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Note: ${alert.note}", fontSize = 11.sp, color = Color(0xFFFBBF24))
            }

            if (currentStockPrice != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val diff = alert.targetPrice - currentStockPrice
                val diffPct = (diff / currentStockPrice) * 100
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Current: ₹${String.format("%.2f", currentStockPrice)}", fontSize = 11.sp, color = TextSecondaryDark)
                    Text("Diff: ${String.format("%+.2f (%.1f%%)", diff, diffPct)}", fontSize = 11.sp, color = if (diff >= 0) ProfitGreen else LossRed)
                }
            }
        }
    }
}

@Composable
private fun TriggeredAlertCard(
    alert: AlertEntity,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    val triggerTime = alert.triggeredAt?.let { sdf.format(Date(it)) } ?: "Recent"

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF131B2E),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(ProfitGreenSubtle, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = ProfitGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(alert.symbol, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimaryDark)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Target: ₹${String.format("%.2f", alert.targetPrice)}", fontSize = 12.sp, color = ProfitGreen, fontWeight = FontWeight.SemiBold)
                    }
                    Text("${alert.companyName} • Triggered $triggerTime", fontSize = 11.sp, color = TextSecondaryDark)
                    if (alert.note.isNotBlank()) {
                        Text(alert.note, fontSize = 11.sp, color = Color(0xFFFBBF24))
                    }
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = TextSecondaryDark, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun EmptyAlertsCard(
    title: String,
    description: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsNone,
                contentDescription = null,
                tint = TextSecondaryDark,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimaryDark)
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, fontSize = 12.sp, color = TextSecondaryDark, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onButtonClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(buttonText)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateAlertDialog(
    allStocks: List<Stock>,
    prefillSymbol: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, AlertCondition, String) -> Unit
) {
    var selectedStock by remember {
        mutableStateOf(allStocks.firstOrNull { it.symbol == prefillSymbol } ?: allStocks.firstOrNull())
    }
    var stockDropdownExpanded by remember { mutableStateOf(false) }
    var stockSearchQuery by remember { mutableStateOf("") }

    var condition by remember { mutableStateOf(AlertCondition.PRICE_ABOVE) }
    var targetPriceInput by remember(selectedStock, condition) {
        val base = selectedStock?.currentPrice ?: 1000.0
        val target = if (condition == AlertCondition.PRICE_ABOVE) base * 1.02 else base * 0.98
        mutableStateOf(String.format(Locale.US, "%.2f", target))
    }
    var noteInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Set Stock Price Alert", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Stock Picker
                Text("Select Stock", fontSize = 11.sp, color = TextSecondaryDark)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    onClick = { stockDropdownExpanded = !stockDropdownExpanded },
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${selectedStock?.symbol ?: "Select"} (₹${String.format("%.2f", selectedStock?.currentPrice ?: 0.0)})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimaryDark
                        )
                        Text(if (stockDropdownExpanded) "▲" else "▼", fontSize = 10.sp, color = TextSecondaryDark)
                    }
                }

                if (stockDropdownExpanded) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = stockSearchQuery,
                        onValueChange = { stockSearchQuery = it },
                        placeholder = { Text("Filter stocks...", fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = CardBorderDark,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val filtered = allStocks.filter {
                        stockSearchQuery.isBlank() || it.symbol.contains(stockSearchQuery, ignoreCase = true)
                    }.take(4)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        filtered.forEach { s ->
                            Surface(
                                onClick = {
                                    selectedStock = s
                                    stockDropdownExpanded = false
                                },
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF0F172A),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(s.symbol, fontSize = 12.sp, color = TextPrimaryDark)
                                    Text("₹${String.format("%.2f", s.currentPrice)}", fontSize = 12.sp, color = TextSecondaryDark)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Condition Radio Chips
                Text("Alert Condition", fontSize = 11.sp, color = TextSecondaryDark)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = condition == AlertCondition.PRICE_ABOVE,
                        onClick = { condition = AlertCondition.PRICE_ABOVE },
                        label = { Text("Price Rises Above", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ProfitGreenSubtle,
                            selectedLabelColor = ProfitGreen
                        )
                    )
                    FilterChip(
                        selected = condition == AlertCondition.PRICE_BELOW,
                        onClick = { condition = AlertCondition.PRICE_BELOW },
                        label = { Text("Price Drops Below", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LossRedSubtle,
                            selectedLabelColor = LossRed
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Target Price Input
                Text("Target Price (₹)", fontSize = 11.sp, color = TextSecondaryDark)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = targetPriceInput,
                    onValueChange = { targetPriceInput = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = CardBorderDark,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("target_price_input")
                )

                // Quick percentage steps
                val current = selectedStock?.currentPrice ?: 1000.0
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(1.0, 2.0, 5.0).forEach { pct ->
                        Surface(
                            onClick = {
                                val mult = if (condition == AlertCondition.PRICE_ABOVE) (1.0 + pct / 100) else (1.0 - pct / 100)
                                targetPriceInput = String.format(Locale.US, "%.2f", current * mult)
                            },
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.weight(1f).height(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (condition == AlertCondition.PRICE_ABOVE) "+$pct%" else "-$pct%",
                                    fontSize = 10.sp,
                                    color = if (condition == AlertCondition.PRICE_ABOVE) ProfitGreen else LossRed
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Optional Note
                Text("Note / Trading Strategy (Optional)", fontSize = 11.sp, color = TextSecondaryDark)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    placeholder = { Text("e.g. 52W Breakout, Take Profit", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = CardBorderDark,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("alert_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = selectedStock ?: return@Button
                    val target = targetPriceInput.toDoubleOrNull() ?: s.currentPrice
                    onConfirm(s.symbol, s.companyName, target, condition, noteInput.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("save_alert_confirm_button")
            ) {
                Text("Set Alert")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondaryDark)
            }
        },
        containerColor = SurfaceDark
    )
}
