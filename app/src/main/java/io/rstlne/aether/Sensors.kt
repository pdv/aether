package io.rstlne.aether

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.functions.Consumer

/**
 * Created by pdv on 12/15/17.
 */

interface MidiSensor {
    val type: Int
    fun eventToMidi(event: SensorEvent): Set<MidiMessage>
}

object AccelerometerMidiSensor: MidiSensor {

    // [-10, 10] -> [0, 127]
    private val Float.normalized: Int get() = ((this + 10) * (127f / 20)).toInt()

    override val type: Int = Sensor.TYPE_ACCELEROMETER

    override fun eventToMidi(event: SensorEvent): Set<MidiMessage> {
        return (0..2).map { MidiMessage.Control(it, event.values[it].normalized) }.toSet()
    }

}

data class Snsr(
    val midiSensor: MidiSensor,
    val listener: RxSensor,
    val sensor: Sensor
)

class Sensors(
    private val sensorManager: SensorManager,
    private val midiOut: Consumer<MidiMessage>
) {
    private val sensors: List<Snsr> = listOf<MidiSensor>(
        AccelerometerMidiSensor
    ).map { Snsr(it, RxSensor(), sensorManager.getDefaultSensor(it.type))}

    fun start() {
        sensors.forEach { (midiSensor, listener, sensor) ->
            listener.events
                .map(midiSensor::eventToMidi)
                .flatMap { Observable.fromIterable(it) }
                .subscribe(midiOut)
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensors.forEach { sensorManager.unregisterListener(it.listener) }
    }

}

class RxSensor : SensorEventListener {

    private val _events = PublishRelay.create<SensorEvent>()
    val events: Observable<SensorEvent> = _events

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // Don't really care about this
    }

    override fun onSensorChanged(event: SensorEvent) {
        _events.accept(event)
    }

}