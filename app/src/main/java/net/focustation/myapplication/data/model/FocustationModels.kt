package net.focustation.myapplication.data.model

data class User(
    val id: String = "user_001",
    val name: String = "김예찬",
    val email: String = "user@focustation.net",
)

data class EnvironmentSnapshot(
    val noiseLevel: Float = 0f, // dB
    val illuminance: Float = 0f, // lux
    val vibration: Double = 0.0, // m/s²
)

data class SessionSummary(
    val id: String,
    val date: String,
    val place: String,
    val focusScore: Int,
    val totalMinutes: Int,
    val avgEnvironmentScore: Float,
)

data class SpaceRecord(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val avgFocusScore: Int,
    val sessionCount: Int,
    val avgNoise: Float,
    val avgIlluminance: Float,
    val avgVibration: Double,
    val lastVisited: String,
    val environmentTags: List<EnvironmentTag> = emptyList(),
)

data class FocusDataPoint(
    val timeLabel: String,
    val focusScore: Float,
)

data class SensorTimelinePoint(
    val timeLabel: String,
    val value: Float,
)

data class SensorTimelines(
    val noise: List<SensorTimelinePoint> = emptyList(),
    val light: List<SensorTimelinePoint> = emptyList(),
    val vibration: List<SensorTimelinePoint> = emptyList(),
)

data class EnvironmentTag(
    val key: String,
    val label: String,
    val category: String,
    val tone: String = "neutral",
)
