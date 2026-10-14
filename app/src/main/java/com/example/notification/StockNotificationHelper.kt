package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AlertEntity

class StockNotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "dalal_stock_alerts_channel"
        const val CHANNEL_NAME = "Stock Price Alerts"
        const val CHANNEL_DESC = "Instant notifications when stock target prices or percentage movements are triggered."
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendStockAlertNotification(
        alert: AlertEntity,
        currentPrice: Double,
        changePercent: Double
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_SYMBOL", alert.symbol)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            alert.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedPrice = String.format("₹%.2f", currentPrice)
        val targetFormatted = String.format("₹%.2f", alert.targetPrice)
        val formattedPercent = String.format("%+.2f%%", changePercent)

        val title = "🔔 Alert Triggered: ${alert.symbol}"
        val content = "${alert.companyName} reached $formattedPrice ($formattedPercent)! Target: $targetFormatted. ${if (alert.note.isNotBlank()) "Note: " + alert.note else ""}"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(alert.id.toInt() + 1000, builder.build())
        } catch (_: SecurityException) {
            // Permission denied or missing
        }
    }

    fun sendTestNotification(symbol: String, price: Double) {
        val formattedPrice = String.format("₹%.2f", price)
        val content = "Test Alert: $symbol is currently trading at $formattedPrice. Real-time alert notifications are active!"
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⚡ DalalPulse Live Alert Test")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
        } catch (_: SecurityException) {
        }
    }
}
