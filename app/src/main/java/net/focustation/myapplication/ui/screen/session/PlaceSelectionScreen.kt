package net.focustation.myapplication.ui.screen.session

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.location.Geocoder
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.util.FusedLocationSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import net.focustation.myapplication.data.repository.NaverPlaceSearchRepository
import net.focustation.myapplication.data.repository.NaverPlaceSearchResult
import net.focustation.myapplication.session.SelectedSessionPlace
import net.focustation.myapplication.session.SessionPlaceSelectionStore
import net.focustation.myapplication.ui.components.ReferenceDesignTokens
import net.focustation.myapplication.ui.theme.FocusInk
import net.focustation.myapplication.ui.theme.FocustationTheme
import net.focustation.myapplication.util.DebugLog
import java.util.Locale
import android.graphics.Color as AndroidColor

data class PlaceSelectionUiState(
    val isLoadingLocation: Boolean = false,
    val isSearching: Boolean = false,
    val query: String = "카페",
    val manualPlaceName: String = "",
    val addressHint: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val results: List<NaverPlaceSearchResult> = emptyList(),
    val selectedPlace: NaverPlaceSearchResult? = null,
    val errorMessage: String? = null,
)

class PlaceSelectionViewModel(
    app: Application,
) : AndroidViewModel(app) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(app)
    private val placeSearchRepository = NaverPlaceSearchRepository()

    private val _uiState = MutableStateFlow(PlaceSelectionUiState())
    val uiState: StateFlow<PlaceSelectionUiState> = _uiState.asStateFlow()

    private var initialLoadRequested = false
    private var searchJob: Job? = null

    fun updateQuery(query: String) {
        _uiState.update {
            it.copy(
                query = query,
                manualPlaceName = "",
                selectedPlace = null,
            )
        }
        scheduleSearch()
    }

    fun selectPlace(place: NaverPlaceSearchResult) {
        _uiState.update { it.copy(selectedPlace = place, manualPlaceName = place.name) }
    }

    fun loadInitialPlaces() {
        if (initialLoadRequested) return
        initialLoadRequested = true
        refreshPlaces()
    }

    fun refreshPlaces() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLocation = true, isSearching = true, errorMessage = null) }
            val location =
                runCatching { currentLocation() }
                    .onFailure { initialLoadRequested = false }
                    .getOrNull()
            val addressHint =
                location?.let { resolveAddressHint(getApplication(), it.latitude, it.longitude) }.orEmpty()

            _uiState.update {
                it.copy(
                    isLoadingLocation = false,
                    latitude = location?.latitude,
                    longitude = location?.longitude,
                    addressHint = addressHint,
                )
            }

            searchCurrentQuery()
        }
    }

    fun searchCurrentQuery() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch { performSearch() }
    }

    private fun scheduleSearch() {
        searchJob?.cancel()
        searchJob =
            viewModelScope.launch {
                delay(SEARCH_DEBOUNCE_MS)
                performSearch()
            }
    }

    private suspend fun performSearch() {
        val state = _uiState.value
        val requestedQuery = state.query.trim()
        if (requestedQuery.isBlank()) {
            _uiState.update {
                it.copy(
                    isSearching = false,
                    results = emptyList(),
                    selectedPlace = null,
                    errorMessage = null,
                )
            }
            return
        }

        _uiState.update { it.copy(isSearching = true, errorMessage = null, selectedPlace = null) }
        val result =
            placeSearchRepository.searchPlaces(
                query = requestedQuery,
                addressHint = state.addressHint,
            )
        result.fold(
            onSuccess = { places ->
                _uiState.update {
                    if (it.query.trim() != requestedQuery) {
                        it
                    } else {
                        it.copy(
                            isSearching = false,
                            results = places,
                            errorMessage = null,
                        )
                    }
                }
            },
            onFailure = { error ->
                _uiState.update {
                    if (it.query.trim() != requestedQuery) {
                        it
                    } else {
                        it.copy(
                            isSearching = false,
                            results = emptyList(),
                            errorMessage = error.message ?: "장소 검색에 실패했어요.",
                        )
                    }
                }
            },
        )
    }

    fun saveSelectedOrManualPlace() {
        val state = _uiState.value
        val selected = state.selectedPlace
        val name =
            selected?.name
                ?: state.manualPlaceName.trim()

        if (name.isBlank()) {
            SessionPlaceSelectionStore.clear()
            return
        }

        SessionPlaceSelectionStore.save(
            SelectedSessionPlace(
                name = name,
                latitude = selected?.latitude ?: state.latitude,
                longitude = selected?.longitude ?: state.longitude,
                address = selected?.roadAddress?.ifBlank { selected.address }.orEmpty(),
                category = selected?.category.orEmpty(),
            ),
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(): android.location.Location? {
        val current =
            runCatching {
                fusedLocationClient
                    .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .await()
            }.getOrNull()
        return current ?: runCatching { fusedLocationClient.lastLocation.await() }.getOrNull()
    }

    private companion object {
        private const val SEARCH_DEBOUNCE_MS = 350L
    }
}

private suspend fun resolveAddressHint(
    context: Context,
    latitude: Double,
    longitude: Double,
): String =
    withContext(Dispatchers.IO) {
        runCatching {
            @Suppress("DEPRECATION")
            val address = Geocoder(context, Locale.KOREA).getFromLocation(latitude, longitude, 1)?.firstOrNull()
            listOfNotNull(address?.adminArea, address?.locality, address?.subLocality)
                .distinct()
                .joinToString(" ")
        }.getOrDefault("")
    }

@Composable
fun PlaceSelectionScreen(
    onPlaceSelected: () -> Unit,
    onBack: () -> Unit,
    viewModel: PlaceSelectionViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var hasLocationPermission by remember { mutableStateOf(context.hasLocationPermission()) }
    val isNaverMapMcpIdConfigured = remember(context) { context.hasNaverMapMcpIdConfigured() }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            hasLocationPermission = result.any { it.value }
            if (hasLocationPermission) {
                viewModel.loadInitialPlaces()
            }
        }

    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            viewModel.loadInitialPlaces()
        } else {
            permissionLauncher.launch(LOCATION_PERMISSIONS)
        }
    }

    PlaceSelectionContent(
        uiState = uiState,
        isNaverMapMcpIdConfigured = isNaverMapMcpIdConfigured,
        hasLocationPermission = hasLocationPermission,
        onBack = onBack,
        onQueryChange = viewModel::updateQuery,
        onSearch = viewModel::searchCurrentQuery,
        onKeyword = {
            viewModel.updateQuery(it)
            viewModel.searchCurrentQuery()
        },
        onPlaceClick = viewModel::selectPlace,
        onSkip = {
            SessionPlaceSelectionStore.clear()
            onPlaceSelected()
        },
        onConfirm = {
            viewModel.saveSelectedOrManualPlace()
            onPlaceSelected()
        },
    )
}

