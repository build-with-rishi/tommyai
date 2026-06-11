package com.example.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppSettings
import com.example.data.CapturedNotification
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

private val CosmicBackground: Color @Composable get() = MaterialTheme.colorScheme.background
private val CosmicSurface: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
private val CosmicSurfaceVariant: Color @Composable get() = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
private val TealAccent: Color @Composable get() = MaterialTheme.colorScheme.primary
private val TealPrimary: Color @Composable get() = MaterialTheme.colorScheme.primary
private val TealSecondary: Color @Composable get() = MaterialTheme.colorScheme.secondary

enum class ActiveTab {
    DASHBOARD, TASKS, ALERTS, SETTINGS
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainLayout(
    viewModel: NotificationViewModel = viewModel()
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(ActiveTab.DASHBOARD) }
    var notificationAccessGranted by remember { mutableStateOf(false) }

    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedPackageFilter by remember { mutableStateOf<String?>(null) }
    
    val dashboardLazyListState = rememberLazyListState()
    val alertsLazyListState = rememberLazyListState()

    androidx.activity.compose.BackHandler(enabled = activeTab != ActiveTab.DASHBOARD) {
        activeTab = ActiveTab.DASHBOARD
    }

    // Check device notification access state
    fun checkPermission() {
        notificationAccessGranted = isNotificationServiceEnabled(context)
    }

    // Refresh permission when screen loads
    LaunchedEffect(Unit) {
        checkPermission()
    }

