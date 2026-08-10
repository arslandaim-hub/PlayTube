/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.utils

import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException

class ErrorHandlingTest {

    @Test
    fun `fromThrowable maps Network errors correctly`() {
        assertTrue(PlayTubeError.fromThrowable(UnknownHostException()) is PlayTubeError.Network)
        assertTrue(PlayTubeError.fromThrowable(IOException()) is PlayTubeError.Network)
    }

    @Test
    fun `fromThrowable maps AuthError correctly`() {
        assertTrue(PlayTubeError.fromThrowable(SecurityException()) is PlayTubeError.AuthError)
    }

    @Test
    fun `fromThrowable maps ApiThrottled correctly`() {
        val throttledException = Exception("HTTP 429 Too Many Requests")
        assertTrue(PlayTubeError.fromThrowable(throttledException) is PlayTubeError.ApiThrottled)
    }

    @Test
    fun `getMessage returns human readable strings`() {
        assertEquals("No internet connection", PlayTubeError.Network.getMessage())
        assertEquals("Authentication required", PlayTubeError.AuthError.getMessage())
        assertEquals("Custom Error", PlayTubeError.Unknown("Custom Error").getMessage())
    }
}