@Composable
private fun PlaceSelectionContent(
    uiState: PlaceSelectionUiState,
    isNaverMapMcpIdConfigured: Boolean,
    hasLocationPermission: Boolean,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onKeyword: (String) -> Unit,
    onPlaceClick: (NaverPlaceSearchResult) -> Unit,
    onSkip: () -> Unit,
    onConfirm: () -> Unit,
    autoFocusSearch: Boolean = true,
) {
    Scaffold(
        containerColor = ReferenceDesignTokens.Screen,
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(ReferenceDesignTokens.Screen)
                    .padding(paddingValues),
        ) {
            PlacePickerMap(
                isNaverMapMcpIdConfigured = isNaverMapMcpIdConfigured,
                hasLocationPermission = hasLocationPermission,
                currentLatitude = uiState.latitude,
                currentLongitude = uiState.longitude,
                results = uiState.results,
                selectedPlace = uiState.selectedPlace,
                onPlaceClick = onPlaceClick,
                modifier = Modifier.fillMaxSize(),
            )

            Column(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PlaceSelectionHeader(onBack = onBack)
                PlaceSearchOverlay(
                    uiState = uiState,
                    onQueryChange = onQueryChange,
                    onSearch = onSearch,
                    onKeyword = onKeyword,
                    autoFocus = autoFocusSearch,
                )
            }

            PlaceResultsSheet(
                uiState = uiState,
                onPlaceClick = onPlaceClick,
                onSkip = onSkip,
                onConfirm = onConfirm,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun PlaceSelectionHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(
            onClick = onBack,
            modifier =
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(ReferenceDesignTokens.WhiteCard),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = FocusInk)
        }
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = ReferenceDesignTokens.WhiteCard,
            shadowElevation = 0.dp,
        ) {
            Text(
                text = "장소 지정",
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                color = ReferenceDesignTokens.TextPrimary,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
            )
        }
        Spacer(Modifier.size(42.dp))
    }
}

