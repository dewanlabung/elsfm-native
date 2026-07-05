package com.elsfm.mobile.core.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class UserProfileSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `deserializes a real user profile JSON shape`() {
        val body = """
            {
                "id": 1,
                "name": "John Doe",
                "email": "john@example.com",
                "image": "https://example.com/avatar.jpg",
                "bio": "Music lover",
                "followers_count": 150,
                "followed_count": 50
            }
        """.trimIndent()

        val profile = json.decodeFromString<UserProfile>(body)

        assertEquals(1, profile.id)
        assertEquals("John Doe", profile.name)
        assertEquals("john@example.com", profile.email)
        assertEquals("https://example.com/avatar.jpg", profile.profileImage)
        assertEquals("Music lover", profile.bio)
        assertEquals(150, profile.followersCount)
        assertEquals(50, profile.followedCount)
    }
}
