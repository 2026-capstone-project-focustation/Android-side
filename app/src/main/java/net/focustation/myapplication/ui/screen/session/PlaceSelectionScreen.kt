package net.focustation.myapplication.ui.screen.session

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.EditLocationAlt
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
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
import java.util.Locale

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

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun updateManualPlaceName(name: String) {
        _uiState.update { it.copy(manualPlaceName = name, selectedPlace = null) }
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
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isSearching = true, errorMessage = null, selectedPlace = null) }
            val result =
                placeSearchRepository.searchPlaces(
                    query = state.query,
                    addressHint = state.addressHint,
                )
            result.fold(
                onSuccess = { places ->
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            results = places,
                            errorMessage = null,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            results = emptyList(),
                            errorMessage = error.message ?: "장소 검색에 실패했어요.",
                        )
                    }
                },
            )
        }
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
                latitude = state.latitude,
                longitude = state.longitude,
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
    val hasLocationPermission = context.hasLocationPermission()
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.any { it.value }) {
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

    Scaffold(
        containerColor = ReferenceDesignTokens.Screen,
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(ReferenceDesignTokens.Screen)
                    .padding(paddingValues)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                PlaceSelectionHeader(onBack = onBack)
            }

            item {
                PlaceSearchCard(
                    uiState = uiState,
                    onQueryChange = viewModel::updateQuery,
                    onSearch = viewModel::searchCurrentQuery,
                    onKeyword = {
                        viewModel.updateQuery(it)
                        viewModel.searchCurrentQuery()
                    },
                    onManualNameChange = viewModel::updateManualPlaceName,
                )
            }

            uiState.errorMessage?.let { message ->
                item {
                    MessageCard(message = message)
                }
            }

            if (uiState.isSearching || uiState.isLoadingLocation) {
                item {
                    MessageCard(message = "현재 위치 기준으로 장소를 찾는 중이에요.")
                }
            } else {
                items(uiState.results, key = { "${it.name}-${it.roadAddress}-${it.address}" }) { place ->
                    PlaceResultCard(
                        place = place,
                        selected = uiState.selectedPlace == place,
                        onClick = { viewModel.selectPlace(place) },
                    )
                }
            }

            item {
                PlaceActionButtons(
                    canSelect = uiState.selectedPlace != null || uiState.manualPlaceName.isNotBlank(),
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "장소 지정",
                color = ReferenceDesignTokens.TextPrimary,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            )
            Text(
                text = "이번 세션을 저장할 장소를 골라주세요",
                color = ReferenceDesignTokens.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Spacer(Modifier.size(42.dp))
    }
}

@Composable
private fun PlaceSearchCard(
    uiState: PlaceSelectionUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onKeyword: (String) -> Unit,
    onManualNameChange: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ReferenceDesignTokens.WhiteCard),
        border = BorderStroke(1.dp, ReferenceDesignTokens.Border),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (uiState.addressHint.isBlank()) "현재 위치 기준" else uiState.addressHint,
                color = ReferenceDesignTokens.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                label = { Text("검색어") },
                placeholder = { Text("카페, 스터디카페, 도서관") },
                shape = RoundedCornerShape(16.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("카페", "스터디카페", "도서관").forEach { keyword ->
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
            Button(
                onClick = onSearch,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ReferenceDesignTokens.Dark),
            ) {
                Text("장소 검색", fontWeight = FontWeight.Bold)
            }
            OutlinedTextField(
                value = uiState.manualPlaceName,
                onValueChange = onManualNameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.EditLocationAlt, contentDescription = null) },
                label = { Text("직접 입력") },
                placeholder = { Text("예: 중앙도서관 3층") },
                shape = RoundedCornerShape(16.dp),
            )
        }
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

private val LOCATION_PERMISSIONS =
    arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

private fun Context.hasLocationPermission(): Boolean =
    LOCATION_PERMISSIONS.any {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PlaceSelectionPreview() {
    FocustationTheme {
        PlaceSelectionScreen(onPlaceSelected = {}, onBack = {})
    }
}
