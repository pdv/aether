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

typealias MidiSensor = (SensorEvent) -> Set<MidiMessage>

// [-10, 10] -> [0, 127]
private val Float.normalized: Int get() = ((this + 10) * (127f / 20)).toInt()

class Sensors(
    private val sensorManager: SensorManager,
    private val midiOut: Consumer<MidiMessage>
) {

    private val accelerometerListener = RxSensor()
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val accelerometerMapper: MidiSensor = { event ->
        (0..2).map { MidiMessage.Control(it, event.values[it].normalized) }.toSet()
    }

    fun start() {
        accelerometerListener.events
            .map(accelerometerMapper)
            .flatMap { Observable.fromIterable(it) }
            .subscribe(midiOut)
        sensorManager.registerListener(accelerometerListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stop() {
        sensorManager.unregisterListener(accelerometerListener)
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