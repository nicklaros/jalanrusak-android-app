package com.jalanrusak.util

/**
 * A generic wrapper for handling success and error states
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>()

    fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    fun onError(action: (String, Throwable?) -> Unit): Result<T> {
        if (this is Error) action(message, cause)
        return this
    }

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw cause ?: IllegalStateException(message)
    }
}

fun <T> Result.Companion.catch(block: () -> T): Result<T> = try {
    Success(block())
} catch (e: Exception) {
    Error(e.message ?: "Unknown error", e)
}

suspend fun <T> Result.Companion.catchSuspend(block: suspend () -> T): Result<T> = try {
    Success(block())
} catch (e: Exception) {
    Error(e.message ?: "Unknown error", e)
}
