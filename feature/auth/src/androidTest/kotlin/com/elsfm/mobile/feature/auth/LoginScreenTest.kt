package com.elsfm.mobile.feature.auth

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsDisplayed
import com.elsfm.mobile.core.designsystem.ElsfmTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun enteringCredentialsAndTappingLoginInvokesTheCallback() {
        val state = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
        var loginClicked: Pair<String, String>? = null

        composeTestRule.setContent {
            ElsfmTheme {
                LoginScreenContent(
                    state = state,
                    onLoginClicked = { email, password -> loginClicked = email to password },
                )
            }
        }

        composeTestRule.onNodeWithTag("email_field").performTextInput("test.elsfm@gmail.com")
        composeTestRule.onNodeWithTag("password_field").performTextInput("secret")
        composeTestRule.onNodeWithTag("login_button").performClick()

        assert(loginClicked == ("test.elsfm@gmail.com" to "secret"))
    }

    @Test
    fun fieldErrorsAreDisplayed() {
        val state = MutableStateFlow<LoginUiState>(
            LoginUiState.FieldErrors(mapOf("email" to listOf("These credentials do not match our records."))),
        )

        composeTestRule.setContent {
            ElsfmTheme {
                LoginScreenContent(state = state, onLoginClicked = { _, _ -> })
            }
        }

        composeTestRule.onNodeWithText("These credentials do not match our records.").assertIsDisplayed()
    }
}
