package com.auratv.chile

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.zip.GZIPInputStream

object XmltvParser {

    private val CHILE_ZONE = ZoneId.of("America/Santiago")
    private const val HOURS_AHEAD = 48L

    fun parse(
        inputStream: InputStream,
        isGzip: Boolean = false,
        now: Long = System.currentTimeMillis()
    ): List<EpgProgram> {
        val stream = if (isGzip) GZIPInputStream(inputStream) else inputStream
        val cutoff = now + HOURS_AHEAD * 60 * 60 * 1000

        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(stream, "UTF-8")

        val programs = mutableListOf<EpgProgram>()
        var event = parser.eventType

        var channelId: String? = null
        var start = 0L
        var stop = 0L
        var title = ""
        var description: String? = null

        try {
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> when (parser.name) {
                        "programme" -> {
                            channelId = parser.getAttributeValue(null, "channel")
                            start = parseXmltvDate(parser.getAttributeValue(null, "start"))
                            stop = parseXmltvDate(parser.getAttributeValue(null, "stop"))
                            title = ""
                            description = null
                        }
                        "title" -> if (title.isEmpty()) title = parser.nextText().trim()
                        "desc" -> if (description == null) {
                            description = parser.nextText().trim().takeIf { it.isNotBlank() }
                        }
                    }
                    XmlPullParser.END_TAG -> if (parser.name == "programme") {
                        if (channelId != null &&
                            title.isNotBlank() &&
                            stop > now &&
                            start < cutoff
                        ) {
                            programs.add(
                                EpgProgram(
                                    channelId = channelId,
                                    start = start,
                                    stop = stop,
                                    title = title,
                                    description = description
                                )
                            )
                        }
                    }
                }
                event = parser.next()
            }
        } finally {
            stream.close()
        }
        return programs
    }

    private fun parseXmltvDate(dateStr: String?): Long {
        if (dateStr.isNullOrBlank() || dateStr.length < 14) return 0L
        return try {
            val clean = dateStr.take(14)
            val year = clean.substring(0, 4).toInt()
            val month = clean.substring(4, 6).toInt()
            val day = clean.substring(6, 8).toInt()
            val hour = clean.substring(8, 10).toInt()
            val minute = clean.substring(10, 12).toInt()
            val second = clean.substring(12, 14).toInt()
            val local = LocalDateTime.of(year, month, day, hour, minute, second)
            val offsetPart = dateStr.drop(14).trim()
            val zoned = if (offsetPart.startsWith("+") || offsetPart.startsWith("-")) {
                val offset = try {
                    ZoneOffset.of(offsetPart.take(5))
                } catch (_: Exception) {
                    ZoneOffset.UTC
                }
                local.atOffset(offset).atZoneSameInstant(CHILE_ZONE)
            } else {
                local.atZone(ZoneOffset.UTC).withZoneSameInstant(CHILE_ZONE)
            }
            zoned.toInstant().toEpochMilli()
        } catch (_: Exception) {
            0L
        }
    }
}
