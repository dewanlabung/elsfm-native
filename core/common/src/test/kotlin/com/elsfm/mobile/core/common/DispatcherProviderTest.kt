package com.elsfm.mobile.core.common

import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class DispatcherProviderTest {
    @Test
    fun `default provider exposes real coroutine dispatchers`() {
        val provider: DispatcherProvider = DefaultDispatcherProvider()

        assertEquals(Dispatchers.IO, provider.io)
        assertEquals(Dispatchers.Main, provider.main)
        assertEquals(Dispatchers.Default, provider.default)
    }
}
