package io.rstlne.aether

import android.util.Log
import io.reactivex.Observable

/**
 * Created by pdv on 10/27/17.
 */

fun <T, R> Observable<T>.unwrapMap(mapper: (T) -> R?): Observable<R> = this
    .flatMap {
        val ret = mapper(it)
        if (ret == null) Observable.empty() else Observable.just(ret)
    }

fun <T> Observable<T>.debug(tag: String = "Obs") = this.doOnEach { Log.d(tag, it.toString()) }