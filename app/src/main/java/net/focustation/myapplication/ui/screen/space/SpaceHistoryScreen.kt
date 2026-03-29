package net.focustation.myapplication.ui.screen.space

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.util.FusedLocationSource
import net.focustation.myapplication.data.model.SpaceRecord
import net.focustation.myapplication.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceHistoryScreen(
    onBack: () -> Unit,
    viewModel: SpaceHistoryViewModel = viewModel(),
) {
    val context = LocalContext.current
    val isNaverMapClientIdConfigured = remember(context) { context.hasNaverMapClientIdConfigured() }
    val uiState by viewModel.uiState.collectAsState()
    var hasLocationPermission by remember { mutableStateOf(context.hasLocationPermission()) }
    var requestedLocationPermission by rememberSaveable { mutableStateOf(false) }

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionResult ->
            hasLocationPermission = permissionResult.any { it.value }
        }

    LaunchedEffect(uiState.isMapView, hasLocationPermission, requestedLocationPermission) {
        if (uiState.isMapView && !hasLocationPermission && !requestedLocationPermission) {
            requestedLocationPermission = true
            locationPermissionLauncher.launch(LOCATION_PERMISSIONS)
        }
    }

    val sortedRecords =
        remember(uiState.spaceRecords, uiState.sortOption) {
            when (uiState.sortOption) {
                SpaceSortOption.SCORE -> uiState.spaceRecords.sortedByDescending { it.avgFocusScore }
                SpaceSortOption.PLACE -> uiState.spaceRecords.sortedBy { it.name }
                SpaceSortOption.DATE -> uiState.spaceRecords
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("공간 기반 이력", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleView() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = "뷰 전환",
                            tint = Color.White,
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0D1B4B),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White,
                    ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
        ) {
            // 지도/리스트 토글 탭
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = uiState.isMapView,
                    onClick = { if (!uiState.isMapView) viewModel.toggleView() },
                    label = { Text("🗺️  지도 뷰") },
                )
                FilterChip(
                    selected = !uiState.isMapView,
                    onClick = { if (uiState.isMapView) viewModel.toggleView() },
                    label = { Text("📋  리스트 뷰") },
                )
            }

            if (uiState.isMapView) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                ) {
                    NaverMapSection(
                        records = sortedRecords,
                        selectedId = uiState.selectedSpaceId,
                        hasLocationPermission = hasLocationPermission,
                        onPinClick = { viewModel.selectSpace(it) },
                        modifier = Modifier.fillMaxSize(),
                    )

                    Column(
                        modifier =
                            Modifier
                                .align(Alignment.TopCenter)
                                .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (!isNaverMapClientIdConfigured) {
                            ElevatedCard(
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            ) {
                                Text(
                                    text = "NAVER_MAP_CLIENT_ID가 비어 있어요. local.properties 또는 gradle.properties를 확인해주세요.",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }

                        if (!hasLocationPermission) {
                            ElevatedCard(
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text("내 위치 추적을 위해 위치 권한이 필요해요.")
                                    TextButton(onClick = { locationPermissionLauncher.launch(LOCATION_PERMISSIONS) }) {
                                        Text("권한 허용")
                                    }
                                }
                            }
                        }
                    }
                }

                // 선택된 장소 팝업 카드
                uiState.selectedSpaceId?.let { id ->
                    val record = uiState.spaceRecords.find { it.id == id }
                    record?.let {
                        SpaceDetailPopup(
                            record = it,
                            onDismiss = { viewModel.selectSpace(null) },
                        )
                    }
                }
            } else {
                // 정렬 옵션
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "정렬:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SpaceSortOption.entries.forEach { opt ->
                        FilterChip(
                            selected = uiState.sortOption == opt,
                            onClick = { viewModel.setSortOption(opt) },
                            label = {
                                Text(
                                    when (opt) {
                                        SpaceSortOption.DATE -> "날짜"
                                        SpaceSortOption.PLACE -> "장소"
                                        SpaceSortOption.SCORE -> "점수"
                                    },
                                    fontSize = 12.sp,
                                )
                            },
                        )
                    }
                }

                // 리스트
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(sortedRecords) { record ->
                        SpaceListCard(
                            record = record,
                            onClick = { viewModel.selectSpace(record.id) },
                        )
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

    // 네이버 지도 SDK 지연 초기화 (처음 지도 화면 진입 시)
    LaunchedEffect(Unit) {
        try {
            android.util.Log.d("NaverMap", "=== 네이버 지도 초기화 시작 ===")
            android.util.Log.d("NaverMap", "Package: ${context.packageName}")
            android.util.Log.d("NaverMap", "META_DATA_KEY: $NAVER_MAP_CLIENT_ID_META_KEY")

            // 메타데이터 확인
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_META_DATA
            )
            android.util.Log.d("NaverMap", "ApplicationInfo obtained")

            val metaData = appInfo.metaData
            if (metaData != null) {
                android.util.Log.d("NaverMap", "MetaData found, size: ${metaData.size()}")

                // 모든 메타데이터 확인
                val keys = metaData.keySet()
                for (key in keys) {
                    if (key.contains("naver", ignoreCase = true) || key.contains("map", ignoreCase = true)) {
                        android.util.Log.d("NaverMap", "  MetaData[$key] = ${metaData.get(key)}")
                    }
                }

                val clientId = metaData.getString(NAVER_MAP_CLIENT_ID_META_KEY)
                if (clientId != null && clientId.isNotEmpty()) {
                    android.util.Log.d("NaverMap", "✓ CLIENT_ID found: $clientId")
                } else {
                    android.util.Log.e("NaverMap", "✗ CLIENT_ID is null or empty!")
                }
            } else {
                android.util.Log.e("NaverMap", "✗ MetaData is null!")
            }

            android.util.Log.d("NaverMap", "Calling NaverMapSdk.getInstance(context)...")
            com.naver.maps.map.NaverMapSdk.getInstance(context)
            android.util.Log.d("NaverMap", "✓ NaverMapSdk initialized")
        } catch (e: Exception) {
            android.util.Log.e("NaverMap", "✗ NaverMapSdk init failed: ${e.message}", e)
        }
    }

    val mapView =
        remember {
            try {
                android.util.Log.d("NaverMap", "Creating MapView...")
                MapView(context)
            } catch (e: Exception) {
                android.util.Log.e("NaverMap", "Failed to create MapView", e)
                null
            }
        }

    LaunchedEffect(mapView) {
        if (mapView != null) {
            try {
                mapView.onCreate(null)
                android.util.Log.d("NaverMap", "MapView onCreate called successfully")
            } catch (e: Exception) {
                android.util.Log.e("NaverMap", "MapView onCreate failed", e)
            }
        }
    }

    val locationSource = remember(activity) { activity?.let { FusedLocationSource(it, LOCATION_PERMISSION_REQUEST_CODE) } }
    var naverMap by remember { mutableStateOf<NaverMap?>(null) }
    val renderedMarkers = remember { mutableStateListOf<Marker>() }
    var mapInitErrorMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner, mapView) {
        if (mapView == null) {
            return@DisposableEffect onDispose {}
        }
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> runCatching { mapView.onStart() }.onFailure { android.util.Log.e("NaverMap", "onStart error", it) }
                    Lifecycle.Event.ON_RESUME -> runCatching { mapView.onResume() }.onFailure { android.util.Log.e("NaverMap", "onResume error", it) }
                    Lifecycle.Event.ON_PAUSE -> runCatching { mapView.onPause() }.onFailure { android.util.Log.e("NaverMap", "onPause error", it) }
                    Lifecycle.Event.ON_STOP -> runCatching { mapView.onStop() }.onFailure { android.util.Log.e("NaverMap", "onStop error", it) }
                    Lifecycle.Event.ON_DESTROY -> runCatching { mapView.onDestroy() }.onFailure { android.util.Log.e("NaverMap", "onDestroy error", it) }
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            runCatching { mapView.onDestroy() }
        }
    }

    LaunchedEffect(mapView, records) {
        if (mapView == null) return@LaunchedEffect
        mapView.getMapAsync { map ->
            try {
                naverMap = map
                map.uiSettings.isLocationButtonEnabled = true
                map.setOnMapClickListener { _, _ -> onPinClick(null) }
                records.firstOrNull()?.let { first ->
                    map.moveCamera(CameraUpdate.scrollTo(LatLng(first.latitude, first.longitude)))
                }
                android.util.Log.d("NaverMap", "Map initialized successfully")
                mapInitErrorMessage = null
            } catch (e: Exception) {
                android.util.Log.e("NaverMap", "Failed to initialize map", e)
                mapInitErrorMessage = "지도 초기화 중 오류: ${e.message}"
            }
        }
    }

    LaunchedEffect(naverMap, records, selectedId) {
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
                        onPinClick(record.id)
                        true
                    }
                }
            renderedMarkers.add(marker)
        }

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
                if (mapInitErrorMessage != null) {
                    Text(mapInitErrorMessage!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                } else {
                    Text("네이버 지도 SDK를 초기화할 수 없습니다.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    } else {
        AndroidView(
            modifier = modifier,
            factory = { mapView!! },
        )
    }
}

/**
 * Displays a popup card showing detailed information for a given space record.
 *
 * Shows the space name, average focus score, metrics for noise, illuminance, and vibration,
 * and session/last-visited summary. Includes a control that invokes `onDismiss` to close the popup.
 *
 * @param record The `SpaceRecord` whose details are rendered.
 * @param onDismiss Callback invoked when the user dismisses the popup.
 */
@Composable
private fun SpaceDetailPopup(
    record: SpaceRecord,
    onDismiss: () -> Unit,
) {
    Card(
        modifier =
            Modifier
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
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Text("✕", fontSize = 14.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricText("소음", "%.0f dB".format(record.avgNoise), ColorNoise)
                MetricText("조도", "%.0f lux".format(record.avgIlluminance), ColorLight)
                MetricText("진동", "%.2f m/s²".format(record.avgVibration), ColorVibration)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "세션 ${record.sessionCount}회 · 마지막 방문: ${record.lastVisited}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Displays a centered metric with a prominent value and a smaller label beneath it.
 *
 * @param label The metric label shown below the value (e.g., "Noise").
 * @param value The metric value shown above the label (formatted string, e.g., "42 dB").
 * @param color The color applied to the value text.
 */
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

/**
 * Displays a clickable card summarizing a space record for the list view.
 *
 * Shows the space name, an average focus score badge, session count with last-visited text,
 * and three small metric tags for noise, illuminance, and vibration.
 *
 * @param record The `SpaceRecord` whose data is displayed.
 * @param onClick Callback invoked when the card is clicked.
 */
@Composable
private fun SpaceListCard(
    record: SpaceRecord,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 점수 원형 배지
            Box(
                modifier =
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(ColorFocus.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${record.avgFocusScore}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ColorFocus,
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "세션 ${record.sessionCount}회 · 마지막 방문: ${record.lastVisited}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallTag("소음 %.0fdB".format(record.avgNoise), ColorNoise)
                    SmallTag("%.0flux".format(record.avgIlluminance), ColorLight)
                    SmallTag("%.2fm/s²".format(record.avgVibration), ColorVibration)
                }
            }
        }
    }
}

@Composable
private fun SmallTag(
    text: String,
    color: Color,
) {
    Box(
        modifier =
            Modifier
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
private const val NAVER_MAP_CLIENT_ID_META_KEY = "com.naver.maps.map.CLIENT_ID"

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

private fun Context.hasNaverMapClientIdConfigured(): Boolean =
    runCatching {
        val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        val clientId = appInfo.metaData?.getString(NAVER_MAP_CLIENT_ID_META_KEY).orEmpty().trim()
        clientId.isNotEmpty() && !clientId.startsWith("\${")
    }.getOrDefault(false)

@Preview(showBackground = true)
@Composable
private fun SpaceHistoryPreview() {
    FocustationTheme {
        SpaceHistoryScreen(onBack = {})
    }
}
