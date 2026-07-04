package com.elsfm.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.elsfm.mobile.core.model.User

@Composable
fun HomePlaceholderScreen(user: User, onLogoutClicked: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text("Logged in as ${user.email}")
        Button(onClick = onLogoutClicked) {
            Text("Log out")
        }
    }
}
