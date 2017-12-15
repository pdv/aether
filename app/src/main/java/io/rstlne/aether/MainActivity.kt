package io.rstlne.aether

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Bundle
import android.os.ParcelUuid
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Gravity
import org.jetbrains.anko.dip
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.verticalMargin


class MainActivity : AppCompatActivity() {

    companion object {
        val MIDI_UUID = "03B80E5A-EDE8-4B33-A751-6CE34EC4C700"
        val BOTTOM_NOTE = 24
    }

    private lateinit var midi: RxMidi
    private lateinit var grid: PushGrid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        midi = RxMidiImpl(this)
        midi.start()
        // requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)

        verticalLayout {
            setBackgroundResource(R.color.gray)

            grid = pushGrid {
                output.subscribe(midi.output)
            }.lparams {
                gravity = Gravity.CENTER_HORIZONTAL
                verticalMargin = dip(48)
            }

            controls(grid, midi).lparams(width = matchParent, height = 0) {
                weight = 1f
                weight = 1f
            }

            // want the controls above the grid
            removeView(grid)
            addView(grid)

            seekBar(127) { grid.velocity = it }.lparams(width = matchParent, height = 0) {
                weight = 1f
            }
        }
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        setupBluetooth()
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}
