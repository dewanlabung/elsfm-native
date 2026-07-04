package com.elsfm.mobile.core.network

import com.elsfm.mobile.core.model.LoginResponse
import org.junit.Assert.assertEquals
import org.junit.Test

class ElsfmJsonTest {
    @Test
    fun `ignores unknown top level keys from the real bootstrap payload`() {
        val json = elsfmJson()
        val body = """
            {"status":"success","user":{"id":1,"email":"a@b.com"},"themes":{},"menus":[],"settings":{},"locales":[]}
        """.trimIndent()

        val decoded = json.decodeFromString<LoginResponse>(body)

        assertEquals(1, decoded.user.id)
        assertEquals("a@b.com", decoded.user.email)
    }
}
