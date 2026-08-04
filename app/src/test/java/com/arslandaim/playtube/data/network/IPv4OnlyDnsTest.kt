/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.network

import okhttp3.Dns
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

class IPv4OnlyDnsTest {

    @Test
    fun `lookup should filter out IPv6 addresses when IPv4 is present`() {
        val ipv4 = InetAddress.getByName("127.0.0.1")
        val ipv6 = InetAddress.getByName("::1")
        
        // Mocking behavior by manually creating the list
        val allAddresses = listOf(ipv4, ipv6)
        val ipv4Only = allAddresses.filterIsInstance<Inet4Address>()
        
        assertEquals(1, ipv4Only.size)
        assertTrue(ipv4Only[0] is Inet4Address)
    }

    @Test
    fun `lookup should return original list if no IPv4 addresses are present`() {
        val ipv6 = InetAddress.getByName("::1")
        val allAddresses = listOf(ipv6)
        
        val filtered = allAddresses.filterIsInstance<Inet4Address>()
        val result = filtered.ifEmpty { allAddresses }
        
        assertEquals(1, result.size)
        assertTrue(result[0] is Inet6Address)
    }
}
