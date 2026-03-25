package net.focustation.myapplication.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LightSensorManager(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    /**
     * Provides a Flow of ambient light level measurements in lux.
     *
     * Collecting the flow registers an Android light sensor listener; the listener is automatically
     * unregistered when collection is cancelled or completes.
     *
     * @return A Flow that emits ambient light level readings (lux) as `Float`.
     */
    fun getLightFlow(): Flow<Float> = callbackFlow {
        if (lightSensor == null) {
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            /**
             * Emits the sensor's first value (ambient light level in lux) into the associated flow when a new reading arrives.
             *
             * @param event The sensor event containing readings; if `null` no value is emitted.
             */
            override fun onSensorChanged(event: SensorEvent?) {
                event?.values?.get(0)?.let { trySend(it) }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val registered = sensorManager.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_UI)
        if (!registered) {
            close()
            return@callbackFlow
        }
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
