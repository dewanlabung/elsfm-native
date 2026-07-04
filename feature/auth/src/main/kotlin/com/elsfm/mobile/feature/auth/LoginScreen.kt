package com.elsfm.mobile.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.elsfm.mobile.core.model.User
import kotlinx.coroutines.flow.StateFlow

@Composable
fun LoginScreen(
    onLoggedIn: (User) -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val passwordSaver = remember { PasswordSaver(context) }
    var lastCredentials by remember { mutableStateOf<Pair<String, String>?>(null) }

    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        val currentState = state
        if (currentState is LoginUiState.Success) {
            lastCredentials?.let { (email, password) -> passwordSaver.save(email, password) }
            onLoggedIn(currentState.user)
        }
    }

    LoginScreenContent(
        state = viewModel.state,
        onLoginClicked = { email, password ->
            lastCredentials = email to password
            viewModel.onLoginClicked(email, password)
        },
    )
}

@Composable
fun LoginScreenContent(
    state: StateFlow<LoginUiState>,
    onLoginClicked: (email: String, password: String) -> Unit,
) {
    val currentState by state.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val fieldErrors = (currentState as? LoginUiState.FieldErrors)?.errors ?: emptyMap()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            isError = fieldErrors.containsKey("email"),
            modifier = Modifier.fillMaxWidth().testTag("email_field"),
        )
        fieldErrors["email"]?.forEach { message -> Text(message) }

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth().testTag("password_field"),
        )

        if (currentState == LoginUiState.InvalidCredentials) {
            Text("Incorrect email or password.")
        }
        if (currentState == LoginUiState.NetworkError) {
            Text("Couldn't reach elsfm.com. Check your connection and try again.")
        }

        Button(
            onClick = { onLoginClicked(email, password) },
            enabled = currentState != LoginUiState.Loading,
            modifier = Modifier.testTag("login_button"),
        ) {
            if (currentState == LoginUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.testTag("login_progress"))
            } else {
                Text("Log in")
            }
        }
    }
}
