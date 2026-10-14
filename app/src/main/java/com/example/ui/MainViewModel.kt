package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AlertEntity
import com.example.data.local.WatchlistEntity
import com.example.data.model.AlertCondition
import com.example.data.model.MarketIndex
import com.example.data.model.MarketSentimentReport
import com.example.data.model.SectorPerformance
import com.example.data.model.Stock
import com.example.data.repository.MarketRepository
import com.example.data.repository.MarketStatusInfo
import com.example.data.repository.NewsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MarketRepository(application, viewModelScope)
    private val newsRepository = NewsRepository(application)

    val stocks: StateFlow<List<Stock>> = repository.stocks
    val indices: StateFlow<List<MarketIndex>> = repository.indices
    val sectors: StateFlow<List<SectorPerformance>> = repository.sectors
    val isLiveSimulationActive: StateFlow<Boolean> = repository.isLiveSimulationActive
    val isInternetSyncing: StateFlow<Boolean> = repository.isInternetSyncing
    val lastInternetSyncTime: StateFlow<Long?> = repository.lastInternetSyncTime
    val isInternetLiveConnected: StateFlow<Boolean> = repository.isInternetLiveConnected
    val activeDataSource: StateFlow<String> = repository.activeDataSource

    val watchlists: StateFlow<List<WatchlistEntity>> = repository.getAllWatchlists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeAlerts: StateFlow<List<AlertEntity>> = repository.getActiveAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val triggeredAlerts: StateFlow<List<AlertEntity>> = repository.getTriggeredAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sentimentReport: StateFlow<MarketSentimentReport> = newsRepository.sentimentReport
    val isNewsLoading: StateFlow<Boolean> = newsRepository.isLoading
    val newsErrorMessage: StateFlow<String?> = newsRepository.errorMessage
    val rateLimitResumeTime: StateFlow<Long?> = newsRepository.rateLimitResumeTime

    init {
        refreshNewsAndSentiment()
    }

    fun refreshNewsAndSentiment() {
        viewModelScope.launch {
            newsRepository.fetchLatestNewsAndSentiment()
        }
    }

    fun resumeSentimentAnalysis() {
        newsRepository.resumeNow()
    }

    fun syncLiveInternetMarketData() {
        viewModelScope.launch {
            repository.syncLiveMarketDataFromInternet()
        }
    }

    fun getMarketStatusInfo(): MarketStatusInfo = repository.getMarketStatusInfo()

    fun toggleSimulation() {
        repository.toggleSimulation()
    }

    fun createWatchlist(name: String) {
        viewModelScope.launch {
            repository.createWatchlist(name)
        }
    }

    fun deleteWatchlist(watchlist: WatchlistEntity) {
        viewModelScope.launch {
            repository.deleteWatchlist(watchlist)
        }
    }

    fun addStockToWatchlist(watchlistId: Long, symbol: String) {
        viewModelScope.launch {
            repository.addStockToWatchlist(watchlistId, symbol)
        }
    }

    fun removeStockFromWatchlist(watchlistId: Long, symbol: String) {
        viewModelScope.launch {
            repository.removeStockFromWatchlist(watchlistId, symbol)
        }
    }

    fun toggleStockInDefaultWatchlist(symbol: String) {
        viewModelScope.launch {
            val defaultWl = watchlists.value.firstOrNull { it.isDefault } ?: watchlists.value.firstOrNull() ?: return@launch
            val symbols = defaultWl.symbolsCsv.split(",").filter { it.isNotBlank() }
            if (symbols.contains(symbol)) {
                repository.removeStockFromWatchlist(defaultWl.id, symbol)
            } else {
                repository.addStockToWatchlist(defaultWl.id, symbol)
            }
        }
    }

    fun createAlert(
        symbol: String,
        companyName: String,
        targetPrice: Double,
        condition: AlertCondition,
        note: String
    ) {
        viewModelScope.launch {
            repository.createAlert(symbol, companyName, targetPrice, condition, note)
        }
    }

    fun deleteAlert(alert: AlertEntity) {
        viewModelScope.launch {
            repository.deleteAlert(alert)
        }
    }

    fun clearTriggeredAlerts() {
        viewModelScope.launch {
            repository.clearTriggeredAlerts()
        }
    }

    fun sendTestNotification(symbol: String, price: Double) {
        repository.notificationHelper.sendTestNotification(symbol, price)
    }
}
