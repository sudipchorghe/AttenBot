package com.example.attenbot.util

import java.net.URI

object UrlUtils {

    const val DEFAULT_BASE_URL = "http://192.168.4.1/"

    /**
     * Normalizes a user-entered ESP32 base URL.
     * Ensures scheme (http/https), strips endpoint paths like /api/status or /download,
     * and guarantees a trailing slash.
     */
    fun normalizeBaseUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) {
            return DEFAULT_BASE_URL
        }

        var urlWithScheme = trimmed
        if (!urlWithScheme.startsWith("http://", ignoreCase = true) &&
            !urlWithScheme.startsWith("https://", ignoreCase = true)
        ) {
            urlWithScheme = "http://$urlWithScheme"
        }

        return try {
            val uri = URI(urlWithScheme)
            val host = uri.host
            if (host.isNull_or_empty_host()) {
                // Handle raw IP or hostname parsing fallback
                cleanUrlString(urlWithScheme)
            } else {
                val scheme = uri.scheme ?: "http"
                val port = if (uri.port != -1) ":${uri.port}" else ""
                "$scheme://$host$port/"
            }
        } catch (e: Exception) {
            cleanUrlString(urlWithScheme)
        }
    }

    private fun String.isNull_or_empty_host(): Boolean {
        return this.isBlank()
    }

    private fun cleanUrlString(input: String): String {
        var clean = input
        // Remove trailing paths if user typed http://192.168.4.1/api/status or /download
        val knownSuffixes = listOf("/api/status", "/api/attendance", "/api/reset", "/api/set-time", "/download", "/api")
        for (suffix in knownSuffixes) {
            if (clean.endsWith(suffix, ignoreCase = true)) {
                clean = clean.substring(0, clean.length - suffix.length)
                break
            }
        }
        clean = clean.trimEnd('/')
        return "$clean/"
    }

    fun isValidUrl(url: String): Boolean {
        return try {
            val normalized = normalizeBaseUrl(url)
            val uri = URI(normalized)
            uri.host != null && uri.host.isNotBlank()
        } catch (e: Exception) {
            false
        }
    }
}
