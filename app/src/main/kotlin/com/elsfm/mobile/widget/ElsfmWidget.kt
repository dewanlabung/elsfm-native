package com.elsfm.mobile.widget

import android.graphics.Bitmap
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.elsfm.mobile.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ElsfmWidget : GlanceAppWidget() {

    // Single-size mode: LocalSize.current gives the actual allocated size,
    // so we can pick the right layout at render time.
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = WidgetPreferences.load(context)
        val artwork: Bitmap? = state.artworkUrl?.let { url ->
            withContext(Dispatchers.IO) {
                runCatching {
                    val req = ImageRequest.Builder(context)
                        .data(url)
                        .allowHardware(false)  // Glance requires software bitmaps
                        .build()
                    val result = context.imageLoader.execute(req)
                    (result as? SuccessResult)?.drawable?.toBitmap()
                }.getOrNull()
            }
        }

        provideContent {
            val size = LocalSize.current
            val bg = ColorProvider(Color(0xFF1A1A2E))
            val colorPrimary = ColorProvider(Color.White)
            val colorSecondary = ColorProvider(Color(0xFFAAAAAA))
            val colorAccent = ColorProvider(Color(0xFF4CAF82))

            when {
                size.height >= 140.dp -> ArtworkLayout(
                    state, artwork, bg, colorPrimary, colorSecondary, colorAccent
                )
                size.height >= 90.dp -> CompactLayout(
                    state, artwork, bg, colorPrimary, colorSecondary, colorAccent
                )
                else -> WideLayout(state, bg, colorPrimary, colorSecondary, colorAccent)
            }
        }
    }
}

@Composable
private fun ArtworkLayout(
    state: WidgetPreferences.State,
    artwork: Bitmap?,
    bg: ColorProvider,
    colorPrimary: ColorProvider,
    colorSecondary: ColorProvider,
    colorAccent: ColorProvider,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bg)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Artwork square
        ArtworkImage(
            artwork = artwork,
            modifier = GlanceModifier.size(120.dp, 120.dp),
        )

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Title
        Text(
            text = state.title.ifEmpty { "Not playing" },
            modifier = GlanceModifier.fillMaxWidth(),
            style = TextStyle(
                color = colorPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )

        // Artist
        Text(
            text = state.artist,
            modifier = GlanceModifier.fillMaxWidth(),
            style = TextStyle(color = colorSecondary, fontSize = 12.sp),
            maxLines = 1,
        )

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Controls
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayPauseButton(isPlaying = state.isPlaying, colorAccent = colorAccent)
            Spacer(modifier = GlanceModifier.width(16.dp))
            SkipNextButton(colorAccent = colorAccent)
        }
    }
}

@Composable
private fun CompactLayout(
    state: WidgetPreferences.State,
    artwork: Bitmap?,
    bg: ColorProvider,
    colorPrimary: ColorProvider,
    colorSecondary: ColorProvider,
    colorAccent: ColorProvider,
) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bg)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Artwork thumbnail
        ArtworkImage(
            artwork = artwork,
            modifier = GlanceModifier.size(70.dp, 70.dp),
        )

        Spacer(modifier = GlanceModifier.width(8.dp))

        // Title + Artist (fill remaining space)
        Column(
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = state.title.ifEmpty { "Not playing" },
                style = TextStyle(
                    color = colorPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            Text(
                text = state.artist,
                style = TextStyle(color = colorSecondary, fontSize = 11.sp),
                maxLines = 1,
            )
        }

        Spacer(modifier = GlanceModifier.width(8.dp))

        // Controls
        Column(
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PlayPauseButton(isPlaying = state.isPlaying, colorAccent = colorAccent)
            Spacer(modifier = GlanceModifier.height(4.dp))
            SkipNextButton(colorAccent = colorAccent)
        }
    }
}

@Composable
private fun WideLayout(
    state: WidgetPreferences.State,
    bg: ColorProvider,
    colorPrimary: ColorProvider,
    colorSecondary: ColorProvider,
    colorAccent: ColorProvider,
) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Title • Artist (fill remaining space)
        Column(
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val trackInfo = when {
                state.title.isNotEmpty() && state.artist.isNotEmpty() ->
                    "${state.title}  ·  ${state.artist}"
                state.title.isNotEmpty() -> state.title
                else -> "Not playing"
            }
            Text(
                text = trackInfo,
                style = TextStyle(color = colorPrimary, fontSize = 12.sp),
                maxLines = 1,
            )
        }

        Spacer(modifier = GlanceModifier.width(8.dp))

        // Controls
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlayPauseButton(isPlaying = state.isPlaying, colorAccent = colorAccent)
            Spacer(modifier = GlanceModifier.width(8.dp))
            SkipNextButton(colorAccent = colorAccent)
        }
    }
}

@Composable
private fun ArtworkImage(artwork: Bitmap?, modifier: GlanceModifier) {
    if (artwork != null) {
        Image(
            provider = ImageProvider(artwork),
            contentDescription = "Album art",
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = modifier.background(ColorProvider(Color(0xFF2A2A3E))),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_shortcut_player),
                contentDescription = "Album art placeholder",
                modifier = GlanceModifier.size(36.dp, 36.dp),
            )
        }
    }
}

@Composable
private fun PlayPauseButton(isPlaying: Boolean, colorAccent: ColorProvider) {
    Text(
        text = if (isPlaying) "⏸" else "▶",
        modifier = GlanceModifier
            .padding(4.dp)
            .clickable(actionRunCallback<PlayPauseCallback>()),
        style = TextStyle(color = colorAccent, fontSize = 20.sp),
    )
}

@Composable
private fun SkipNextButton(colorAccent: ColorProvider) {
    Text(
        text = "⏭",
        modifier = GlanceModifier
            .padding(4.dp)
            .clickable(actionRunCallback<SkipNextCallback>()),
        style = TextStyle(color = colorAccent, fontSize = 20.sp),
    )
}
