package com.rainyseason.cj

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized

fun <T, R> Async<T>.mapSuccess(onSuccess: (T) -> R): Async<R> {
    return when (this) {
        is Uninitialized -> Uninitialized
        is Loading<T> -> Loading(value = null)
        is Fail<T> -> Fail(error = this.error, value = null)
        is Success<T> -> Success(value = onSuccess(this.invoke()))
    }
}