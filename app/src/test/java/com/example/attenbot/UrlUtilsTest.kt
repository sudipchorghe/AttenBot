package com.example.attenbot

import com.example.attenbot.util.UrlUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlUtilsTest {

    @Test
    fun normalizeBaseUrl_addsSchemeAndTrailingSlash() {
        val input = "192.168.4.1"
        val normalized = UrlUtils.normalizeBaseUrl(input)
        assertEquals("http://192.168.4.1/", normalized)
    }

    @Test
    fun normalizeBaseUrl_removesEndpointPaths() {
        assertEquals("http://192.168.4.1/", UrlUtils.normalizeBaseUrl("http://192.168.4.1/api/status"))
        assertEquals("http://192.168.4.1/", UrlUtils.normalizeBaseUrl("http://192.168.4.1/download"))
        assertEquals("http://192.168.4.1/", UrlUtils.normalizeBaseUrl("http://192.168.4.1/api/attendance"))
        assertEquals("http://192.168.4.1/", UrlUtils.normalizeBaseUrl("http://192.168.4.1/api/reset"))
    }

    @Test
    fun normalizeBaseUrl_handlesBlankUrl() {
        val normalized = UrlUtils.normalizeBaseUrl("   ")
        assertEquals("http://192.168.4.1/", normalized)
    }

    @Test
    fun isValidUrl_validatesHosts() {
        assertTrue(UrlUtils.isValidUrl("http://192.168.4.1/"))
        assertTrue(UrlUtils.isValidUrl("192.168.4.2"))
    }
}
