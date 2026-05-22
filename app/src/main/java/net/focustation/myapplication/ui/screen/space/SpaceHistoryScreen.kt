package net.focustation.myapplication.ui.screen.space

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.util.FusedLocationSource
import net.focustation.myapplication.data.model.SpaceRecord
import net.focustation.myapplication.ui.components.MainBottomDestination
import net.focustation.myapplication.ui.components.MainBottomNavigationBar
import net.focustation.myapplication.ui.components.ReferenceDesignTokens
import net.focustation.myapplication.ui.theme.ColorFocus
import net.focustation.myapplication.ui.theme.ColorLight
import net.focustation.myapplication.ui.theme.ColorNoise
import net.focustation.myapplication.ui.theme.ColorVibration
import net.focustation.myapplication.ui.theme.FocusInk
import net.focustation.myapplication.ui.theme.FocustationTheme
import net.focustation.myapplication.util.DebugLog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceHistoryScreen(
    onBack: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToReport: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: SpaceHistoryViewModel = viewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isNaverMapMcpIdConfigured = remember(context) { context.hasNaverMapMcpIdConfigured() }
    val uiState by viewModel.uiState.collectAsState()
    var hasLocationPermission by remember { mutableStateOf(context.hasLocationPermission()) }
    var requestedLocationPermission by rememberSaveable { mutableStateOf(false) }

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionResult ->
            hasLocationPermission = permissionResult.any { it.value }
        }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    hasLocationPermission = context.hasLocationPermission()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(hasLocationPermission, requestedLocationPermission) {
        if (!hasLocationPermission && !requestedLocationPermission) {
            requestedLocationPermission = true
            locationPermissionLauncher.launch(LOCATION_PERMISSIONS)
        }
    }

    val selectedRecord =
        remember(uiState.selectedSpaceId, uiState.spaceRecords) {
            uiState.selectedSpaceId?.let { selectedId ->
                uiState.spaceRecords.find { it.id == selectedId }
            }
        }

    Scaffold(
        bottomBar = {
            MainBottomNavigationBar(
                selected = MainBottomDestination.MAP,
                onTabClick = { destination ->
                    when (destination) {
                        MainBottomDestination.HOME -> onNavigateToHome()
                        MainBottomDestination.REPORT -> onNavigateToReport()
                        MainBottomDestination.MAP -> Unit
                        MainBottomDestination.SETTINGS -> onNavigateToSettings()
                    }
                },
            )
        },
        topBar = {
            TopAppBar(
                title = { Text("공간 기록", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = ReferenceDesignTokens.Screen,
                        titleContentColor = FocusInk,
                        navigationIconContentColor = FocusInk,
                    ),
            )
        },
        containerColor = ReferenceDesignTokens.Screen,
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(ReferenceDesignTokens.Screen),
        ) {
            NaverMapSection(
                records = uiState.spaceRecords,
                selectedId = uiState.selectedSpaceId,
                hasLocationPermission = hasLocationPermission,
                onPinClick = { viewModel.selectSpace(it) },
                modifier = Modifier.fillMaxSize(),
            )

            MapNoticeLayer(
                isNaverMapMcpIdConfigured = isNaverMapMcpIdConfigured,
                hasLocationPermission = hasLocationPermission,
                errorMessage = uiState.errorMessage,
                onRequestLocationPermission = { locationPermissionLauncher.launch(LOCATION_PERMISSIONS) },
                modifier = Modifier.align(Alignment.TopCenter),
            )

            selectedRecord?.let {
                SpaceDetailPopup(
                    record = it,
                    onDismiss = { viewModel.selectSpace(null) },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun MapNoticeLayer(
    isNaverMapMcpIdConfigured: Boolean,
    hasLocationPermission: Boolean,
    errorMessage: String?,
    onRequestLocationPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!isNaverMapMcpIdConfigured) {
            ElevatedCard(
                colors =
                    CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
            ) {
                Text(
                    text = "NAVER_MAP_MCP_ID가 비어 있어요. local.properties 또는 gradle.properties를 확인해주세요.",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        if (errorMessage != null) {
            ElevatedCard(
                colors =
                    CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
            ) {
                Text(
                    text = errorMessage,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        if (!hasLocationPermission) {
            ElevatedCard(
                colors =
                    CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("현재 위치 추적을 위해 위치 권한이 필요해요.")
                    TextButton(onClick = onRequestLocationPermission) {
                        Text("권한 허용")
                    }
                }
            }
        }
    }
}

@Composable
private fun NaverMapSection(
    records: List<SpaceRecord>,
    selectedId: String?,
    hasLocationPermission: Boolean,
    onPinClick: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = remember(context) { context.findActivity() }
    val onPinClickState by rememberUpdatedState(onPinClick)
    val latestRecords by rememberUpdatedState(records)

    val mapView =
        remember {
            try {
                DebugLog.d("Creating MapView...")
                MapView(context)
            } catch (e: Exception) {
                DebugLog.e("Failed to create MapView", e)
                null
            }
        }

    LaunchedEffect(mapView) {
        val createdMapView = mapView ?: return@LaunchedEffect
        try {
            createdMapView.onCreate(null)
            DebugLog.d("MapView onCreate called successfully")
        } catch (e: Exception) {
            DebugLog.e("MapView onCreate failed", e)
        }
    }

    val locationSource =
        remember(activity) { activity?.let { FusedLocationSource(it, LOCATION_PERMISSION_REQUEST_CODE) } }
    var naverMap by remember { mutableStateOf<NaverMap?>(null) }
    val renderedMarkers = remember { mutableStateListOf<Marker>() }
    var movedToInitialCamera by remember { mutableStateOf(false) }
    var mapInitErrorMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner, mapView) {
        if (mapView == null) {
            return@DisposableEffect onDispose {}
        }
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START ->
                        runCatching { mapView.onStart() }.onFailure {
                            DebugLog.e("onStart error", it)
                        }

                    Lifecycle.Event.ON_RESUME ->
                        runCatching { mapView.onResume() }.onFailure {
                            DebugLog.e("onResume error", it)
                        }

                    Lifecycle.Event.ON_PAUSE ->
                        runCatching { mapView.onPause() }.onFailure {
                            DebugLog.e("onPause error", it)
                        }

                    Lifecycle.Event.ON_STOP ->
                        runCatching { mapView.onStop() }.onFailure {
                            DebugLog.e("onStop error", it)
                        }

                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            runCatching { mapView.onDestroy() }.onFailure { DebugLog.e("onDestroy error", it) }
            renderedMarkers.forEach { it.map = null }
            renderedMarkers.clear()
        }
    }

    LaunchedEffect(mapView) {
        if (mapView == null) return@LaunchedEffect
        mapView.getMapAsync { map ->
            try {
                naverMap = map
                movedToInitialCamera = false
                map.uiSettings.isLocationButtonEnabled = true
                map.setOnMapClickListener { _, _ -> onPinClickState(null) }
                latestRecords.firstOrNull()?.let { first ->
                    map.moveCamera(CameraUpdate.scrollTo(LatLng(first.latitude, first.longitude)))
                    movedToInitialCamera = true
                }
                DebugLog.d("Map initialized successfully")
                mapInitErrorMessage = null
            } catch (e: Exception) {
                DebugLog.e("Failed to initialize map", e)
                mapInitErrorMessage = "지도 초기화 중 오류: ${e.message}"
            }
        }
    }

    LaunchedEffect(naverMap, records, selectedId) {
        val map = naverMap ?: return@LaunchedEffect
        if (!movedToInitialCamera && selectedId == null) {
            records.firstOrNull()?.let { first ->
                map.moveCamera(CameraUpdate.scrollTo(LatLng(first.latitude, first.longitude)))
                movedToInitialCamera = true
            }
        }
    }

    LaunchedEffect(naverMap, records) {
        val naverMapInstance = naverMap ?: return@LaunchedEffect
        renderedMarkers.forEach { it.map = null }
        renderedMarkers.clear()

        records.forEach { record ->
            val marker =
                Marker().apply {
                    position = LatLng(record.latitude, record.longitude)
                    captionText = record.name
                    map = naverMapInstance
                    setOnClickListener {
                        onPinClickState(record.id)
                        true
                    }
                }
            renderedMarkers.add(marker)
        }
    }

    LaunchedEffect(naverMap, records, selectedId) {
        val naverMapInstance = naverMap ?: return@LaunchedEffect
        records.find { it.id == selectedId }?.let { selected ->
            naverMapInstance.moveCamera(CameraUpdate.scrollTo(LatLng(selected.latitude, selected.longitude)))
        }
    }

    LaunchedEffect(naverMap, hasLocationPermission, locationSource) {
        val map = naverMap ?: return@LaunchedEffect
        runCatching {
            if (locationSource != null && hasLocationPermission) {
                map.locationSource = locationSource
            }
        }
        runCatching {
            map.locationTrackingMode =
                if (hasLocationPermission) {
                    LocationTrackingMode.Follow
                } else {
                    LocationTrackingMode.None
                }
        }
    }

    if (mapView == null) {
        Box(
            modifier = modifier.background(Color(0xFFFFCDD2)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("지도 초기화 실패", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                Text(
                    text = mapInitErrorMessage ?: "네이버 지도 SDK를 초기화할 수 없습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    } else {
        AndroidView(
            modifier = modifier,
            factory = { mapView },
        )
    }
}

@Composable
private fun SpaceDetailPopup(
    record: SpaceRecord,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = record.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                            Modifier
                                .clip(CircleShape)
                                .background(ColorFocus.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "${record.avgFocusScore}점",
                            style = MaterialTheme.typography.labelLarge,
                            color = ColorFocus,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "닫기", modifier = Modifier.size(16.dp))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricText("소음", "%.0f dB".format(record.avgNoise), ColorNoise)
                MetricText("조도", "%.0f lux".format(record.avgIlluminance), ColorLight)
                MetricText("진동", "%.2f m/s²".format(record.avgVibration), ColorVibration)
            }
            Text(
                text = record.toSessionSummary(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MetricText(
    label: String,
    value: String,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
private const val NAVER_MAP_MCP_ID_META_KEY = "com.naver.maps.map.MCP_ID"

private val LOCATION_PERMISSIONS =
    arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

private fun Context.hasLocationPermission(): Boolean =
    LOCATION_PERMISSIONS.any {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private fun Context.hasNaverMapMcpIdConfigured(): Boolean =
    runCatching {
        val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        val clientId =
            appInfo.metaData
                ?.getString(NAVER_MAP_MCP_ID_META_KEY)
                .orEmpty()
                .trim()
        clientId.isNotEmpty() && !clientId.startsWith("\${")
    }.getOrDefault(false)

private fun SpaceRecord.toSessionSummary(): String = "세션 ${sessionCount}회 · 마지막 방문: $lastVisited"

@Preview(showBackground = true)
@Composable
private fun SpaceHistoryPreview() {
    FocustationTheme {
        SpaceHistoryScreen(onBack = {})
    }
}
