package com.lastwave.app.data

import com.lastwave.app.model.LrcLine
import java.io.File
import java.util.regex.Pattern

object LrcParser {
    private val LRC_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)")

    fun parse(lrcContent: String): List<LrcLine> {
        val lines = mutableListOf<LrcLine>()
        lrcContent.lineSequence().forEach { line ->
            val matcher = LRC_PATTERN.matcher(line.trim())
            if (matcher.matches()) {
                val min = matcher.group(1)?.toLongOrNull() ?: 0L
                val sec = matcher.group(2)?.toLongOrNull() ?: 0L
                val msPart = matcher.group(3) ?: "0"
                val ms = if (msPart.length == 2) msPart.toLong() * 10 else msPart.toLong()
                val timestamp = (min * 60 + sec) * 1000 + ms
                val text = matcher.group(4)?.trim().orEmpty()
                if (text.isNotEmpty()) {
                    lines.add(LrcLine(timestamp, text))
                }
            }
        }
        return lines.sortedBy { it.timestampMs }
    }

    fun findLrcForTrack(filePath: String): List<LrcLine>? {
        return try {
            val audioFile = File(filePath)
            val lrcFile = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.lrc")
            if (lrcFile.exists() && lrcFile.canRead()) {
                parse(lrcFile.readText())
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
