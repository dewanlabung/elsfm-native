package com.elsfm.mobile.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

@Composable
fun PasswordResetScreen(
    viewModel: PasswordResetViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
) {
    val state = viewModel.state.collectAsState().value

    if (state.isSubmitted) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = "http://localhost:8000/forgot-password",
                contentDescription = "Password reset hero image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(bottom = 32.dp),
                contentScale = ContentScale.Crop
            )

            Text(
                text = "Check your email",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "We've sent a password reset link to ${state.email}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 32.dp),
                textAlign = TextAlign.Center
            )
            Button(onClick = onBackClick) {
                Text("Back to sign in")
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = "http://localhost:8000/forgot-password",
                contentDescription = "Password reset hero image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(bottom = 32.dp),
                contentScale = ContentScale.Crop
            )

            Text(
                text = "Reset password",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = state.email,
                onValueChange = { viewModel.onEvent(PasswordResetEvent.EmailChanged(it)) },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
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

            Button(
                onClick = { viewModel.onEvent(PasswordResetEvent.ResetClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                enabled = !state.isLoading && state.email.isNotBlank()
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                } else {
                    Text("Continue")
                }
            }

            TextButton(
                onClick = onBackClick,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Back")
            }
        }
    }
}