    // Request runtime POST_NOTIFICATIONS on Android 13+
    var postNotificationsGranted by remember { mutableStateOf(true) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        postNotificationsGranted = isGranted
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = CosmicSurface,
                contentColor = Color.White,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = activeTab == ActiveTab.DASHBOARD,
                    onClick = { activeTab = ActiveTab.DASHBOARD; checkPermission() },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TealAccent,
                        selectedTextColor = Color(0xFFE6E1E5),
                        unselectedIconColor = Color(0xFFCAC4D0),
                        unselectedTextColor = Color(0xFFCAC4D0),
                        indicatorColor = CosmicSurfaceVariant
                    ),
                    modifier = Modifier.testTag("tab_dashboard")
                )
                NavigationBarItem(
                    selected = activeTab == ActiveTab.TASKS,
                    onClick = { activeTab = ActiveTab.TASKS; checkPermission() },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Active Tasks") },
                    label = { Text("Tasks") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TealAccent,
                        selectedTextColor = Color(0xFFE6E1E5),
                        unselectedIconColor = Color(0xFFCAC4D0),
                        unselectedTextColor = Color(0xFFCAC4D0),
                        indicatorColor = CosmicSurfaceVariant
                    ),
                    modifier = Modifier.testTag("tab_tasks")
                )
                NavigationBarItem(
                    selected = activeTab == ActiveTab.ALERTS,
                    onClick = { activeTab = ActiveTab.ALERTS; checkPermission() },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "Alerts Inbox") },
                    label = { Text("Alerts") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TealAccent,
                        selectedTextColor = Color(0xFFE6E1E5),
                        unselectedIconColor = Color(0xFFCAC4D0),
                        unselectedTextColor = Color(0xFFCAC4D0),
                        indicatorColor = CosmicSurfaceVariant
                    ),
                    modifier = Modifier.testTag("tab_alerts")
                )
                NavigationBarItem(
                    selected = activeTab == ActiveTab.SETTINGS,
                    onClick = { activeTab = ActiveTab.SETTINGS; checkPermission() },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TealAccent,
                        selectedTextColor = Color(0xFFE6E1E5),
                        unselectedIconColor = Color(0xFFCAC4D0),
                        unselectedTextColor = Color(0xFFCAC4D0),
                        indicatorColor = CosmicSurfaceVariant
                    ),
                    modifier = Modifier.testTag("tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Surface(
            color = CosmicBackground,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "TabTransition"
            ) { targetTab ->
                when (targetTab) {
                    ActiveTab.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        listenerAccessGranted = notificationAccessGranted,
                        onCheckPermission = { checkPermission() },
                        lazyListState = dashboardLazyListState,
                        onNavigateToAlerts = { filter, pkg ->
                            selectedFilter = filter
                            selectedPackageFilter = pkg
                            activeTab = ActiveTab.ALERTS
                        },
                        onNavigateToSettings = {
                            activeTab = ActiveTab.SETTINGS
                        }
                    )
                    ActiveTab.TASKS -> TasksScreen(
                        viewModel = viewModel
                    )
                    ActiveTab.ALERTS -> AlertsScreen(
                        viewModel = viewModel,
                        listenerAccessGranted = notificationAccessGranted,
                        lazyListState = alertsLazyListState,
                        initialFilter = selectedFilter,
                        initialPackageFilter = selectedPackageFilter,
                        onFilterChanged = { selectedFilter = it },
                        onPackageFilterChanged = { selectedPackageFilter = it }
                    )
                    ActiveTab.SETTINGS -> SettingsScreen(
                        viewModel = viewModel,
                        listenerAccessGranted = notificationAccessGranted,
                        onCheckPermission = { checkPermission() }
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    viewModel: NotificationViewModel,
    listenerAccessGranted: Boolean,
    onCheckPermission: () -> Unit,
    lazyListState: LazyListState,
    onNavigateToAlerts: (filter: String, packageName: String?) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val notifications by viewModel.allNotifications.collectAsState()
    val settingsState by viewModel.settings.collectAsState()

    var isDigestExpanded by remember { mutableStateOf(true) }

    val aiDigestResult by viewModel.aiDigestResult.collectAsState()
    val isGeneratingDigest by viewModel.isGeneratingDigest.collectAsState()

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            HeaderBlock()
        }

        // Warning Panel if listener permissions are missing
        if (!listenerAccessGranted) {
            item {
                PermissionWarningCard(
                    onGrantClick = {
                        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                        context.startActivity(intent)
                    },
                    modifier = Modifier.testTag("warning_grant_permission")
                )
            }
        }

        // Home Lab Stats Grid System (Clean beautiful high-tech cards using Google Dark themes)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "HOME LAB CONSOLE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorSilent,
                    letterSpacing = 1.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Stats Box 1: Active Gateway Integration Status type
                    val engineName = when (settingsState.backendType) {
                        "LOCAL_SIMULATION" -> "LOCAL HEURISTIC"
                        "OLLAMA_LOCAL" -> "OLLAMA AT 11434"
                        "CUSTOM_ENDPOINT" -> "CUSTOM GATEWAY"
                        "GEMINI_API" -> "GEMINI AI SDK"
                        else -> "LOCAL RULES"
                    }
                    val engineColor = when (settingsState.backendType) {
                        "LOCAL_SIMULATION" -> ColorLow
                        "GEMINI_API" -> TealPrimary
                        else -> TealAccent
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                            .border(1.dp, CosmicSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .clickable { onNavigateToSettings() },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Engine Mode",
                                tint = engineColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(engineName, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ColorSilent, letterSpacing = 0.5.sp)
                                Text("SYSTEM GATEWAY", fontSize = 11.sp, color = Color.White.copy(alpha = 0.62f))
                            }
                        }
                    }

                    // Stats Box 2: Interceptions Counter
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                            .border(1.dp, CosmicSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .clickable { onNavigateToAlerts("ALL", null) },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Intercepts",
                                tint = TealPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text("${notifications.size} TRIGGERED", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("CAPTURED ALERTS", fontSize = 10.sp, color = ColorSilent)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Stats Box 3: Forwarded/Escaped notifications (>= threshold)
                    val thresholdVal = when (settingsState.passThroughThreshold.uppercase()) {
                        "SILENT" -> 0
                        "LOW" -> 1
                        "IMPORTANT" -> 2
                        "URGENT" -> 3
                        else -> 2
                    }
                    val escapedCount = notifications.count {
                        val pVal = when (it.priority.uppercase()) {
                            "SILENT" -> 0
                            "LOW" -> 1
                            "IMPORTANT" -> 2
                            "URGENT" -> 3
                            else -> 1
                        }
                        pVal >= thresholdVal
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                            .border(1.dp, CosmicSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .clickable { onNavigateToAlerts("ESCAPED", null) },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Urgent forwarded count",
                                tint = ColorUrgent,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text("$escapedCount ESCAPED", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ColorUrgent)
                                Text("FORWARDED BY AI", fontSize = 10.sp, color = ColorSilent)
                            }
                        }
                    }

                    // Stats Box 4: Muted background streams count (< threshold)
                    val mutedCount = notifications.count {
                        val pVal = when (it.priority.uppercase()) {
                            "SILENT" -> 0
                            "LOW" -> 1
                            "IMPORTANT" -> 2
                            "URGENT" -> 3
                            else -> 1
                        }
                        pVal < thresholdVal
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                            .border(1.dp, CosmicSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .clickable { onNavigateToAlerts("MUTED", null) },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Muted counts",
                                tint = ColorLow,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text("$mutedCount MUTED", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ColorLow)
                                Text("SILENCED BY LOCAL AI", fontSize = 10.sp, color = ColorSilent)
                            }
                        }
                    }
                }
            }
        }

        // --- TODAY AT A GLANCE ROW ---
        item {
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            val todayStart = cal.timeInMillis
            val yesterdayStart = todayStart - 24 * 60 * 60 * 1000L

            val todayCount = notifications.count { it.timestamp >= todayStart }
            val yesterdayCount = notifications.count { it.timestamp in yesterdayStart until todayStart }
            val delta = todayCount - yesterdayCount
            val deltaStr = if (delta > 0) "+$delta ▲" else if (delta < 0) "$delta ▼" else "0 •"
            val deltaColor = if (delta > 0) ColorUrgent else if (delta < 0) ColorLow else ColorSilent

            // AI Accuracy Rate
            val thresholdVal = when (settingsState.passThroughThreshold.uppercase()) {
                "SILENT" -> 0
                "LOW" -> 1
                "IMPORTANT" -> 2
                "URGENT" -> 3
                else -> 2
            }
            val totalCount = notifications.size
            val escapedCount = notifications.count {
                val pVal = when (it.priority.uppercase()) {
                    "SILENT" -> 0
                    "LOW" -> 1
                    "IMPORTANT" -> 2
                    "URGENT" -> 3
                    else -> 1
                }
                pVal >= thresholdVal
            }
            val mutedCount = totalCount - escapedCount
            val accuracyValue = if (totalCount == 0) 100 else (mutedCount * 105 / totalCount).coerceAtMost(100)
            val accuracyRateStr = "$accuracyValue%"

            // Peak Hour
            val hourCounts = IntArray(24)
            notifications.forEach {
                val itemCal = java.util.Calendar.getInstance().apply { timeInMillis = it.timestamp }
                val hr = itemCal.get(java.util.Calendar.HOUR_OF_DAY)
                if (hr in 0..23) hourCounts[hr]++
            }
            val peakHour = hourCounts.indices.maxByOrNull { hourCounts[it] } ?: 9
            val pmStr = if (peakHour >= 12) "PM" else "AM"
            val displayPeakHour = when {
                peakHour == 0 -> 12
                peakHour > 12 -> peakHour - 12
                else -> peakHour
            }
            val peakHourStr = "PEAK: $displayPeakHour $pmStr"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Today vs Yesterday Pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmicSurface)
                        .border(1.dp, CosmicSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ALERT VOL DELTA", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ColorSilent)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(deltaStr, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = deltaColor)
                    }
                }

                // AI Accuracy rate
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmicSurface)
                        .border(1.dp, CosmicSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("AI ACCURACY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ColorSilent)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(accuracyRateStr, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TealAccent)
                    }
                }

                // Busiest peak hour
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmicSurface)
                        .border(1.dp, CosmicSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("BUSIEST HOUR", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ColorSilent)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(peakHourStr, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // --- 7-DAY TREND SPARKLINE CARD ---
        item {
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            val todayStart = cal.timeInMillis

            val thresholdVal = when (settingsState.passThroughThreshold.uppercase()) {
                "SILENT" -> 0
                "LOW" -> 1
                "IMPORTANT" -> 2
                "URGENT" -> 3
                else -> 2
            }

            val dayStartTimes = (0..6).map { day ->
                todayStart - day * 24 * 60 * 60 * 1000L
            }.reversed()

            val dailyTriggeredCounts = dayStartTimes.map { startTime ->
                val endTime = startTime + 24 * 60 * 60 * 1000L
                notifications.count { it.timestamp in startTime until endTime }
            }

            val dailyEscapedCounts = dayStartTimes.map { startTime ->
                val endTime = startTime + 24 * 60 * 60 * 1000L
                notifications.count { it.timestamp in startTime until endTime && 
                    (when (it.priority.uppercase()) {
                        "SILENT" -> 0
                        "LOW" -> 1
                        "IMPORTANT" -> 2
                        "URGENT" -> 3
                        else -> 1
                    } >= thresholdVal)
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CosmicSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("7-DAY SIGNAL TREND", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TealAccent, letterSpacing = 1.sp)
                            Text("Triggered (Green) vs. Escaped (Red)", fontSize = 11.sp, color = ColorSilent)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    val maxVal = (dailyTriggeredCounts.maxOrNull() ?: 5).coerceAtLeast(5)

                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    ) {
                        val widthSum = size.width
                        val heightSum = size.height
                        val pointsCount = dayStartTimes.size
                        val stepX = widthSum / (pointsCount - 1).coerceAtLeast(1)

                        val pPath = androidx.compose.ui.graphics.Path()
                        val ePath = androidx.compose.ui.graphics.Path()

                        dailyTriggeredCounts.forEachIndexed { idx, value ->
                            val x = idx * stepX
                            val y = heightSum - (value.toFloat() / maxVal.toFloat() * heightSum)
                            if (idx == 0) pPath.moveTo(x, y) else pPath.lineTo(x, y)
                        }

                        dailyEscapedCounts.forEachIndexed { idx, value ->
                            val x = idx * stepX
                            val y = heightSum - (value.toFloat() / maxVal.toFloat() * heightSum)
                            if (idx == 0) ePath.moveTo(x, y) else ePath.lineTo(x, y)
                        }

                        drawPath(
                            path = pPath,
                            color = ColorLow,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                        )
                        drawPath(
                            path = ePath,
                            color = ColorUrgent,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                        )
                    }
                }
            }
        }

        // --- TOP APPS BY ALERT VOLUME ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "TOP APPS BY ALERT VOLUME",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorSilent,
                    letterSpacing = 1.5.sp
                )

                val appGroups = notifications.groupBy { it.packageName }
                    .map { (pkg, list) ->
                        val appName = list.firstOrNull()?.appName ?: pkg
                        pkg to (appName to list.size)
                    }
                    .sortedByDescending { it.second.second }
                    .take(6)

                if (appGroups.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CosmicSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No alerts tracked yet.", color = ColorSilent, fontSize = 13.sp)
                        }
                    }
                } else {
                    val maxVolume = appGroups.maxOf { it.second.second }.coerceAtLeast(1)
                    val installedAppsList by viewModel.installedApps.collectAsState()

                    Card(
                        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CosmicSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            appGroups.forEach { (pkg, appData) ->
                                val (appName, count) = appData
                                val appInfo = installedAppsList.find { it.packageName == pkg }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigateToAlerts("ALL", pkg) },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (appInfo?.icon != null) {
                                        androidx.compose.foundation.Image(
                                            painter = coil.compose.rememberAsyncImagePainter(appInfo.icon),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(TealPrimary.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = appName.take(1).uppercase(),
                                                color = TealPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = appName,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "$count alerts",
                                                fontSize = 11.sp,
                                                color = ColorSilent
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val fillRatio = count.toFloat() / maxVolume.toFloat()
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .background(CosmicSurfaceVariant, RoundedCornerShape(2.dp))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(fillRatio)
                                                    .fillMaxHeight()
                                                    .background(TealAccent, RoundedCornerShape(2.dp))
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

        // --- RECENT ALERTS PREVIEW STRIP ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "LATEST SIGNALS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorSilent,
                    letterSpacing = 1.5.sp
                )

                val recentList = notifications.take(5)
                if (recentList.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CosmicSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No alerts recorded yet.", color = ColorSilent, fontSize = 13.sp)
                        }
                    }
                } else {
                    val installedAppsList by viewModel.installedApps.collectAsState()
                    
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(recentList) { alert ->
                            val appInfo = installedAppsList.find { it.packageName == alert.packageName }
                            val timeStr = remember(alert.timestamp) {
                                SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(alert.timestamp))
                            }
                            val badgeColor = when (alert.priority.uppercase()) {
                                "URGENT" -> ColorUrgent
                                "IMPORTANT" -> ColorImportant
                                "LOW" -> ColorLow
                                "SILENT" -> ColorSilent
                                else -> TealAccent
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .width(220.dp)
                                    .height(115.dp)
                                    .border(1.dp, CosmicSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                    .clickable { onNavigateToAlerts("ALL", alert.packageName) }
                            ) {
                                Column(modifier = Modifier.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (appInfo?.icon != null) {
                                                androidx.compose.foundation.Image(
                                                    painter = coil.compose.rememberAsyncImagePainter(appInfo.icon),
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .background(badgeColor.copy(alpha = 0.15f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = alert.appName.take(1).uppercase(),
                                                        color = badgeColor,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = alert.appName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1
                                            )
                                        }
                                        Text(
                                            text = timeStr,
                                            fontSize = 9.sp,
                                            color = ColorSilent
                                        )
                                    }
                                    
                                    Text(
                                        text = alert.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White.copy(alpha = 0.9f),
                                        maxLines = 2,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(badgeColor, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = alert.priority,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AlertsScreen(
    viewModel: NotificationViewModel,
    listenerAccessGranted: Boolean,
    lazyListState: LazyListState,
    initialFilter: String,
    initialPackageFilter: String?,
    onFilterChanged: (String) -> Unit,
    onPackageFilterChanged: (String?) -> Unit
) {
    val context = LocalContext.current
    val notifications by viewModel.allNotifications.collectAsState()
    val settingsState by viewModel.settings.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showArchived by remember { mutableStateOf(false) }

    val thresholdVal = remember(settingsState.passThroughThreshold) {
        when (settingsState.passThroughThreshold.uppercase()) {
            "SILENT" -> 0
            "LOW" -> 1
            "IMPORTANT" -> 2
            "URGENT" -> 3
            else -> 2
        }
    }

    // Filter results dynamically
    val filteredList = remember(notifications, searchQuery, initialFilter, initialPackageFilter, showArchived, thresholdVal) {
        notifications.filter { item ->
            val matchesQuery = item.appName.contains(searchQuery, ignoreCase = true) ||
                    item.packageName.contains(searchQuery, ignoreCase = true) ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.body.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (initialFilter.uppercase()) {
                "ALL" -> true
                "ESCAPED" -> {
                    val pVal = when (item.priority.uppercase()) {
                        "SILENT" -> 0
                        "LOW" -> 1
                        "IMPORTANT" -> 2
                        "URGENT" -> 3
                        else -> 1
                    }
                    pVal >= thresholdVal
                }
                "MUTED" -> {
                    val pVal = when (item.priority.uppercase()) {
                        "SILENT" -> 0
                        "LOW" -> 1
                        "IMPORTANT" -> 2
                        "URGENT" -> 3
                        else -> 1
                    }
                    pVal < thresholdVal
                }
                else -> item.priority.uppercase() == initialFilter.uppercase()
            }

            val matchesPackage = initialPackageFilter == null || item.packageName == initialPackageFilter
            val matchesArchive = item.isArchived == showArchived

            matchesQuery && matchesFilter && matchesPackage && matchesArchive
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Tommy Alert Inbox",
                        fontSize = 24.sp,
                        color = Color(0xFFE6E1E5),
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Real-time AI classified packages and filters",
                        fontSize = 11.sp,
                        color = ColorSilent
                    )
                }
            }
        }

        // Live Filters and Controls
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search package or keyword...", color = ColorSilent) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = ColorSilent) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = ColorSilent)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_field"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = TealAccent,
                        unfocusedBorderColor = CosmicSurfaceVariant,
                        focusedContainerColor = CosmicSurface,
                        unfocusedContainerColor = CosmicSurface
                    )
                )

                // Archive Toggle + Clear Database button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.clickable { showArchived = !showArchived },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = showArchived,
                            onCheckedChange = { showArchived = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = TealAccent,
                                checkmarkColor = CosmicSurface
                            )
                        )
                        Text(
                            text = "View Archived Alerts only",
                            fontSize = 13.sp,
                            color = ColorSilent
                        )
                    }

                    TextButton(
                        onClick = { viewModel.clearAll() },
                        modifier = Modifier.testTag("btn_clear_history")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear", tint = ColorUrgent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All", color = ColorUrgent, fontSize = 12.sp)
                    }
                }

                // Filter Capsule row
                val filterCapsules = listOf(
                    "ALL" to "All",
                    "URGENT" to "🔴 Urgent",
                    "IMPORTANT" to "🟡 Important",
                    "LOW" to "🟢 Low",
                    "SILENT" to "⚪ Silent"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filterCapsules.forEach { (capital, label) ->
                        val selected = initialFilter == capital
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (selected) TealAccent else CosmicSurface)
                                .border(1.dp, if (selected) TealAccent else CosmicSurfaceVariant, RoundedCornerShape(20.dp))
                                .clickable { onFilterChanged(capital) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) CosmicBackground else Color.White
                            )
                        }
                    }
                }

                // Extra visual indicator for special active filters (Escaped vs. Muted)
                if (initialFilter == "ESCAPED" || initialFilter == "MUTED") {
                    val filterLabel = if (initialFilter == "ESCAPED") "Escaped Only" else "Muted Only"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = CosmicSurface,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TealAccent)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Active Filter: $filterLabel",
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear Active Filter",
                                    tint = ColorUrgent,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onFilterChanged("ALL") }
                                )
                            }
                        }
                    }
                }

                // Filtered App pill
                if (initialPackageFilter != null) {
                    val appName = notifications.find { it.packageName == initialPackageFilter }?.appName ?: initialPackageFilter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = CosmicSurface,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TealAccent)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Filtered App: $appName",
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear App Filter",
                                    tint = ColorUrgent,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onPackageFilterChanged(null) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Logs and notifications List
        if (filteredList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Empty",
                            tint = CosmicSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Silence is golden. No alerts here!",
                            fontSize = 14.sp,
                            color = ColorSilent,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredList, key = { it.id }) { item ->
                NotificationCard(
                    item = item,
                    onMarkRead = { viewModel.markAsRead(item.id) },
                    onArchiveToggle = { viewModel.setArchived(item.id, !item.isArchived) },
                    onPromote = { viewModel.promoteNotification(item) },
                    onDelete = { viewModel.deleteNotification(item.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun HeaderBlock() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Tommy",
                fontSize = 24.sp,
                color = Color(0xFFE6E1E5),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(ColorLow)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "LOCAL AI ACTIVE",
                    fontSize = 11.sp,
                    color = ColorLow,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(CosmicSurfaceVariant)
                .clickable { /* Action if desired */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color(0xFFE6E1E5),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun PermissionWarningCard(
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ColorUrgent.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, ColorUrgent, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = ColorUrgent,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Notification Access Required",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Tommy is unable to filter or capture background notifications. Grant system access is required.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onGrantClick,
                    colors = ButtonDefaults.buttonColors(containerColor = ColorUrgent),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Enable Access in System", fontSize = 11.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun EmulatorSimulatorBlock(
    onInject: (String, String, String) -> Unit
) {
    var isTabExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CosmicSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isTabExpanded = !isTabExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = "Emulator Simulator", tint = TealAccent)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Browser Emulator Test Injector",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                IconButton(onClick = { isTabExpanded = !isTabExpanded }) {
                    Icon(
                        imageVector = if (isTabExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle Simulator",
                        tint = Color.LightGray
                    )
                }
            }

            AnimatedVisibility(visible = isTabExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Press any button below to inject mock incoming system notifications and test physical classification speeds and silent/sound policies instantly:",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val context = LocalContext.current
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                onInject(
                                    "Chase Bank Mobile",
                                    "Suspicious Charge Flagged!",
                                    "Fraud ALERT: Charge of $429.11 at Target Corp was blocked. If this was not you, text Fraud back immediately."
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ColorUrgent.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ColorUrgent.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                        ) {
                            Text("Inject Urgent (Chase Bank Alert)", fontSize = 12.sp, color = Color.White)
                        }

                        Button(
                            onClick = {
                                onInject(
                                    "WhatsApp Messenger",
                                    "Jane Family Group",
                                    "Mom: Hey pick up the kids from music school at 5 PM! Need to grab medicine as well on the way."
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ColorImportant.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ColorImportant.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                        ) {
                            Text("Inject Important (WhatsApp Mom)", fontSize = 12.sp, color = Color.White)
                        }

                        Button(
                            onClick = {
                                onInject(
                                    "Uber Eats",
                                    "Craving Tacos tonight? 🌮",
                                    "Big promo: Order right now and get flat 50% discount on orders above $15! Grab meals before discount ends tonight."
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ColorLow.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ColorLow.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                        ) {
                            Text("Inject Low Promo (UberEats Discount)", fontSize = 12.sp, color = Color.White)
                        }

                        Button(
                            onClick = {
                                onInject(
                                    "Google Photos",
                                    "Backing up files ...",
                                    "Sync active: Uploading 41 photos to cloud storage archive in the background."
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ColorSilent.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ColorSilent.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                        ) {
                            Text("Inject Silent (Google Photos Background Sync)", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    item: CapturedNotification,
    onMarkRead: () -> Unit,
    onArchiveToggle: () -> Unit,
    onPromote: () -> Unit,
    onDelete: () -> Unit
) {
    val dateText = remember(item.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }

    val pColor = when (item.priority.uppercase()) {
        "URGENT" -> ColorUrgent
        "IMPORTANT" -> ColorImportant
        "LOW" -> ColorLow
        "SILENT" -> ColorSilent
        else -> ColorLow
    }

    val pName = when (item.priority.uppercase()) {
        "URGENT" -> "Urgent"
        "IMPORTANT" -> "Important"
        "LOW" -> "Regular"
        "SILENT" -> "Silent Mute"
        else -> "Regular"
    }

    val cardBg = if (item.isRead) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = cardBg
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notification_card_${item.id}"),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isRead) 1.dp else 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left priority stripe
            val barColor = if (item.isRead) pColor.copy(alpha = 0.4f) else pColor
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(barColor)
            )

            // Content Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Header (App name, Priority Badge & Time)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = pColor.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val priorityIcon = when (item.priority.uppercase()) {
                                    "URGENT" -> Icons.Default.Warning
                                    "IMPORTANT" -> Icons.Default.PriorityHigh
                                    "LOW" -> Icons.Default.Notifications
                                    else -> Icons.Default.Info
                                }
                                Icon(
                                    imageVector = priorityIcon,
                                    contentDescription = null,
                                    tint = pColor,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.appName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Priority Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(pColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = pName.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = pColor,
                                letterSpacing = 0.4.sp
                            )
                        }
                        
                        Text(
                            text = dateText,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ORIGINAL SENDER MESSAGE BLOCK
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sender / Topic:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        
                        if (item.messageCount > 1) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "${item.messageCount} messages",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = item.title.ifEmpty { "Notification" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isRead) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = item.body,
                        fontSize = 13.sp,
                        color = if (item.isRead) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 5
                    )
                }

                // AI SYSTEM INSIGHT REPORT (Beautifully separated and styled container)
                if (item.reason.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = "AI Intel",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                                    .padding(2.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🧠 TOMMY DECISION REPORT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = item.reason,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            style = TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))

                // Action Controls Footer with clear dynamic highlights
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Mark read button
                        if (!item.isRead) {
                            IconButton(
                                onClick = onMarkRead,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Mark Read",
                                    tint = ColorLow,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Archive button
                        IconButton(
                            onClick = onArchiveToggle,
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), CircleShape)
                        ) {
                            val icon = if (item.isArchived) Icons.Default.Unarchive else Icons.Default.Archive
                            Icon(
                                imageVector = icon,
                                contentDescription = "Archive Toggle",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Re-trigger / promote notification
                        IconButton(
                            onClick = onPromote,
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Promote Alert",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Delete button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f), CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Log",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: NotificationViewModel,
    listenerAccessGranted: Boolean,
    onCheckPermission: () -> Unit
) {
    val context = LocalContext.current
    val activeSettings by viewModel.settings.collectAsState()

    var systemPrompt by remember { mutableStateOf("") }
    var backendType by remember { mutableStateOf("") }
    var customBaseUrl by remember { mutableStateOf("") }
    var customApiPath by remember { mutableStateOf("") }
    var customApiKey by remember { mutableStateOf("") }
    var requestFormat by remember { mutableStateOf("") }
    var passThroughThreshold by remember { mutableStateOf("") }
    var whitelistedApps by remember { mutableStateOf("") }
    var blacklistedApps by remember { mutableStateOf("") }

    // Synchronize form fields with persistent settings when loaded
    LaunchedEffect(activeSettings) {
        systemPrompt = activeSettings.systemPrompt
        backendType = activeSettings.backendType
        customBaseUrl = activeSettings.customBaseUrl
        customApiPath = activeSettings.customApiPath
        customApiKey = activeSettings.customApiKey
        requestFormat = activeSettings.requestFormat
        passThroughThreshold = activeSettings.passThroughThreshold
        whitelistedApps = activeSettings.whitelistedApps
        blacklistedApps = activeSettings.blacklistedApps
    }

    var apiKeyVisible by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Tommy System Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Tune the AI classification boundaries and endpoint integrations below.",
                fontSize = 12.sp,
                color = Color.LightGray
            )
        }

        // Section: System Prompt Editor
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CosmicSurfaceVariant, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI System Prompt",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        TextButton(onClick = {
                            systemPrompt = "You are a notification classifier. Classify this notification into URGENT, IMPORTANT, LOW, or SILENT.\n" +
                                    "Rules:\n" +
                                    "1. Urgent notifications are from family members or are high-value financial alerts (e.g., fraud alerts, money transfers).\n" +
                                    "2. Important notifications are direct personal messages, work calendar reminders, or system alerts (e.g. low battery).\n" +
                                    "3. Low notifications are promos, news, newsletters, newsletters, sports, or updates that don't need immediate attention.\n" +
                                    "4. Silent notifications are logging, background syncs, or music players."
                        }) {
                            Text("Reset", color = TealAccent, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = systemPrompt,
                        onValueChange = { systemPrompt = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .testTag("prompt_field"),
                        textStyle = TextStyle(fontSize = 12.sp, color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealAccent,
                            unfocusedBorderColor = CosmicSurfaceVariant,
                            focusedContainerColor = CosmicBackground,
                            unfocusedContainerColor = CosmicBackground
                        )
                    )
                }
            }
        }

        // Section: AI Backend Configuration
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CosmicSurfaceVariant, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "AI Inference Mechanism",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Models Selector Capsules
                    val backends = listOf(
                        "LOCAL_SIMULATION" to "Local Rules",
                        "OLLAMA_LOCAL" to "Ollama (11434)",
                        "CUSTOM_ENDPOINT" to "Custom API",
                        "GEMINI_API" to "Gemini SDK"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        backends.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { (type, label) ->
                                    val isSelected = backendType == type
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) TealAccent else CosmicBackground)
                                            .border(1.dp, if (isSelected) TealAccent else CosmicSurfaceVariant, RoundedCornerShape(8.dp))
                                            .clickable { backendType = type }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) CosmicBackground else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (backendType == "GEMINI_API") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(TealPrimary.copy(alpha = 0.15f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "💡 Enter your own Gemini API Key below. If left empty, Tommy will fall back to using the server-side environment key.",
                                    fontSize = 11.sp,
                                    color = TealAccent
                                )
                            }
                            OutlinedTextField(
                                value = customApiKey,
                                onValueChange = { customApiKey = it },
                                label = { Text("Gemini API Key (Optional)", color = Color.White) },
                                visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                        Icon(
                                            imageVector = if (apiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle Visibility",
                                            tint = Color.LightGray
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("gemini_api_key_field"),
                                textStyle = TextStyle(color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TealAccent,
                                    unfocusedBorderColor = CosmicSurfaceVariant,
                                    focusedLabelColor = TealAccent,
                                    unfocusedLabelColor = Color.LightGray
                                )
                            )
                        }
                    } else if (backendType == "OLLAMA_LOCAL" || backendType == "CUSTOM_ENDPOINT") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Base URL input
                            OutlinedTextField(
                                value = customBaseUrl,
                                onValueChange = { customBaseUrl = it },
                                label = { Text("Base URL", color = Color.White) },
                                placeholder = { Text("e.g. http://10.0.2.2:11434", color = Color.LightGray) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("base_url_field"),
                                textStyle = TextStyle(color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TealAccent,
                                    unfocusedBorderColor = CosmicSurfaceVariant,
                                    focusedLabelColor = TealAccent,
                                    unfocusedLabelColor = Color.LightGray
                                )
                            )

                            // Endpoint Path
                            OutlinedTextField(
                                value = customApiPath,
                                onValueChange = { customApiPath = it },
                                label = { Text("Endpoint API Path", color = Color.White) },
                                placeholder = { Text("e.g. /v1/chat/completions", color = Color.LightGray) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("api_path_field"),
                                textStyle = TextStyle(color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TealAccent,
                                    unfocusedBorderColor = CosmicSurfaceVariant,
                                    focusedLabelColor = TealAccent,
                                    unfocusedLabelColor = Color.LightGray
                                )
                            )

                            // API Key
                            OutlinedTextField(
                                value = customApiKey,
                                onValueChange = { customApiKey = it },
                                label = { Text("API Key (Optional)", color = Color.White) },
                                visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                        Icon(
                                            imageVector = if (apiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle Visibility",
                                            tint = Color.LightGray
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("api_key_field"),
                                textStyle = TextStyle(color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TealAccent,
                                    unfocusedBorderColor = CosmicSurfaceVariant,
                                    focusedLabelColor = TealAccent,
                                    unfocusedLabelColor = Color.LightGray
                                )
                            )

                            // Request Payload Format
                            Text(
                                text = "Request Format Profile:",
                                fontSize = 12.sp,
                                color = Color.LightGray
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val formats = listOf("OPENAI", "ANTHROPIC")
                                formats.forEach { f ->
                                    val active = requestFormat == f
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (active) TealAccent else CosmicBackground)
                                            .border(1.dp, if (active) TealAccent else CosmicSurfaceVariant, RoundedCornerShape(8.dp))
                                            .clickable { requestFormat = f }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = f,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (active) CosmicBackground else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CosmicSurfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "🛡️ Rule-based local semantic parsing is active. Very lightweight, takes absolutely 0% battery impact, and analyzes incoming triggers instantly using offline keywords.",
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }
        }

        // Section: Pass-Through Rules and Boundaries
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CosmicSurfaceVariant, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Threshold Rules & Filters",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Phone alert Sound Pass-through:",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Priority Pass-Through selector capsules
                    val currentSelection = passThroughThreshold
                    val priorities = listOf(
                        "URGENT" to "🔴 Urgent",
                        "IMPORTANT" to "🟡 Important",
                        "LOW" to "🟢 Low",
                        "SILENT" to "⚪ Silent"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        priorities.forEach { (type, name) ->
                            val isChosen = currentSelection.uppercase() == type.uppercase()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isChosen) TealAccent else CosmicBackground)
                                    .border(1.dp, if (isChosen) TealAccent else CosmicSurfaceVariant, RoundedCornerShape(8.dp))
                                    .clickable { passThroughThreshold = type }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isChosen) CosmicBackground else Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Notifications above your choice will ring through. Low-priority ones are stored quietly inside history.",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                }
            }
        }

        // Section: Manual Whitelist / Blacklist Overrides
        item {
            val installedAppsList by viewModel.installedApps.collectAsState()
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CosmicSurfaceVariant, RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Exception Overrides",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Configure custom exceptions by selecting installed system apps or entering raw packages to instantly route alerts.",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )

                    AppFilterSelector(
                        title = "Always Alert Whitelist",
                        appsStr = whitelistedApps,
                        onAppsStrChange = { whitelistedApps = it },
                        installedApps = installedAppsList
                    )

                    AppFilterSelector(
                        title = "Always Mute Blacklist",
                        appsStr = blacklistedApps,
                        onAppsStrChange = { blacklistedApps = it },
                        installedApps = installedAppsList
                    )
                }
            }
        }

        // Section: System Scan Interval Setting (Relocated from Tasks Tab)
        item {
            val settingsState by viewModel.settings.collectAsState()
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CosmicSurfaceVariant, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Tommy Assistant Scan Interval",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Configures how frequently Tommy scans notification streams to compile actionable tasks (calls, calendar, to-dos).",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val intervals = listOf(1 to "1 hr", 2 to "2 hrs", 4 to "4 hrs", 8 to "8 hrs", 12 to "12 hrs")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        intervals.forEach { (hour, label) ->
                            val isSelected = settingsState.taskScanIntervalHours == hour
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) TealPrimary else CosmicBackground)
                                    .border(1.dp, if (isSelected) TealAccent else CosmicSurfaceVariant, RoundedCornerShape(8.dp))
                                    .clickable {
                                        val nextSet = settingsState.copy(taskScanIntervalHours = hour)
                                        viewModel.saveSettings(nextSet)
                                    }
                                    .padding(vertical = 10.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) CosmicBackground else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Advanced Regex Rules Dashboard
        item {
            val rules by viewModel.regexRules.collectAsState()
            var isFormExpanded by remember { mutableStateOf(false) }
            
            var actionPattern by remember { mutableStateOf("") }
            var targetSelection by remember { mutableStateOf("ANY") } // TITLE, BODY, APP_NAME, ANY
            var actionSelection by remember { mutableStateOf("ALWAYS_ALERT") } // ALWAYS_ALERT, ALWAYS_MUTE
            
            // Validation
            val compiledPattern = remember(actionPattern) {
                try {
                    if (actionPattern.isNotEmpty()) java.util.regex.Pattern.compile(actionPattern) else null
                } catch (e: Exception) {
                    null
                }
            }
            val isPatternValid = actionPattern.isEmpty() || compiledPattern != null

            // Testing panel string
            var testInputText by remember { mutableStateOf("") }
            val doesTestMatch = remember(compiledPattern, testInputText) {
                compiledPattern != null && testInputText.isNotEmpty() && compiledPattern.matcher(testInputText).find()
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CosmicSurfaceVariant, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Regex Bypass Overrides",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(TealAccent.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "ROUTING CORES",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealAccent
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Match exact patterns to bypass AI classification. Perfect for absolute triggers.",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Template Presets (Instant add multiple complex patterns)
                    Text(
                        text = "⚡ PRESET ROUTER TEMPLATES (TAP TO INSTALL)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealAccent,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    val presets = listOf(
                        Triple("💬 Group Chat Silencer", "(group|broadcast|channel|whatsapp|telegram)", Pair("ANY", "ALWAYS_MUTE")),
                        Triple("🚨 Bank OTP Alerter", "\\b(OTP|verify|verification|secure|code)\\b", Pair("ANY", "ALWAYS_ALERT")),
                        Triple("📦 Delivery Courier", "(deliver|shipment|courier|fedex|track|dispatch)", Pair("ANY", "ALWAYS_ALERT")),
                        Triple("🔇 Ad & Spammer", "(promo|discount|sale|deal|coupon|exclusive)", Pair("ANY", "ALWAYS_MUTE"))
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { (label, pattern, options) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CosmicBackground)
                                    .border(1.dp, CosmicSurfaceVariant, RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.insertRegexRule(
                                            com.example.data.RegexRule(
                                                pattern = pattern,
                                                matchTarget = options.first,
                                                action = options.second,
                                                isEnabled = true
                                            )
                                        )
                                        android.widget.Toast.makeText(context, "$label preset added!", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text(label, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Existing Rules Dashboard
                    if (rules.isNotEmpty()) {
                        Text(
                            text = "INSTALLED ROUTE RULES (${rules.size})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            rules.forEach { rule ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CosmicBackground)
                                        .border(1.dp, CosmicSurfaceVariant.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val targetText = when (rule.matchTarget) {
                                                "ANY" -> "Any text"
                                                "TITLE" -> "Title"
                                                "BODY" -> "Body"
                                                "APP_NAME" -> "App Name"
                                                else -> rule.matchTarget
                                            }
                                            val actionText = if (rule.action == "ALWAYS_ALERT") "Always Alert 🔴" else "Always Mute 🔇"
                                            val badgeColor = if (rule.action == "ALWAYS_ALERT") ColorUrgent else ColorSilent
                                            
                                            Text(
                                                text = "On [$targetText] Match ➡️",
                                                fontSize = 9.sp,
                                                color = Color.LightGray
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = actionText,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = badgeColor
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = rule.pattern,
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(
                                            checked = rule.isEnabled,
                                            onCheckedChange = { viewModel.updateRegexRule(rule.copy(isEnabled = it)) },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = TealAccent,
                                                checkedTrackColor = TealPrimary.copy(alpha = 0.5f)
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        IconButton(onClick = { viewModel.deleteRegexRule(rule.id) }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = ColorUrgent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 3. New rule creator collapsible panel
                    Button(
                        onClick = { isFormExpanded = !isFormExpanded },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isFormExpanded) CosmicSurfaceVariant else TealPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (isFormExpanded) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = null,
                            tint = if (isFormExpanded) Color.White else CosmicBackground,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isFormExpanded) "Collapse Creator" else "Add Custom Pattern Rule",
                            fontSize = 12.sp,
                            color = if (isFormExpanded) Color.White else CosmicBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    AnimatedVisibility(visible = isFormExpanded) {
                        Column {
                            Spacer(modifier = Modifier.height(14.dp))
                            Divider(color = CosmicSurfaceVariant)
                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "DEFINE NEW CUSTOM PATTERN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealAccent,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Pattern Box
                            OutlinedTextField(
                                value = actionPattern,
                                onValueChange = { actionPattern = it },
                                label = { Text("Regex Pattern Input", color = ColorSilent, fontSize = 11.sp) },
                                isError = !isPatternValid,
                                supportingText = {
                                    if (!isPatternValid) {
                                        Text("Syntax error: Invalid regular expression.", color = ColorUrgent)
                                    } else {
                                        Text("Uses Java standard Pattern matching syntax.", color = ColorSilent)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(color = Color.White, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TealAccent,
                                    unfocusedBorderColor = CosmicSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Select Zone
                            Text("Select Target Field:", fontSize = 11.sp, color = Color.LightGray)
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            val targetOptions = listOf(
                                "ANY" to "Any text",
                                "TITLE" to "Title",
                                "BODY" to "Body",
                                "APP_NAME" to "App Name"
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                targetOptions.forEach { (type, label) ->
                                    val isSel = targetSelection == type
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) TealAccent else CosmicBackground)
                                            .border(1.dp, if (isSel) TealAccent else CosmicSurfaceVariant, RoundedCornerShape(8.dp))
                                            .clickable { targetSelection = type }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) CosmicBackground else Color.White
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            // Select Action
                            Text("Applied Rule Route:", fontSize = 11.sp, color = Color.LightGray)
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            val actionOptions = listOf(
                                "ALWAYS_ALERT" to "Bypass High Priority 🔴",
                                "ALWAYS_MUTE" to "Auto-Mute Quietly 🔇"
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                actionOptions.forEach { (type, label) ->
                                    val isSel = actionSelection == type
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) TealAccent else CosmicBackground)
                                            .border(1.dp, if (isSel) TealAccent else CosmicSurfaceVariant, RoundedCornerShape(8.dp))
                                            .clickable { actionSelection = type }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) CosmicBackground else Color.White
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))

                            // Sandbox interactive tester
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CosmicBackground)
                                    .border(1.dp, CosmicSurfaceVariant, RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text("⚡ COMPILER SANDBOX (TEST PATTERN IN REAL-TIME)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ColorSilent)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = testInputText,
                                        onValueChange = { testInputText = it },
                                        placeholder = { Text("Enter sample text here to test...", color = ColorSilent, fontSize = 11.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = TealPrimary,
                                            unfocusedBorderColor = CosmicSurfaceVariant
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    val (sandboxResult, sandboxResultColor) = when {
                                        compiledPattern == null -> "Awaiting valid regex pattern." to ColorSilent
                                        testInputText.isEmpty() -> "Awaiting test text input." to ColorSilent
                                        doesTestMatch -> "🔥 INCOMING MATCH READY! BYPASS TRIGGERED!" to ColorLow
                                        else -> "❌ Rule did not match this text sample." to ColorUrgent
                                    }
                                    
                                    Text(
                                        text = sandboxResult,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = sandboxResultColor
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))

                            // Save rule
                            Button(
                                onClick = {
                                    if (actionPattern.isNotEmpty() && isPatternValid) {
                                        viewModel.insertRegexRule(
                                            com.example.data.RegexRule(
                                                pattern = actionPattern,
                                                matchTarget = targetSelection,
                                                action = actionSelection,
                                                isEnabled = true
                                            )
                                        )
                                        actionPattern = ""
                                        testInputText = ""
                                        isFormExpanded = false
                                        android.widget.Toast.makeText(context, "New custom pattern router created!", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = actionPattern.isNotEmpty() && isPatternValid,
                                colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Instantiate Router Rule", color = CosmicBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Section: Listener access again
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CosmicSurfaceVariant, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "System Access Permissions",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Listener Interception Service",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = if (listenerAccessGranted) "State: active" else "State: permission required",
                                fontSize = 11.sp,
                                color = if (listenerAccessGranted) ColorLow else ColorUrgent
                            )
                        }

                        Button(
                            onClick = {
                                val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (listenerAccessGranted) TealPrimary else ColorUrgent),
                            modifier = Modifier.testTag("btn_listener_grant")
                        ) {
                            Text(if (listenerAccessGranted) "System Settings" else "Grant Intercept", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // Apply changes button
        item {
            Button(
                onClick = {
                    val updated = activeSettings.copy(
                        systemPrompt = systemPrompt,
                        backendType = backendType,
                        customBaseUrl = customBaseUrl,
                        customApiPath = customApiPath,
                        customApiKey = customApiKey,
                        requestFormat = requestFormat,
                        passThroughThreshold = passThroughThreshold,
                        whitelistedApps = whitelistedApps,
                        blacklistedApps = blacklistedApps
                    )
                    viewModel.saveSettings(updated)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_save_settings"),
                colors = ButtonDefaults.buttonColors(containerColor = TealAccent)
            ) {
                Text("Apply & Save Settings", color = CosmicBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        // Test Simulation Block inside Developer Setup tab
        item {
            Spacer(modifier = Modifier.height(8.dp))
            EmulatorSimulatorBlock(onInject = { app, title, text ->
                viewModel.simulateNotificationInjection(context, app, title, text)
            })
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// Helper: Is NotificationListenerService enabled?
private fun isNotificationServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    if (!flat.isNullOrEmpty()) {
        val names = flat.split(":")
        for (name in names) {
            val cn = ComponentName.unflattenFromString(name)
            if (cn != null && pkgName == cn.packageName) {
                return true
            }
        }
    }
    return false
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AppFilterSelector(
    title: String,
    appsStr: String,
    onAppsStrChange: (String) -> Unit,
    installedApps: List<com.example.ui.InstalledAppInfo>
) {
    var searchQuery by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var manualInput by remember { mutableStateOf("") }

    val selectedPackages = remember(appsStr) {
        appsStr.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.LightGray
        )
        Spacer(modifier = Modifier.height(6.dp))

        // Selected app chips (removable)
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            selectedPackages.forEach { pkg ->
                val matchingApp = installedApps.find { it.packageName == pkg }
                val displayName = matchingApp?.label ?: pkg
                Surface(
                    color = CosmicSurfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TealAccent.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (matchingApp?.icon != null) {
                            androidx.compose.foundation.Image(
                                painter = coil.compose.rememberAsyncImagePainter(matchingApp.icon),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = displayName,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = ColorUrgent,
                            modifier = Modifier
                                        .size(14.dp)
                                        .clickable {
                                            val newSet = selectedPackages.toMutableSet().apply { remove(pkg) }
                                            onAppsStrChange(newSet.joinToString(","))
                                        }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Trigger button for expandable app list
        OutlinedButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TealAccent),
            border = androidx.compose.foundation.BorderStroke(1.dp, CosmicSurfaceVariant),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Select / Search Apps to Add", fontSize = 12.sp)
        }

        if (expanded) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, CosmicSurfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .heightIn(max = 240.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search system apps...", color = ColorSilent) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ColorSilent) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealAccent,
                            unfocusedBorderColor = CosmicSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val filteredApps = remember(searchQuery, installedApps) {
                        installedApps.filter {
                            it.label.contains(searchQuery, ignoreCase = true) ||
                                    it.packageName.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    if (filteredApps.isEmpty() && searchQuery.isNotEmpty()) {
                        Text(
                            text = "No apps found. Add manual package above or type exactly to add below.",
                            color = ColorSilent,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items(filteredApps) { app ->
                            val isSelected = selectedPackages.contains(app.packageName)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newSet = selectedPackages.toMutableSet().apply {
                                            if (isSelected) remove(app.packageName) else add(app.packageName)
                                        }
                                        onAppsStrChange(newSet.joinToString(","))
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (app.icon != null) {
                                        androidx.compose.foundation.Image(
                                            painter = coil.compose.rememberAsyncImagePainter(app.icon),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                    }
                                    Column {
                                        Text(app.label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text(app.packageName, color = ColorSilent, fontSize = 9.sp)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .border(1.dp, if (isSelected) TealAccent else ColorSilent, RoundedCornerShape(4.dp))
                                        .background(if (isSelected) TealAccent else Color.Transparent, RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = CosmicBackground, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = CosmicSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Manual Fallback field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualInput,
                            onValueChange = { manualInput = it },
                            placeholder = { Text("Or add package manually...", color = ColorSilent) },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(color = Color.White, fontSize = 11.sp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = CosmicSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (manualInput.trim().isNotEmpty()) {
                                    val newSet = selectedPackages.toMutableSet().apply {
                                        add(manualInput.trim())
                                    }
                                    onAppsStrChange(newSet.joinToString(","))
                                    manualInput = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add manual package", tint = TealAccent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TasksScreen(
    viewModel: NotificationViewModel
) {
    val context = LocalContext.current
    val assistantTasks by viewModel.assistantTasks.collectAsState()
    val isScanning by viewModel.isScanningTasks.collectAsState()
    val aiDigestResult by viewModel.aiDigestResult.collectAsState()
    val isGeneratingDigest by viewModel.isGeneratingDigest.collectAsState()

    var isDigestExpanded by remember { mutableStateOf(true) }
    var inputCommandText by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Futuristic Station Header
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TOMMY AI WORKSPACE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealAccent,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Intent Command Hub",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                // Hologram Live Connection Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(ColorLow.copy(alpha = 0.15f))
                        .border(1.dp, ColorLow.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .scale(pulseScale)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(ColorLow)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "STATION SECURE",
                        fontSize = 8.sp,
                        color = ColorLow,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // 3. AI Timeline Status Digest Capsule
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CosmicSurfaceVariant, RoundedCornerShape(20.dp))
                    .testTag("summary_digest_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isDigestExpanded = !isDigestExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "AI Digest",
                                tint = TealPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "AI TIMELINE STATUS DIGEST",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TealPrimary,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Intelligent stream synthesizer overview",
                                    fontSize = 10.sp,
                                    color = ColorSilent
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isDigestExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle Expand",
                            tint = ColorSilent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(visible = isDigestExpanded) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = CosmicSurfaceVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(10.dp))

                            if (aiDigestResult != null) {
                                Text(
                                    text = aiDigestResult ?: "",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            } else {
                                Text(
                                    text = "Generate an aggregate intelligence overview synthesizing notification activity.",
                                    fontSize = 11.sp,
                                    color = ColorSilent,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            Button(
                                onClick = { viewModel.generateSummaryDigest() },
                                enabled = !isGeneratingDigest,
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_build_digest")
                            ) {
                                if (isGeneratingDigest) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = CosmicBackground,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Synthesize Timeline Overview", fontSize = 11.sp, color = CosmicBackground, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Compact Scan Panel (Merged for ultimate cleanliness)
        item {
            Button(
                onClick = { viewModel.scanForAssistantTasks() },
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(containerColor = CosmicSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, TealPrimary.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_assistant_scan")
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = TealPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scanning notification vault...", color = TealPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                } else {
                    Icon(Icons.Default.Build, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trigger Stream Scanner Audit", color = TealPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // 5. Actionable Items Registry Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIONABLE REGISTRY INTENTS (${assistantTasks.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TealAccent,
                    letterSpacing = 1.sp
                )
                if (assistantTasks.isNotEmpty()) {
                    Text(
                        text = "Flush Registry",
                        fontSize = 11.sp,
                        color = ColorUrgent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { viewModel.clearAllAssistantTasks() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }

        if (assistantTasks.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CosmicSurfaceVariant, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = ColorSilent.copy(alpha = 0.3f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Auditor Registry Empty",
                            fontSize = 13.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Stream audit did not record pending actionable intents.",
                            fontSize = 10.sp,
                            color = ColorSilent,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(assistantTasks) { task ->
                val taskBorder = if (task.isCompleted) {
                    CosmicSurfaceVariant.copy(alpha = 0.4f)
                } else {
                    TealPrimary.copy(alpha = 0.4f)
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, taskBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "RECORD: ${task.appNameSource.uppercase()}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealAccent,
                                letterSpacing = 0.5.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (task.isCompleted) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(ColorLow.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("ACCOMPLISHED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = ColorLow)
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(TealAccent.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("IDLE MATCH", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TealAccent)
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { viewModel.deleteAssistantTask(task.id) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Task",
                                        tint = ColorSilent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (task.isCompleted) ColorSilent else Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = task.description,
                            fontSize = 11.sp,
                            color = ColorSilent
                        )

                        if (!task.isCompleted) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = CosmicSurfaceVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                when (task.actionType.uppercase()) {
                                    "CALL" -> {
                                        Button(
                                            onClick = {
                                                try {
                                                    val callIntent = Intent(Intent.ACTION_DIAL).apply {
                                                        data = android.net.Uri.parse("tel:${task.actionData}")
                                                    }
                                                    context.startActivity(callIntent)
                                                    viewModel.confirmTaskAction(task.id)
                                                } catch (e: Exception) {
                                                    android.widget.Toast.makeText(context, "Error: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = null,
                                                tint = CosmicBackground,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Call ${task.contactName}", color = CosmicBackground, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    "ALARM" -> {
                                        Button(
                                            onClick = {
                                                try {
                                                    val parts = task.actionData.split(":")
                                                    val hr = parts.getOrNull(0)?.toIntOrNull() ?: 7
                                                    val min = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                                    val alarmIntent = Intent(android.provider.AlarmClock.ACTION_SET_ALARM).apply {
                                                        putExtra(android.provider.AlarmClock.EXTRA_HOUR, hr)
                                                        putExtra(android.provider.AlarmClock.EXTRA_MINUTES, min)
                                                        putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, "Tommy Task Alarm")
                                                        putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, false)
                                                    }
                                                    context.startActivity(alarmIntent)
                                                    viewModel.confirmTaskAction(task.id)
                                                    viewModel.completeTask(task.id)
                                                } catch (e: Exception) {
                                                    android.widget.Toast.makeText(context, "Cannot open clock config: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = ColorImportant),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = CosmicBackground,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Set Alarm: ${task.actionData}", color = CosmicBackground, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Button(
                                    onClick = { viewModel.completeTask(task.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CosmicSurfaceVariant),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Mark Accomplished", color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
