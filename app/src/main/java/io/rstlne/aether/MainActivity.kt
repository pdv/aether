package io.rstlne.aether

import android.content.res.ColorStateList
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import com.jakewharton.rxbinding2.view.touches
import org.jetbrains.anko.dip
import org.jetbrains.anko.gridLayout
import org.jetbrains.anko.linearLayout
import org.jetbrains.anko.margin
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.seekBar
import org.jetbrains.anko.spinner
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.verticalMargin
import org.jetbrains.anko.view


fun colorStateList(pressed: Int, enabled: Int, disabled: Int) = ColorStateList(
    arrayOf(
        intArrayOf(android.R.attr.state_pressed),
        intArrayOf(android.R.attr.state_enabled),
        intArrayOf()
    ),
    intArrayOf(pressed, enabled, disabled)
)

class MainActivity : AppCompatActivity() {

    companion object {
        val ROWS = 8
        val COLS = 6
        val MAJOR = listOf(2, 2, 1, 2, 2, 2, 1)
        val MINOR = listOf(2, 1, 2, 2, 1, 2, 2)
        val BLUES = listOf(3, 2, 1, 1, 3, 2)
    }

    private lateinit var midi: RxMidi

    private lateinit var pads: List<View>
    private val notes: MutableList<Int> = (0..ROWS * COLS).map { 0 }.toMutableList()

    private var root = 12
        set(value) {
            field = value
            setScale(root, scale)
        }
    private var scale = MAJOR
        set(value) {
            field = value
            setScale(root, scale)
        }

    private var velocity: Int = 127

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        midi = RxMidiImpl(this)
        midi.start()

        verticalLayout {
            setBackgroundResource(R.color.gray)

            linearLayout {
                gravity = Gravity.CENTER

                spinner {
                    adapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, listOf("C", "C# / Db", "D", "D# / Eb", "E", "F", "F# / Gb", "G", "G# / Ab", "A", "A# / Bb", "B"))
                    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, idx: Int, p3: Long) {
                            root = idx + 12
                        }
                        override fun onNothingSelected(p0: AdapterView<*>?) = Unit
                    }
                }

                spinner {
                    val options = listOf(
                        "Major" to MAJOR,
                        "Minor" to MINOR,
                        "Blues" to BLUES
                    )
                    adapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, options.map { it.first })
                    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, idx: Int, p3: Long) {
                            scale = options[idx].second
                        }
                        override fun onNothingSelected(p0: AdapterView<*>?) = Unit
                    }

                }
            }.lparams(width = matchParent, height = 0) {
                weight = 1f
            }

            gridLayout {
                columnCount = COLS

                val pads = mutableMapOf<Int, View>()
                (ROWS - 1 downTo 0).forEach { row ->
                    (0 until COLS).forEach { col ->
                        val idx = row * COLS + col
                        val btn = view {

                            touches()
                                .unwrapMap {
                                    when (it.action) {
                                        MotionEvent.ACTION_DOWN -> MidiMessage.NoteOn(notes[idx], velocity)
                                        MotionEvent.ACTION_UP -> MidiMessage.NoteOff(notes[idx])
                                        else -> null
                                    }
                                }
                                .doOnNext { msg ->
                                    val matchingPads = notes.mapIndexed(::Pair).filter { it.second == msg.key }.map { it.first }
                                    val highlight = ContextCompat.getColor(context, R.color.green)
                                    when (msg) {
                                        is MidiMessage.NoteOn ->
                                            matchingPads.forEach { pads[it]?.backgroundTintList = colorStateList(highlight, highlight, highlight) }
                                        is MidiMessage.NoteOff ->
                                            matchingPads.forEach { pads[it]?.backgroundTintList = null }
                                }}
                                .subscribe(midi.output)

                        }.lparams {
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
                verticalMargin = dip(48)
            }

            seekBar {
                max = 127
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(p0: SeekBar?, value: Int, p2: Boolean) {
                        velocity = value
                    }
                    override fun onStartTrackingTouch(p0: SeekBar?) = Unit
                    override fun onStopTrackingTouch(p0: SeekBar?) = Unit
                })
            }.lparams(width = matchParent, height = 0) {
                weight = 1f
            }
        }
        setScale(12, MAJOR)
    }

    private fun setScale(root: Int, intervals: List<Int>) {
        val octaveSize = intervals.sum()
        val scaleLength = intervals.count()
        pads.forEachIndexed { index, pad ->
            val effectiveIndex = index - (index / COLS) * (COLS / 2)
            val interval = effectiveIndex % scaleLength
            pad.setBackgroundResource(if (interval == 0) R.color.dark_blue else R.color.blue)
            val octave = (effectiveIndex / scaleLength)
            notes[index] = root + (octave * octaveSize) + (intervals.subList(0, interval).sum())
        }
    }

}
