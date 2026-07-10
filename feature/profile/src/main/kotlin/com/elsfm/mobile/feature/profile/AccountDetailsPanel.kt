package com.elsfm.mobile.feature.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.elsfm.mobile.core.model.UserProfile

/**
 * Mirrors the real PWA "Update name and profile image" account setting: an avatar with
 * a camera-icon overlay for picking a new image, a "Remove image" link, a read-only
 * email row, an editable name field and a Save button. Backed by [AccountViewModel] ->
 * real `POST api/v1/uploads` + `PUT api/v1/users/{id}` endpoints.
 */
@Composable
fun AccountDetailsPanel(
    profile: UserProfile,
    isSavingName: Boolean,
    isUploadingAvatar: Boolean,
    accountError: String?,
    onAvatarSelected: (bytes: ByteArray, filename: String, mimeType: String) -> Unit,
    onRemoveAvatar: () -> Unit,
    onSaveName: (String) -> Unit,
) {
    val context = LocalContext.current
    var name by rememberSaveable(profile.name) { mutableStateOf(profile.name) }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri) ?: "image/jpeg"
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                val extension = mimeType.substringAfter("/", "jpg")
                onAvatarSelected(bytes, "avatar.$extension", mimeType)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Update name and profile image", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clickable(enabled = !isUploadingAvatar) {
                        pickMedia.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (profile.profileImage != null) {
                    AsyncImage(
                        model = profile.profileImage,
                        contentDescription = "Profile avatar",
                        modifier = Modifier
                            .size(90.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(profile.name.take(1).uppercase(), style = MaterialTheme.typography.headlineMedium)
                    }
                }

                if (isUploadingAvatar) {
                    CircularProgressIndicator(modifier = Modifier.size(90.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Change profile image",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                if (profile.profileImage != null) {
                    TextButton(onClick = onRemoveAvatar, enabled = !isUploadingAvatar) {
                        Text("Remove image")
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(profile.email, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (accountError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(accountError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(
                onClick = { onSaveName(name) },
                enabled = !isSavingName && name.isNotBlank() && name != profile.name,
            ) {
                Text("Save")
            }
        }
    }
}
