package io.rstlne.aether

import android.content.Context
import android.support.v4.content.ContextCompat
import android.view.MotionEvent
import android.view.View
import android.view.ViewManager
import com.jakewharton.rxbinding2.view.touches
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import org.jetbrains.anko._GridLayout
import org.jetbrains.anko.custom.ankoView
import org.jetbrains.anko.dip
import org.jetbrains.anko.margin
import org.jetbrains.anko.view

/**
 * Created by pdv on 12/15/17.
 */

fun ViewManager.pushGrid(init: PushGrid.() -> Unit = {}) = ankoView(::PushGrid, 0, init)

class PushGrid(ctx: Context) : _GridLayout(ctx) {

    companion object {
        const val ROWS = 8
        const val COLS = 6
    }

    private val _output = PublishRelay.create<MidiMessage>()
    val output: Observable<MidiMessage> = _output

    var velocity: Int = 127

    private val pads: List<View>
    private var notes: List<Int> = (0..ROWS * COLS).map { 0 }

    var root = C2
        set(value) {
            field = value
            setScale(root, scale)
        }

    var scale = Scale.MAJOR
        set(value) {
            field = value
            setScale(root, scale)
        }

    init {
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
                        .subscribe(_output)

                }.lparams {
                    margin = dip(2)
                    width = dip(64)
                    height = dip(64)
                }
                pads.put(idx, btn)
            }
        }
        this.pads = pads.toSortedMap().map { it.value }
        setScale(12, Scale.MAJOR)
    }

    private fun setScale(root: Int, intervals: List<Int>) {
        val octaveSize = intervals.sum()
        val scaleLength = intervals.count()
        val mutableNotes = mutableListOf<Int>()
        pads.forEachIndexed { index, pad ->
            val effectiveIndex = index - (index / COLS) * (COLS / 2)
            val interval = effectiveIndex % scaleLength
            val octave = effectiveIndex / scaleLength
            pad.setBackgroundResource(if (interval == 0) R.color.dark_blue else R.color.blue)
            mutableNotes.add(root + (octave * octaveSize) + (intervals.subList(0, interval).sum()))
        }
        notes = mutableNotes
    }

}
