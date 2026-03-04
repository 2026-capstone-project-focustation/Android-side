package net.focustation.myapplication.data.model

data class User(
    val id: String = "user_001",
    val name: String = "김예찬",
    val email: String = "user@focustation.net",
)

data class EnvironmentSnapshot(
    val noiseLevel: Float = 35f, // dB
    val illuminance: Float = 420f, // lux
    val temperature: Float = 22.5f, // °C
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
    val avgTemperature: Float,
    val lastVisited: String,
)

data class FocusDataPoint(
    val timeLabel: String,
    val focusScore: Float,
)
