package io.rstlne.aether

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.hardware.SensorManager
import android.os.Bundle
import android.os.ParcelUuid
import android.support.v7.app.AppCompatActivity
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import org.jetbrains.anko.AnkoComponent
import org.jetbrains.anko.AnkoContext
import org.jetbrains.anko.checkBox
import org.jetbrains.anko.dip
import org.jetbrains.anko.linearLayout
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.setContentView
import org.jetbrains.anko.textView
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.verticalMargin


class MainActivity : AppCompatActivity() {

    companion object {
        val MIDI_UUID = "03B80E5A-EDE8-4B33-A751-6CE34EC4C700"
    }

    private lateinit var midi: RxMidi
    private lateinit var sensors: Sensors

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        midi = RxMidiImpl(this)
        sensors = Sensors(getSystemService(Context.SENSOR_SERVICE) as SensorManager, midi.output)
        MainActivityUI(midi, sensors).setContentView(this)
    }

    override fun onResume() {
        midi.start()
        sensors.start()
        super.onResume()
    }

    override fun onPause() {
        sensors.stop()
        super.onPause()
    }

    /** Bluetooth **/

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

    // BT is disabled for now. To re-enable:
    // requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        setupBluetooth()
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    /** Sensors **/

    private fun setupSensors() {
        val sensorManager = getSystemService(Context.SENSOR_SERVICE)


    }

}

class MainActivityUI(private val midi: RxMidi, private val sensors: Sensors) : AnkoComponent<MainActivity> {

    override fun createView(ui: AnkoContext<MainActivity>): View = with (ui) {
        verticalLayout {
            setBackgroundResource(R.color.gray)

            val grid = pushGrid {
                output.subscribe(midi.output)
            }.lparams {
                gravity = Gravity.CENTER_HORIZONTAL
                verticalMargin = dip(48)
            }

            controls(grid, midi).lparams(width = matchParent, height = 0) {
                weight = 1f
                weight = 1f
            }

            linearLayout {
                textView("Accelerometer")
                checkBox {
                    isChecked = true
                    setOnCheckedChangeListener { _, checked ->
                        if (checked) {
                            sensors.filteredNotes.removeAll(AccelerometerMidiSensor.notes)
                        } else {
                            sensors.filteredNotes.addAll(AccelerometerMidiSensor.notes)
                        }
                    }
                }
                textView("Ambient Light")
                checkBox {
                    isChecked = true
                    setOnCheckedChangeListener { _, checked ->
                        if (checked) {
                            sensors.filteredNotes.remove(LightMidiSensor.note)
                        } else {
                            sensors.filteredNotes.add(LightMidiSensor.note)
                        }
                    }
                }
                textView("Gyro")
                checkBox {
                    isChecked = true
                    setOnCheckedChangeListener { _, checked ->
                        if (checked) {
                            sensors.filteredNotes.removeAll(GyroMidiSensor.notes)
                        } else {
                            sensors.filteredNotes.addAll(GyroMidiSensor.notes)
                        }
                    }
                }
            }.lparams(width = matchParent, height = 0, weight = 1f)


            // want the controls above the grid
            removeView(grid)
            addView(grid)

            seekBar(127) { grid.velocity = it }.lparams(width = matchParent, height = 0) {
                weight = 1f
            }

            seekBar(127) { midi.output.accept(MidiMessage.Control(5, it)) }.lparams(width = matchParent, height = 0) {
                weight = 1f
            }
        }
    }

}
