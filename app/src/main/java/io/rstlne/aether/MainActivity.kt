package io.rstlne.aether

import android.content.Context
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.jetbrains.anko.button
import org.jetbrains.anko.linearLayout

class MainActivity : AppCompatActivity() {

    lateinit var inputPort: MidiInputPort

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val m = getSystemService(Context.MIDI_SERVICE) as MidiManager
        val info = m.devices[0]

        m.openDevice(info, {
            if (it == null) {
                Log.e("MIDI", "Failed to open")
            } else {
                inputPort = it.openInputPort(0)
            }

        }, Handler(Looper.getMainLooper()))

        linearLayout {
            button {
                setOnClickListener {
                    val buffer = ByteArray(32)
                    var numBytes = 0
                    val channel = 3 // MIDI channels 1-16 are encoded as 0-15.
                    buffer[numBytes++] = (0x90 + (channel - 1)).toByte() // note on
                    buffer[numBytes++] = 60.toByte() // pitch is middle C
                    buffer[numBytes++] = 127.toByte() // max velocity
                    val offset = 0
                    // post is non-blocking
                    inputPort.send(buffer, offset, numBytes)
                }
            }
        }
    }

}
