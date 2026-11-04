/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.network

import com.arslandaim.playtube.utils.PTLog
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientGateTracker @Inject constructor() {

    private class GateState {
        val consecutiveFailures = AtomicInteger(0)
        @Volatile var demotedUntilMs: Long = 0L
    }

    private val gates = ConcurrentHashMap<String, GateState>()
    private val COOLDOWN_DURATION_MS = 5 * 60 * 1000L // 5 minutes

    fun recordSuccess(clientProfile: String) {
        if (clientProfile.isBlank()) return
        val state = gates[clientProfile]
        if (state != null) {
            state.consecutiveFailures.set(0)
            state.demotedUntilMs = 0L
            PTLog.d("ClientGateTracker", "Client profile $clientProfile health restored (Success recorded)")
        }
    }

    fun recordFailure(clientProfile: String, statusCode: Int) {
        if (clientProfile.isBlank()) return
        val state = gates.getOrPut(clientProfile) { GateState() }
        val failures = state.consecutiveFailures.incrementAndGet()
        val now = System.currentTimeMillis()

        if (statusCode == 403 || statusCode == 429 || failures >= 2) {
            state.demotedUntilMs = now + COOLDOWN_DURATION_MS
            PTLog.w(
                "ClientGateTracker",
                "Client profile $clientProfile DEMOTED for ${COOLDOWN_DURATION_MS / 1000}s due to error HTTP $statusCode (failures: $failures)"
            )
        }
    }

    fun isClientHealthy(clientProfile: String): Boolean {
        if (clientProfile.isBlank()) return true
        val state = gates[clientProfile] ?: return true
        val now = System.currentTimeMillis()
        if (state.demotedUntilMs > 0L) {
            if (now < state.demotedUntilMs) {
                return false
            } else {
                // Cooldown expired, reset
                state.demotedUntilMs = 0L
                state.consecutiveFailures.set(0)
                PTLog.d("ClientGateTracker", "Client profile $clientProfile demotion cooldown expired. Marking healthy.")
            }
        }
        return true
    }

    fun prioritizeClients(availableClients: List<String>): List<String> {
        return availableClients.sortedByDescending { isClientHealthy(it) }
    }
}
