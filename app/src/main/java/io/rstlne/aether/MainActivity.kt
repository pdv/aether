package io.rstlne.aether

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import com.jakewharton.rxbinding2.view.touches
import org.jetbrains.anko.button
import org.jetbrains.anko.gridLayout
import org.jetbrains.anko.linearLayout

class MainActivity : AppCompatActivity() {

    private lateinit var rxMidi: RxMidi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rxMidi = RxMidiImpl(this)
        rxMidi.start()

        gridLayout {
            columnCount = 4
            (60..80).forEach { note ->
                button {
                    text = note.toString()
                    touches()
                        .unwrapMap { when (it.action) {
                            MotionEvent.ACTION_DOWN -> MidiMessage.NoteOn(note, 127)
                            MotionEvent.ACTION_UP -> MidiMessage.NoteOff(note)
                            else -> null
                        }}
                        .subscribe(rxMidi.output)
                }
            }
        }
    }

}
