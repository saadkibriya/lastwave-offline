package com.lastwave.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LrcParserTest {

    @Test
    fun testParseStandardLrc() {
        val sampleLrc = """
            [00:12.00]First line of lyrics
            [01:05.50]Second line of lyrics
            [02:15.123]Third line of lyrics
        """.trimIndent()

        val parsed = LrcParser.parse(sampleLrc)
        assertEquals(3, parsed.size)
        assertEquals(12000L, parsed[0].timestampMs)
        assertEquals("First line of lyrics", parsed[0].text)
        assertEquals(65500L, parsed[1].timestampMs)
        assertEquals("Second line of lyrics", parsed[1].text)
        assertEquals(135123L, parsed[2].timestampMs)
        assertEquals("Third line of lyrics", parsed[2].text)
    }
}
