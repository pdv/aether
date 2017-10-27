package io.rstlne.aether

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.os.Handler
import android.os.Looper
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.rxkotlin.addTo

/**
 * Created by pdv on 10/27/17.
 */

sealed class MidiMessage {
    abstract val key: Int
    data class NoteOn(override val key: Int, val velocity: Int) : MidiMessage()
    data class NoteOff(override val key: Int) : MidiMessage()
}

fun MidiMessage.toByteArray(channel: Int): ByteArray {
    val buffer = ByteArray(32)
    buffer[0] = (when (this) {
        is MidiMessage.NoteOn -> 0x90
        is MidiMessage.NoteOff -> 0x80
    } + channel).toByte()
    buffer[1] = when (this) {
        is MidiMessage.NoteOn -> key
        is MidiMessage.NoteOff -> key
    }.toByte()
    buffer[2] = when (this) {
        is MidiMessage.NoteOn -> velocity
        is MidiMessage.NoteOff -> 0
    }.toByte()
    return buffer
}

interface RxMidi {
    fun start()
    fun start(bluetoothDevice: BluetoothDevice)
    val output: Consumer<MidiMessage>
}

class RxMidiImpl(context: Context) : RxMidi {

    override val output: PublishRelay<MidiMessage> = PublishRelay.create()

    private val midiManager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager

    override fun start() {
        // TODO: More than one device
        disposeBag.clear()
        val info = midiManager.devices[0]
        midiManager.openDevice(info, {
            routeOutput(it.openInputPort(0))
        }, Handler(Looper.getMainLooper()))
    }

    override fun start(bluetoothDevice: BluetoothDevice) {
        disposeBag.clear()
        midiManager.openBluetoothDevice(bluetoothDevice, {
            routeOutput(it.openInputPort(0))
        }, Handler(Looper.getMainLooper()))
    }

    private val disposeBag = CompositeDisposable()

    private fun routeOutput(inputPort: MidiInputPort) {
        output
            .map { it.toByteArray(3) }
            .debug("MIDI")
            .subscribe { inputPort.send(it, 0, 3) }
            .addTo(disposeBag)
    }

}
