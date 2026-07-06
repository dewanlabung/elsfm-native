package com.elsfm.mobile.feature.profile

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeThemeStore(initialDarkMode: Boolean = false) : ThemeStore {
    private var darkMode = initialDarkMode
    var setDarkModeCallCount = 0
        private set

    override fun isDarkMode(): Boolean = darkMode

    override fun setDarkMode(isDarkMode: Boolean) {
        darkMode = isDarkMode
        setDarkModeCallCount++
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {

    @Test
    fun `initial state reflects stored preference`() = runTest {
        val store = FakeThemeStore(initialDarkMode = true)
        val viewModel = ThemeViewModel(store)

        assertTrue(viewModel.isDarkMode.value)
    }

    @Test
    fun `setDarkMode true persists and updates state`() = runTest {
        val store = FakeThemeStore(initialDarkMode = false)
        val viewModel = ThemeViewModel(store)

        viewModel.setDarkMode(true)

        assertTrue(viewModel.isDarkMode.value)
        assertTrue(store.isDarkMode())
        assertEquals(1, store.setDarkModeCallCount)
    }

    @Test
    fun `toggleTheme flips current state`() = runTest {
        val store = FakeThemeStore(initialDarkMode = false)
        val viewModel = ThemeViewModel(store)

        viewModel.toggleTheme()
        assertTrue(viewModel.isDarkMode.value)

        viewModel.toggleTheme()
        assertFalse(viewModel.isDarkMode.value)
    }
}
