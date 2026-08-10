/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.utils

import java.io.IOException
import java.net.UnknownHostException

sealed class PlayTubeError {
    object Network : PlayTubeError()
    data class Extraction(val errorMessage: String) : PlayTubeError()
    data class Unknown(val errorMessage: String) : PlayTubeError()
    object AuthError : PlayTubeError()
    object ApiThrottled : PlayTubeError()
    object StorageFull : PlayTubeError()
    data class UnsupportedFormat(val format: String) : PlayTubeError()

    fun getMessage(): String {
        return when (this) {
            is Network -> "No internet connection"
            is Extraction -> errorMessage
            is Unknown -> errorMessage
            is AuthError -> "Authentication required"
            is ApiThrottled -> "Service busy, try again later"
            is StorageFull -> "Storage is full"
            is UnsupportedFormat -> "Format $format is not supported"
        }
    }

    companion object {
        fun fromThrowable(t: Throwable): PlayTubeError {
            return when (t) {
                is UnknownHostException, is IOException -> Network
                is java.lang.SecurityException -> AuthError
                else -> {
                    val message = t.message ?: "An unexpected error occurred"
                    if (message.contains("429") || message.contains("Too Many Requests")) ApiThrottled
                    else Extraction(message)
                }
            }
        }
    }
}