@Composable
private fun PlaceSearchOverlay(
    uiState: PlaceSelectionUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onKeyword: (String) -> Unit,
    autoFocus: Boolean = true,
) {
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun requestSearchFocus() {
        runCatching { focusRequester.requestFocus() }
        keyboardController?.show()
        view.postDelayed(
            {
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
            },
            KEYBOARD_RETRY_DELAY_MS,
        )
    }

    LaunchedEffect(autoFocus) {
        if (!autoFocus) return@LaunchedEffect
        delay(KEYBOARD_SHOW_DELAY_MS)
        repeat(KEYBOARD_SHOW_ATTEMPTS) {
            requestSearchFocus()
            delay(KEYBOARD_RETRY_DELAY_MS)
        }
    }

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { requestSearchFocus() },
        shape = RoundedCornerShape(24.dp),
        color = ReferenceDesignTokens.WhiteCard,
        border = BorderStroke(1.dp, ReferenceDesignTokens.Border),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChange,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                keyboardController?.show()
                            }
                        },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                label = { Text("장소 검색") },
                placeholder = { Text("카페, 스터디카페, 도서관") },
                shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions =
                    KeyboardActions(
                        onSearch = {
                            onSearch()
                            keyboardController?.hide()
                        },
                    ),
            )
            Text(
                text =
                    if (uiState.addressHint.isBlank()) {
                        "현재 위치 기준으로 검색합니다"
                    } else {
                        "${uiState.addressHint} 주변 검색"
                    },
                color = ReferenceDesignTokens.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PLACE_KEYWORDS.forEach { keyword ->
                    FilterChip(
                        selected = uiState.query == keyword,
                        onClick = { onKeyword(keyword) },
                        label = { Text(keyword) },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ReferenceDesignTokens.Dark,
                                selectedLabelColor = Color.White,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceResultsSheet(
    uiState: PlaceSelectionUiState,
    onPlaceClick: (NaverPlaceSearchResult) -> Unit,
    onSkip: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = ReferenceDesignTokens.WhiteCard,
        border = BorderStroke(1.dp, ReferenceDesignTokens.Border),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "검색 결과",
                        color = ReferenceDesignTokens.TextPrimary,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    )
                    Text(
                        text = resultSummary(uiState),
                        color = ReferenceDesignTokens.TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                uiState.selectedPlace?.let {
                    Text(
                        text = "선택됨",
                        color = ReferenceDesignTokens.BlueDark,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }

            uiState.errorMessage?.let { message ->
                MessageCard(message = message)
            }

            if (uiState.isSearching || uiState.isLoadingLocation) {
                MessageCard(message = "검색어에 맞는 장소를 찾는 중이에요.")
            } else if (uiState.results.isEmpty()) {
                MessageCard(message = "검색 결과가 없어요. 다른 장소명을 입력해보세요.")
            } else {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.results, key = { "${it.name}-${it.roadAddress}-${it.address}" }) { place ->
                        PlaceResultCard(
                            place = place,
                            selected = uiState.selectedPlace == place,
                            onClick = { onPlaceClick(place) },
                        )
                    }
                }
            }

            PlaceActionButtons(
                canSelect = uiState.selectedPlace != null,
                onSkip = onSkip,
                onConfirm = onConfirm,
            )
        }
    }
}

