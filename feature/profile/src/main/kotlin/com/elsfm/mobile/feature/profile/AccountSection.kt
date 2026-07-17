package com.elsfm.mobile.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elsfm.mobile.core.model.UserProfile

/**
 * Account settings menu shown on the Profile screen: edit-profile form (when in edit
 * mode), dark mode toggle, and logout. Mirrors the web app's account dropdown menu
 * (theme toggle + logout), adapted to an in-page section for mobile.
 */
@Composable
fun AccountSection(
    profile: UserProfile,
    isEditMode: Boolean,
    onEditProfileClicked: () -> Unit,
    onSaveProfile: (name: String, bio: String?) -> Unit,
    onCancelEdit: () -> Unit,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onChangePasswordClicked: () -> Unit = {},
    onLogout: () -> Unit,
    onManageSubscriptionClicked: () -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Account", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(12.dp))

        if (isEditMode) {
            EditProfileForm(
                profile = profile,
                onSave = onSaveProfile,
                onCancel = onCancelEdit,
            )
        } else {
            TextButton(onClick = onEditProfileClicked) {
                Text("Edit Profile")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Dark Mode", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = isDarkMode, onCheckedChange = onToggleDarkMode)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        TextButton(
            onClick = onChangePasswordClicked,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Change Password")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        TextButton(
            onClick = onManageSubscriptionClicked,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Subscription")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Logout")
        }
    }
}

@Composable
private fun EditProfileForm(
    profile: UserProfile,
    onSave: (name: String, bio: String?) -> Unit,
    onCancel: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(profile.name) }
    var bio by rememberSaveable { mutableStateOf(profile.bio.orEmpty()) }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Bio") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = {
                    onSave(name, bio.ifBlank { null })
                },
            ) {
                Text("Save")
            }
        }
    }
}
