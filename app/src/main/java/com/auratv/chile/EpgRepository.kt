package com.auratv.chile

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object EpgRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /** Fuentes EPG Chile (fallback en orden) */
    private val sources = listOf(
        "https://epg.lat/files/cl.xml.gz",
        "https://iptv-epg.org/files/epg-cl.xml.gz"
    )

    fun loadChileEpg(): List<EpgProgram> {
        for (url in sources) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "AuraTV/1.0")
                    .build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) continue
                val body = response.body ?: continue
                val isGzip = url.endsWith(".gz", ignoreCase = true) ||
                    response.header("Content-Encoding")?.contains("gzip") == true
                val programs = XmltvParser.parse(body.byteStream(), isGzip = isGzip)
                if (programs.isNotEmpty()) return programs
            } catch (_: Exception) {
                // probar siguiente fuente
            }
        }
        return emptyList()
    }

    fun matchChannels(
        channels: List<Channel>,
        programs: List<EpgProgram>
    ): List<ChannelWithEpg> {
        val byId = programs.groupBy { it.channelId.lowercase() }
        return channels.map { ch ->
            val key = (ch.tvgId ?: ch.name).lowercase()
            val matched = byId[key]
                ?: byId.entries.firstOrNull { (id, _) ->
                    id.contains(ch.name.lowercase().take(6)) ||
                        ch.name.lowercase().contains(id.take(6))
                }?.value
                ?: emptyList()
            ChannelWithEpg(ch, matched.sortedBy { it.start })
        }
    }
}
