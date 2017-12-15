package io.rstlne.aether

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.ParcelUuid
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import com.jakewharton.rxbinding2.view.touches
import io.reactivex.Observable
import org.jetbrains.anko.button
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

fun <T, R> Observable<T>.unwrapMap(mapper: (T) -> R?): Observable<R> = this
    .flatMap {
        val ret = mapper(it)
        if (ret == null) Observable.empty() else Observable.just(ret)
    }

fun <T> Observable<T>.debug(tag: String = "Obs") = this.doOnEach { Log.d(tag, it.toString()) }

class MainActivity : AppCompatActivity() {

    companion object {
        val ROWS = 8
        val COLS = 6
        val MAJOR = listOf(2, 2, 1, 2, 2, 2, 1)
        val MINOR = listOf(2, 1, 2, 2, 1, 2, 2)
        val BLUES = listOf(3, 2, 1, 1, 3, 2)
        val CHROMATIC = (0..11).map { 1 }
        val MIDI_UUID = "03B80E5A-EDE8-4B33-A751-6CE34EC4C700"
        val BOTTOM_NOTE = 24
    }

    private lateinit var midi: RxMidi

    private lateinit var pads: List<View>
    private val notes: MutableList<Int> = (0..ROWS * COLS).map { 0 }.toMutableList()

    private var root = BOTTOM_NOTE
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

    private fun setupBluetooth() {
        Log.d("BLUETOOTH", "Starting")
        val scanFilter = ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(MIDI_UUID)).build()
        val settings = ScanSettings.Builder().build()
        BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner.startScan(listOf(scanFilter), settings, object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                Log.d("BLUETOOTH", "Got result: $callbackType")
                midi.start(result.device)
                super.onScanResult(callbackType, result)
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        setupBluetooth()
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        midi = RxMidiImpl(this)
        midi.start()
        //requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)

        verticalLayout {
            setBackgroundResource(R.color.gray)

            linearLayout {
                gravity = Gravity.CENTER

                spinner {
                    adapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, listOf("C", "C# / Db", "D", "D# / Eb", "E", "F", "F# / Gb", "G", "G# / Ab", "A", "A# / Bb", "B"))
                    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, idx: Int, p3: Long) {
                            root = idx + BOTTOM_NOTE
                        }
                        override fun onNothingSelected(p0: AdapterView<*>?) = Unit
                    }
                }

                spinner {
                    val options = listOf(
                        "Major" to MAJOR,
                        "Minor" to MINOR,
                        "Blues" to BLUES,
                        "Chromatic" to CHROMATIC
                    )
                    adapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, options.map { it.first })
                    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, idx: Int, p3: Long) {
                            scale = options[idx].second
                        }
                        override fun onNothingSelected(p0: AdapterView<*>?) = Unit
                    }
                }

                button {
                    text = "Reconnect"
                    setOnClickListener {
                        midi.start()
                        //setupBluetooth()
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
            val octave = effectiveIndex / scaleLength
            pad.setBackgroundResource(if (interval == 0) R.color.dark_blue else R.color.blue)
            notes[index] = root + (octave * octaveSize) + (intervals.subList(0, interval).sum())
        }
    }

}
