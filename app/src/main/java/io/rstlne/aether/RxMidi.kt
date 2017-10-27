package io.rstlne.aether

import android.content.Context
import android.media.midi.MidiManager
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.functions.Consumer

/**
 * Created by pdv on 10/27/17.
 */

sealed class MidiMessage {
    data class NoteOn(val key: Int, val velocity: Int) : MidiMessage()
    data class NoteOff(val key: Int) : MidiMessage()
}

fun MidiMessage.toByteArray(channel: Int): ByteArray {
    val buffer = ByteArray(32)
    buffer[0] = (when (this) {
        is MidiMessage.NoteOn -> 0x90
        is MidiMessage.NoteOff -> 0x80
    } + (channel -1)).toByte()
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
    val output: Consumer<MidiMessage>
}

class RxMidiImpl(context: Context) : RxMidi {

    override val output: Consumer<MidiMessage> = PublishRelay.create()

    private val midiManager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager

    fun start() {

    }


}
