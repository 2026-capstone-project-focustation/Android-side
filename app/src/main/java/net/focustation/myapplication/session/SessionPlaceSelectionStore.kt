package net.focustation.myapplication.session

import net.focustation.myapplication.util.DebugLog

data class SelectedSessionPlace(
    val name: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String = "",
    val category: String = "",
)

object SessionPlaceSelectionStore {
    private var latestPlace: SelectedSessionPlace? = null

    @Synchronized
    fun save(place: SelectedSessionPlace) {
        latestPlace = place
        DebugLog.d("[SessionPlace][저장] name=${place.name}, lat=${place.latitude}, lon=${place.longitude}")
    }

    @Synchronized
    fun consume(): SelectedSessionPlace? =
        latestPlace.also {
            latestPlace = null
        }

    @Synchronized
    fun clear() {
        latestPlace = null
    }
}
