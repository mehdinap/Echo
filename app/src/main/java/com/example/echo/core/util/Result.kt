package com.example.echo.core.util

/** A tiny Result type so repositories never throw across a layer boundary. */
sealed interface Outcome<out T> {
    data class Success<T>(val data: T) : Outcome<T>
    data class Failure(val error: AppError) : Outcome<Nothing>
}

sealed interface AppError {
    data object Network : AppError
    data object Unauthorized : AppError
    data class Http(val code: Int) : AppError
    data class Unknown(val message: String?) : AppError
}

inline fun <T> runCatchingOutcome(block: () -> T): Outcome<T> = try {
    Outcome.Success(block())
} catch (e: java.io.IOException) {
    Outcome.Failure(AppError.Network)
} catch (e: retrofit2.HttpException) {
    Outcome.Failure(if (e.code() == 401) AppError.Unauthorized else AppError.Http(e.code()))
} catch (e: Throwable) {
    Outcome.Failure(AppError.Unknown(e.message))
}
