# Real-Time Indian Market Data & Free-Tier AI Sentiment Engine

A high-performance real-time market tracking architecture for NSE and BSE equities and indices using Yahoo Finance public endpoints, direct NSE indices integration, high-frequency 7-second polling with graceful simulation fallback, and Google Gemini 2.5 Flash on the free-tier quota for live market sentiment.

### User Review & Critical Decisions

> [!IMPORTANT]
> The architectural directions below reflect your confirmed selections from Phase 1. Review the strategy and proceed when ready.

- **Confirmed Decision 1 (Primary Data Source)**: Yahoo Finance v8 Chart API as the primary real-time quotes provider for NSE (`.NS`) and BSE (`.BO`) equities and indices (`^NSEI`, `^BSESN`, `^NSEBANK`, etc.), with secondary integration of NSE India public endpoints and automated fallback to simulated live ticks outside trading hours (9:15 AM – 3:30 PM IST) or on network disconnect.
- **Confirmed Decision 2 (Refresh Cadence)**: 7-second periodic polling interval across active watchlists and market indices, optimizing for live real-time price responsiveness while staying comfortably within public endpoint rate limits.
- **Confirmed Decision 3 (Free-Tier AI Model)**: Google Gemini 2.5 Flash (`gemini-2.5-flash`) on the Google AI Studio standard Free Tier (15 Requests/Min, 1,500 Requests/Day, 1M Tokens/Min at zero cost) for live news aggregation and sentiment analysis.

---

### 1. Overview & Core Concept

- **What It Does**: DalalPulse connects directly to live financial endpoints to stream real-time price quotes, percentage changes, daily volume, 52-week highs/lows, and intraday candlestick data for top Indian stocks across NSE and BSE. In parallel, it queries Gemini 2.5 Flash on the free tier to synthesize live financial news, institutional FII/DII flow dynamics, and macro drivers affecting Nifty 50 and Sensex.
- **Target Persona**: Indian stock market investors, active day traders, and swing traders looking for snappy, live tracking of Dalal Street without needing paid Bloomberg/Refinitiv subscriptions or complex broker logins.
- **Key Value**: 100% free, real-time, non-blocking financial experience with transparent source health indicators, live green/red price flash animations, and AI market context.

---

### 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Market Overview**: Users see live indices (NIFTY 50, SENSEX, BANK NIFTY, NIFTY IT, NIFTY AUTO) updating with green/red flash animations every 7 seconds. An active data source badge in the header shows real-time connectivity (`Yahoo Finance Live` or `NSE Direct Live`) with last sync time in IST.
  2. **Sectoral Movers**: Dedicated tabs for Top Gainer Sectors, Top Loser Sectors, and Sector Matrix ranked by percentage movement.
  3. **Stock Detail & Interactive Charts**: Tapping any stock displays intraday candlestick charts, day high/low bars, 5-level market depth order book, and key fundamentals.
  4. **Free-Tier Market Sentiment**: Dedicated sentiment tab displays a Fear & Greed gauge (0–100), overall sentiment badge (e.g. *Moderately Bullish*), key macroeconomic drivers (Crude Oil, RBI Policy, FII/DII flows), and a Gemini 2.5 Flash Free Tier indicator with an on-demand refresh button.
- **Visual Identity & Theme**:
  - **Aesthetic Direction**: High-contrast, institutional dark trading terminal inspired by Bloomberg and TradingView.
  - **Color Tokens**:
    - Dark Canvas: `#0F172A` (Slate Charcoal)
    - Card Surface: `#1E293B` with border `#334155`
    - Bullish / Profit: `#00C087` (Emerald Neon)
    - Bearish / Loss: `#FF3B30` (Vivid Crimson)
    - Source / Accent: `#3B82F6` (Electric Blue)
  - **Typography**: Crisp tabular numbers for financial figures, eliminating layout shift during rapid price ticks.

---

### 3. Key Product Decisions & Trade-Offs

