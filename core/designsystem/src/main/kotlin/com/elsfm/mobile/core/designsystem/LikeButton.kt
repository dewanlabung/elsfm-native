package com.elsfm.mobile.core.designsystem

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Reusable like/save toggle button (filled heart when liked, outline when not).
 *
 * Stateless: callers own the actual liked/loading state and the mutation logic
 * (calling the real add/remove-from-library endpoint). This composable only
 * renders and reports taps, so it can be reused across any track row (Library,
 * Search results, Player, etc.) without duplicating that logic.
 */
@Composable
fun LikeButton(
    isLiked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier.testTag("like_button"),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
        } else if (isLiked) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Remove from library",
                tint = MaterialTheme.colorScheme.primary,
            )
        } else {
            Icon(
                imageVector = Icons.Filled.FavoriteBorder,
                contentDescription = "Add to library",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
