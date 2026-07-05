package com.elsfm.mobile.core.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `Channel deserializes from API response`() {
        val json = """
            {
              "id": 1,
              "name": "Sunday School"
            }
        """.trimIndent()

        val channel = this.json.decodeFromString<Channel>(json)

        assertEquals(1, channel.id)
        assertEquals("Sunday School", channel.name)
    }
}
