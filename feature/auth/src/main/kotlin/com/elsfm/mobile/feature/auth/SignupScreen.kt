package com.elsfm.mobile.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SignupScreen(
    viewModel: SignupViewModel = hiltViewModel(),
    onSignupSuccess: () -> Unit = {},
    onSigninClick: () -> Unit = {},
) {
    val state = viewModel.state.collectAsState().value

    if (state.isSignedUp) {
        onSignupSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Create account",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = state.email,
            onValueChange = { viewModel.onEvent(SignupEvent.EmailChanged(it)) },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = state.password,
            onValueChange = { viewModel.onEvent(SignupEvent.PasswordChanged(it)) },
            label = { Text("Password") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )

        OutlinedTextField(
            value = state.confirmPassword,
            onValueChange = { viewModel.onEvent(SignupEvent.ConfirmPasswordChanged(it)) },
            label = { Text("Confirm password") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )

        if (state.error != null) {
            Text(
                text = state.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = state.acceptTerms,
                    onCheckedChange = { viewModel.onEvent(SignupEvent.AcceptTermsChanged(it)) }
                )
                Text("I accept Terms of Service", style = MaterialTheme.typography.labelSmall)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = state.acceptPrivacy,
                    onCheckedChange = { viewModel.onEvent(SignupEvent.AcceptPrivacyChanged(it)) }
                )
                Text("I accept Privacy Policy", style = MaterialTheme.typography.labelSmall)
            }
        }

        Button(
            onClick = { viewModel.onEvent(SignupEvent.SignupClicked) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            enabled = !state.isLoading && state.email.isNotBlank() && 
                      state.password.isNotBlank() && state.acceptTerms && state.acceptPrivacy
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            } else {
                Text("Create account")
            }
        }

        TextButton(
            onClick = onSigninClick,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Already have an account? Sign in")
        }
    }
}
