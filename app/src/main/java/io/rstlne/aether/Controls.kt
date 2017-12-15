package io.rstlne.aether

import android.R
import android.view.Gravity
import android.view.View
import android.view.ViewManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import org.jetbrains.anko.button
import org.jetbrains.anko.linearLayout
import org.jetbrains.anko.seekBar
import org.jetbrains.anko.spinner
import org.jetbrains.anko.textView

/**
 * Created by pdv on 12/15/17.
 */

fun ViewManager.spinner(items: List<String>, onItemSelected: (Int) -> Unit) = spinner {
    adapter = ArrayAdapter<String>(context, R.layout.simple_spinner_dropdown_item, items)
    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, idx: Int, p3: Long) {
            onItemSelected(idx)
        }
        override fun onNothingSelected(p0: AdapterView<*>?) = Unit
    }
}

fun ViewManager.seekBar(max: Int, onChange: (Int) -> Unit) = seekBar {
    this.max = max
    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(p0: SeekBar?, value: Int, p2: Boolean) {
            onChange(value)
        }
        override fun onStartTrackingTouch(p0: SeekBar?) = Unit
        override fun onStopTrackingTouch(p0: SeekBar?) = Unit
    })
}

fun ViewManager.controls(grid: PushGrid, midi: RxMidi) = linearLayout {
    gravity = Gravity.CENTER

    spinner(NOTE_NAMES) { grid.root = it + C2 }

    spinner(SCALES_BY_NAME.map { it.first }) { grid.scale = SCALES_BY_NAME[it].second }

    textView("Channel")

    spinner((1..16).map(Int::toString)) { midi.channel = it }

    button {
        text = "!"
        setOnClickListener {
            midi.start()
        }
    }
}