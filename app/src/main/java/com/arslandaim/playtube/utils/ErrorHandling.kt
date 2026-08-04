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

    fun getMessage(): String {
        return when (this) {
            is Network -> "No internet connection"
            is Extraction -> errorMessage
            is Unknown -> errorMessage
        }
    }

    companion object {
        fun fromThrowable(t: Throwable): PlayTubeError {
            return when (t) {
                is UnknownHostException, is IOException -> Network
                else -> Extraction(t.message ?: "Extraction failed")
            }
        }
    }
}
