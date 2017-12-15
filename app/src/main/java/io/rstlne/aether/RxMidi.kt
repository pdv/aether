package io.rstlne.aether

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.rxkotlin.addTo
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by pdv on 10/27/17.
 */

sealed class MidiMessage {
    abstract val key: Int
    data class NoteOn(override val key: Int, val velocity: Int) : MidiMessage()
    data class NoteOff(override val key: Int) : MidiMessage()
    data class Control(override val key: Int, val value: Int) : MidiMessage()
}

fun MidiMessage.toByteArray(channel: Int): ByteArray = when (this) {
    is MidiMessage.NoteOn -> listOf(0x90 + channel, key, velocity)
    is MidiMessage.NoteOff -> listOf(0x80 + channel, key, 0)
    is MidiMessage.Control -> listOf(0xb0 + channel, key, value)
}.map(Int::toByte).toByteArray()

interface RxMidi {
    var channel: Int
    fun start()
    fun start(bluetoothDevice: BluetoothDevice)
    val output: Consumer<MidiMessage>
}

class RxMidiImpl(context: Context) : RxMidi {

    override var channel: Int = 3

    override val output: PublishRelay<MidiMessage> = PublishRelay.create()

    private val midiManager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager

    override fun start() {
        // TODO: More than one device
        disposeBag.clear()
        val info = midiManager.devices.getOrNull(0)
        if (info == null) {
            Log.e("RxMidi", "Failed to open midi device")
            return
        }
        midiManager.openDevice(info, {
            val inputPort = it.openInputPort(0)
            if (inputPort != null) {
                routeOutput(inputPort)
            } else {
                Log.e("RxMidi", "Failed to open input port")
            }
        }, Handler(Looper.getMainLooper()))
    }

    private var running = AtomicBoolean(false)

    override fun start(bluetoothDevice: BluetoothDevice) {
        if (running.getAndSet(true)) {
            return
        }
        disposeBag.clear()
        midiManager.openBluetoothDevice(bluetoothDevice, {
            try {
                routeOutput(it.openInputPort(0))
            } catch (e: Exception) {
                Log.e("BLUETOOTH MIDI", "Failed to open port")
            }
        }, Handler(Looper.getMainLooper()))
    }

    private val disposeBag = CompositeDisposable()

    private fun routeOutput(inputPort: MidiInputPort) {
        output
            .map { it.toByteArray(channel) }
            .subscribe { inputPort.send(it, 0, 3) }
            .addTo(disposeBag)
    }

}
