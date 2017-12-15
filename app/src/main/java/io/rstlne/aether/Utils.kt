package io.rstlne.aether

import android.R
import android.content.res.ColorStateList
import android.util.Log
import io.reactivex.Observable

/**
 * Created by pdv on 12/15/17.
 */

fun colorStateList(pressed: Int, enabled: Int, disabled: Int) = ColorStateList(
    arrayOf(
        intArrayOf(R.attr.state_pressed),
        intArrayOf(R.attr.state_enabled),
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