- **Data Source Strategy (Yahoo Finance v8 + NSE Direct + Auto-Fallback)**:
  - *Chosen Approach*: Query `https://query1.finance.yahoo.com/v8/finance/chart/{symbol}` for Indian symbols (`RELIANCE.NS`, `TCS.NS`, `^NSEI`, `^BSESN`). Combine with `https://www.nseindia.com/api/allIndices` where accessible.
  - *Why*: Yahoo Finance provides reliable real-time and historical 15m/1d candlestick data for Indian equities without requiring private API tokens or complex browser cookie rotation.
  - *Fallback*: If the device is offline, rate-limited, or testing outside Indian trading hours (e.g., late nights or weekends), DalalPulse automatically engages its local tick simulation engine so users can always interact with live charts and price alerts.
- **Polling Interval (7 Seconds)**:
  - *Chosen Approach*: Coroutine-based background polling loop with `delay(7000)` running on `Dispatchers.IO`, coupled with parallel batch requests (`coroutineScope.async`).
  - *Why*: 7 seconds delivers genuine real-time ticker feedback while staying well under Yahoo's rate limits and conserving mobile device battery.
- **AI Model Selection (Gemini 2.5 Flash)**:
  - *Chosen Approach*: `gemini-2.5-flash` with direct REST invocation via Retrofit.
  - *Why*: Gemini 2.5 Flash is officially supported on Google AI Studio's Free Tier (15 RPM, 1,500 RPD, zero cost) and offers ultra-low latency and reliable structured JSON outputs.

---

### 4. Technical Architecture & Data Strategy

#### System Architecture & Data Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                       External Data Sources                     │
│  ┌───────────────────────┐             ┌─────────────────────┐  │
│  │   Yahoo Finance v8    │             │ Google Gemini 2.5   │  │
│  │ (.NS / .BO / ^Indices)│             │ Flash (Free Tier)   │  │
│  └───────────┬───────────┘             └──────────┬──────────┘  │
└──────────────┼────────────────────────────────────┼─────────────┘
               │ HTTP GET (Every 7s)                │ REST POST
               ▼                                    ▼
┌───────────────────────────────────┐    ┌────────────────────────┐
│     MultiSourceMarketService      │    │     NewsRepository     │
│   - YahooFinanceService (v8)      │    │   - Gemini 2.5 Flash   │
│   - NsePublicApiService           │    │   - Fallback Sentiment │
└─────────────────┬─────────────────┘    └──────────┬─────────────┘
                  │                                 │
                  ▼                                 │
┌───────────────────────────────────┐               │
│         MarketRepository          │               │
│   - 7s Periodic Polling Job       │               │
│   - Offline Simulation Fallback   │               │
│   - Alert Trigger Evaluator       │               │
│   - Sector Performance Aggregator │               │
└─────────────────┬─────────────────┘               │
                  │ StateFlow Streams               │ StateFlow
                  ▼                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                    ViewModels & Jetpack Compose UI              │
│   - MainViewModel & MarketSentimentViewModel                    │
│   - MarketOverviewScreen (Live Indices & Ticker Flash)          │
│   - SectoralMoversScreen (Gainer/Loser Sectors)                 │
│   - WatchlistScreen & InteractiveStockChart                     │
│   - MarketSentimentScreen (Fear/Greed Gauge & Macro Drivers)    │
└─────────────────────────────────────────────────────────────────┘
```

#### Component Implementation Checklist

1. **`YahooFinanceService.kt`**: Verify and tune batch quote fetching using parallel coroutines, extracting `regularMarketPrice`, `chartPreviousClose`, `regularMarketDayHigh/Low`, `fiftyTwoWeekHigh/Low`, and intraday candle arrays.
2. **`MarketRepository.kt`**:
   - Update periodic sync loop from 30s to 7s (`delay(7000)`).
   - Ensure resilient exception handling and immediate fallback to tick simulation when endpoints return HTTP 429 or network timeouts.
   - Update `_activeDataSource` label to display live status accurately.
3. **`NewsRepository.kt`**:
   - Confirm target endpoint is strictly `models/gemini-2.5-flash:generateContent`.
   - Ensure clean fallback when API key is not present so the UI never crashes or shows empty views.
4. **Build & Compilation Verification**: Run `compile_applet` to guarantee complete build integrity.
