package com.elsfm.mobile.core.designsystem

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Rule
import org.junit.Test

class ElsfmThemeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun themeRendersContentWithoutCrashing() {
        composeTestRule.setContent {
            ElsfmTheme {
                Text("Hello ELSFM")
            }
        }

        composeTestRule.onNodeWithText("Hello ELSFM").assertIsDisplayed()
    }
}
