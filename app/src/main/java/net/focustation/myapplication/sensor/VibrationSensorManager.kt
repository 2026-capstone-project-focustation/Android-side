package net.focustation.myapplication.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.sqrt

class VibrationSensorManager(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    /**
     * Provides a Flow that emits vibration magnitudes computed from the device's linear acceleration sensor.
     *
     * Each emission is the vector magnitude sqrt(x*x + y*y + z*z) in meters per second squared for a sensor event; emissions occur while the flow is collected.
     *
     * @return The measured linear acceleration magnitude as a `Double` for each sensor event.
     */
    fun getVibrationFlow(): Flow<Double> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.values?.let { v ->
                    // 3축 선형가속도 벡터 크기: sqrt(x² + y² + z²) m/s²
                    val magnitude = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]).toDouble()
                    trySend(magnitude)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
