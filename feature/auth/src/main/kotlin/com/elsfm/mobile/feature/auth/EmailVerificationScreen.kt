package com.elsfm.mobile.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun EmailVerificationScreen(
    email: String,
    viewModel: EmailVerificationViewModel = hiltViewModel(),
    onVerified: () -> Unit = {},
    onBackClick: () -> Unit = {},
) {
    val state = viewModel.state.collectAsState().value

    LaunchedEffect(email) {
        viewModel.onEvent(EmailVerificationEvent.EmailSet(email))
    }

    LaunchedEffect(state.isVerified) {
        if (state.isVerified) onVerified()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.auth_hero),
            contentDescription = "Siyonka Geetars",
            modifier = Modifier
                .size(80.dp)
                .padding(bottom = 16.dp),
            contentScale = ContentScale.Fit,
        )

        Text(
            text = "Verify your email",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        Text(
            text = "We sent a 6-digit security code to\n$email\n\nEnter it below to activate your account.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp),
        )

        OutlinedTextField(
            value = state.code,
            onValueChange = { viewModel.onEvent(EmailVerificationEvent.CodeChanged(it)) },
            label = { Text("6-digit code") },
            placeholder = { Text("657661") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        if (state.error != null) {
            Text(
                text = state.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Button(
            onClick = { viewModel.onEvent(EmailVerificationEvent.VerifyClicked) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            enabled = !state.isLoading && state.code.length == 6,
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            } else {
                Text("Verify")
            }
        }

        TextButton(
            onClick = onBackClick,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text("Back")
        }
    }
}