@Composable
private fun PlacePickerMap(
    isNaverMapMcpIdConfigured: Boolean,
    hasLocationPermission: Boolean,
    currentLatitude: Double?,
    currentLongitude: Double?,
    results: List<NaverPlaceSearchResult>,
    selectedPlace: NaverPlaceSearchResult?,
    onPlaceClick: (NaverPlaceSearchResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isNaverMapMcpIdConfigured) {
        Box(
            modifier = modifier.background(ReferenceDesignTokens.PaleBlueTrack),
            contentAlignment = Alignment.Center,
        ) {
            MessageCard(message = "NAVER_MAP_MCP_ID가 설정되면 지도 위에서 장소를 확인할 수 있어요.")
        }
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = remember(context) { context.findActivity() }
    val onPlaceClickState by rememberUpdatedState(onPlaceClick)
    val latestResults by rememberUpdatedState(results)
    val latestSelectedPlace by rememberUpdatedState(selectedPlace)
    val mapView =
        remember {
            try {
                MapView(context).apply {
                    isFocusable = false
                    isFocusableInTouchMode = false
                }
            } catch (error: Exception) {
                DebugLog.e("Failed to create PlaceSelection MapView", error)
                null
            }
        }

    LaunchedEffect(mapView) {
        val createdMapView = mapView ?: return@LaunchedEffect
        runCatching { createdMapView.onCreate(null) }
            .onFailure { DebugLog.e("PlaceSelection MapView onCreate failed", it) }
    }

    val locationSource =
        remember(activity) { activity?.let { FusedLocationSource(it, LOCATION_PERMISSION_REQUEST_CODE) } }
    var naverMap by remember { mutableStateOf<NaverMap?>(null) }
    val renderedMarkers = remember { mutableStateListOf<Marker>() }

    DisposableEffect(lifecycleOwner, mapView) {
        if (mapView == null) return@DisposableEffect onDispose {}

        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START ->
                        runCatching { mapView.onStart() }.onFailure { DebugLog.e("PlaceSelection onStart error", it) }

                    Lifecycle.Event.ON_RESUME ->
                        runCatching { mapView.onResume() }.onFailure { DebugLog.e("PlaceSelection onResume error", it) }

                    Lifecycle.Event.ON_PAUSE ->
                        runCatching { mapView.onPause() }.onFailure { DebugLog.e("PlaceSelection onPause error", it) }

                    Lifecycle.Event.ON_STOP ->
                        runCatching { mapView.onStop() }.onFailure { DebugLog.e("PlaceSelection onStop error", it) }

                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            renderedMarkers.forEach { it.map = null }
            renderedMarkers.clear()
            runCatching { mapView.onDestroy() }.onFailure { DebugLog.e("PlaceSelection onDestroy error", it) }
        }
    }

    LaunchedEffect(mapView) {
        val createdMapView = mapView ?: return@LaunchedEffect
        createdMapView.getMapAsync { map ->
            naverMap = map
            runCatching {
                map.uiSettings.isLocationButtonEnabled = true
                map.locationTrackingMode =
                    if (hasLocationPermission) {
                        LocationTrackingMode.Follow
                    } else {
                        LocationTrackingMode.None
                    }
            }.onFailure { DebugLog.e("PlaceSelection map init failed", it) }
        }
    }

    LaunchedEffect(naverMap, hasLocationPermission, locationSource) {
        val map = naverMap ?: return@LaunchedEffect
        runCatching {
            if (locationSource != null && hasLocationPermission) {
                map.locationSource = locationSource
            }
            map.locationTrackingMode =
                if (hasLocationPermission) {
                    LocationTrackingMode.Follow
                } else {
                    LocationTrackingMode.None
                }
        }.onFailure { DebugLog.e("PlaceSelection location tracking failed", it) }
    }

    LaunchedEffect(naverMap, results, selectedPlace) {
        val map = naverMap ?: return@LaunchedEffect
        renderedMarkers.forEach { it.map = null }
        renderedMarkers.clear()

        latestResults.forEach { place ->
            val position = place.toLatLngOrNull() ?: return@forEach
            val isSelected = place == latestSelectedPlace
            val marker =
                Marker().apply {
                    this.position = position
                    captionText = place.name
                    iconTintColor =
                        if (isSelected) {
                            AndroidColor.rgb(45, 93, 246)
                        } else {
                            AndroidColor.rgb(40, 47, 58)
                        }
                    this.map = map
                    setOnClickListener {
                        onPlaceClickState(place)
                        true
                    }
                }
            renderedMarkers.add(marker)
        }
    }

    LaunchedEffect(naverMap, currentLatitude, currentLongitude, results) {
        val map = naverMap ?: return@LaunchedEffect
        val target =
            latestResults.firstNotNullOfOrNull { it.toLatLngOrNull() }
                ?: currentLatLngOrNull(currentLatitude, currentLongitude)
                ?: return@LaunchedEffect
        map.moveCamera(
            CameraUpdate
                .scrollAndZoomTo(target, DEFAULT_MAP_ZOOM)
                .animate(CameraAnimation.Easing),
        )
    }

    LaunchedEffect(naverMap, selectedPlace) {
        val map = naverMap ?: return@LaunchedEffect
        val target = latestSelectedPlace?.toLatLngOrNull() ?: return@LaunchedEffect
        map.moveCamera(
            CameraUpdate
                .scrollAndZoomTo(target, SELECTED_MAP_ZOOM)
                .animate(CameraAnimation.Easing),
        )
    }

    if (mapView == null) {
        Box(
            modifier = modifier.background(ReferenceDesignTokens.PaleBlueTrack),
            contentAlignment = Alignment.Center,
        ) {
            MessageCard(message = "네이버 지도를 초기화할 수 없어요.")
        }
    } else {
        AndroidView(
            modifier = modifier,
            factory = { mapView },
        )
    }
}

