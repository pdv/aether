package io.rstlne.aether

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
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

    val notes = setOf(5, 6, 7)

    // [-10, 10] -> [0, 127]
    private val Float.normalized: Int get() = ((this + 10) * (127f / 20)).toInt()

    override val type: Int = Sensor.TYPE_ACCELEROMETER

    override fun eventToMidi(event: SensorEvent): Set<MidiMessage> {
        return notes.mapIndexed { index, note -> MidiMessage.Control(note, event.values[index].normalized) }.toSet()
    }

}

object LightMidiSensor: MidiSensor {

    val note = 8

    override val type: Int = Sensor.TYPE_LIGHT

    override fun eventToMidi(event: SensorEvent): Set<MidiMessage> {
        return setOf(MidiMessage.Control(note, event.values[0].toInt() * 2))
    }

}

object GyroMidiSensor : MidiSensor {

    val notes = setOf(9, 10, 11)

    override val type: Int = Sensor.TYPE_GYROSCOPE

    override fun eventToMidi(event: SensorEvent): Set<MidiMessage> {
        return notes.mapIndexed { index, note -> MidiMessage.Control(note, event.values[index].toInt()) }.toSet()
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
        AccelerometerMidiSensor,
        LightMidiSensor,
        GyroMidiSensor
    ).map { Snsr(it, RxSensor(), sensorManager.getDefaultSensor(it.type))}

    var filteredNotes = mutableSetOf<Int>()

    fun start() {
        sensors.forEach { (midiSensor, listener, sensor) ->
            listener.events
                .map(midiSensor::eventToMidi)
                .flatMap { Observable.fromIterable(it) }
                .filter { !filteredNotes.contains(it.key) }
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