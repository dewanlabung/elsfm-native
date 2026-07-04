package com.elsfm.mobile.core.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoginResponseSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decodes real backend login response shape`() {
        val body = """
            {
              "status": "success",
              "user": {
                "id": 207,
                "username": null,
                "name": "ELSFM APP",
                "email": "test.elsfm@gmail.com",
                "avatar_url": null,
                "permissions": [
                  {"id": 49, "name": "music.view", "restrictions": []},
                  {"id": 52, "name": "music.embed", "restrictions": []}
                ],
                "access_token": "1|abcdef1234567890"
              },
              "themes": {"light": {}, "dark": {}},
              "menus": [],
              "settings": {},
              "locales": []
            }
        """.trimIndent()

        val decoded = json.decodeFromString<LoginResponse>(body)

        assertEquals(207, decoded.user.id)
        assertEquals("ELSFM APP", decoded.user.name)
        assertNull(decoded.user.username)
        assertEquals(2, decoded.user.permissions.size)
        assertEquals("music.view", decoded.user.permissions.first().name)
        assertEquals("1|abcdef1234567890", decoded.user.accessToken)
    }

    @Test
    fun `decodes laravel validation error shape`() {
        val body = """
            {"message": "The given data was invalid.", "errors": {"email": ["These credentials do not match our records."]}}
        """.trimIndent()

        val decoded = json.decodeFromString<LaravelValidationError>(body)

        assertEquals("The given data was invalid.", decoded.message)
        assertEquals(listOf("These credentials do not match our records."), decoded.errors["email"])
    }

    @Test
    fun `encodes login request with snake case token_name field`() {
        val request = LoginRequest(email = "a@b.com", password = "secret", tokenName = "android-uuid-1")

        val encoded = json.encodeToString(LoginRequest.serializer(), request)

        assertEquals(true, encoded.contains("\"token_name\":\"android-uuid-1\""))
    }
}
