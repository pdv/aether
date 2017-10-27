package io.rstlne.aether

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewManager
import android.widget.GridLayout
import android.widget.TextView
import com.jakewharton.rxbinding2.view.touches
import io.reactivex.functions.Consumer
import org.jetbrains.anko.backgroundResource
import org.jetbrains.anko.button
import org.jetbrains.anko.custom.ankoView
import org.jetbrains.anko.dip
import org.jetbrains.anko.gridLayout
import org.jetbrains.anko.horizontalScrollView
import org.jetbrains.anko.margin
import org.jetbrains.anko.scrollView
import org.jetbrains.anko.verticalLayout

class MainActivity : AppCompatActivity() {

    companion object {
        val COLS = 6
    }

    private lateinit var midi: RxMidi

    private lateinit var pads: List<Pad>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        midi = RxMidiImpl(this)
        midi.start()

        verticalLayout {
            setBackgroundResource(R.color.gray)
            gridLayout {
                columnCount = COLS

                val pads = mutableMapOf<Int, Pad>()
                (7 downTo 0).forEach { row ->
                    (0 until COLS).forEach { col ->
                        val idx = row * COLS + col
                        val btn = pad(midi.output).lparams {
                            margin = dip(2)
                            width = dip(64)
                            height = dip(64)
                        }
                        pads.put(idx, btn)
                    }
                }
                this@MainActivity.pads = pads.toSortedMap().map { it.value }
            }.lparams {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }
        setScale(12, MAJOR)
    }

    fun setScale(root: Int, intervals: List<Int>) {
        val octaveSize = intervals.sum()
        val scaleLength = intervals.count()
        pads.forEachIndexed { index, pad ->
            val effectiveIndex = if (index >= COLS) index - (index / COLS) * (COLS / 2) else index
            val interval = effectiveIndex % scaleLength
            if (interval == 0) {
                pad.setBackgroundResource(R.color.dark_blue)
            }
            val octave = (effectiveIndex / scaleLength)
            pad.midiNote = root + (octave * octaveSize) + (intervals.subList(0, interval).sum())
        }
    }

}

fun ViewManager.pad(output: Consumer<MidiMessage>, init: Pad.() -> Unit = {}) = ankoView({ Pad(it, output) }, 0, init)

class Pad(ctx: Context, output: Consumer<MidiMessage>) : View(ctx) {

    var midiNote: Int = 60

    init {
        backgroundResource = R.color.blue
        touches()
            .unwrapMap {
                when (it.action) {
                    MotionEvent.ACTION_DOWN -> MidiMessage.NoteOn(midiNote, 127)
                    MotionEvent.ACTION_UP -> MidiMessage.NoteOff(midiNote)
                    else -> null
                }
            }
            .subscribe(output)
    }

}