@Composable
private fun PlaceResultCard(
    place: NaverPlaceSearchResult,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) ReferenceDesignTokens.PaleBlueTrack else ReferenceDesignTokens.WhiteCard,
        border =
            BorderStroke(
                width = if (selected) 1.6.dp else 1.dp,
                color = if (selected) ReferenceDesignTokens.Blue else ReferenceDesignTokens.Border,
            ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(ReferenceDesignTokens.PaleBlueTrack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Place, contentDescription = null, tint = ReferenceDesignTokens.BlueDark)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = place.name,
                    color = ReferenceDesignTokens.TextPrimary,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = place.roadAddress.ifBlank { place.address }.ifBlank { place.category },
                    color = ReferenceDesignTokens.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MessageCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = ReferenceDesignTokens.PaleBlueTrack,
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            color = ReferenceDesignTokens.TextPrimary,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun PlaceActionButtons(
    canSelect: Boolean,
    onSkip: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.weight(1f).height(54.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text("건너뛰기")
        }
        Button(
            onClick = onConfirm,
            enabled = canSelect,
            modifier = Modifier.weight(1f).height(54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ReferenceDesignTokens.Dark),
        ) {
            Text("집중 시작", fontWeight = FontWeight.Bold)
        }
    }
}

private fun resultSummary(uiState: PlaceSelectionUiState): String =
    when {
        uiState.isLoadingLocation -> "현재 위치 확인 중"
        uiState.isSearching -> "\"${uiState.query}\" 검색 중"
        uiState.results.isEmpty() -> "\"${uiState.query}\" 결과 없음"
        else -> "\"${uiState.query}\" ${uiState.results.size}개"
    }

private fun currentLatLngOrNull(
    latitude: Double?,
    longitude: Double?,
): LatLng? {
    if (latitude == null || longitude == null) return null
    return LatLng(latitude, longitude).takeIf { it.isValid() }
}

private fun NaverPlaceSearchResult.toLatLngOrNull(): LatLng? {
    val latitude = latitude ?: return null
    val longitude = longitude ?: return null
    return LatLng(latitude, longitude).takeIf { it.isValid() }
}

private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
private const val NAVER_MAP_MCP_ID_META_KEY = "com.naver.maps.map.MCP_ID"
private const val KEYBOARD_SHOW_DELAY_MS = 250L
private const val KEYBOARD_RETRY_DELAY_MS = 180L
private const val KEYBOARD_SHOW_ATTEMPTS = 3
private const val DEFAULT_MAP_ZOOM = 15.0
private const val SELECTED_MAP_ZOOM = 16.5

private val PLACE_KEYWORDS = listOf("카페", "스터디카페", "도서관")

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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PlaceSelectionPreview() {
    FocustationTheme {
        val samplePlaces =
            listOf(
                NaverPlaceSearchResult(
                    name = "연남 스터디카페",
                    category = "스터디카페",
                    address = "서울 마포구 연남동",
                    roadAddress = "서울 마포구 동교로 240",
                    latitude = 37.5622,
                    longitude = 126.9254,
                ),
                NaverPlaceSearchResult(
                    name = "중앙 도서관",
                    category = "도서관",
                    address = "서울 서대문구 신촌동",
                    roadAddress = "서울 서대문구 연세로 50",
                    latitude = 37.5658,
                    longitude = 126.9386,
                ),
            )
        PlaceSelectionContent(
            uiState =
                PlaceSelectionUiState(
                    query = "스터디카페",
                    addressHint = "서울 마포구",
                    latitude = 37.5622,
                    longitude = 126.9254,
                    results = samplePlaces,
                    selectedPlace = samplePlaces.first(),
                ),
            isNaverMapMcpIdConfigured = false,
            hasLocationPermission = true,
            onBack = {},
            onQueryChange = {},
            onSearch = {},
            onKeyword = {},
            onPlaceClick = {},
            onSkip = {},
            onConfirm = {},
            autoFocusSearch = false,
        )
    }
}
