package com.elsfm.mobile.core.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

private const val IMAGE_BASE_URL = "https://www.elsfm.com/"

/**
 * The Laravel backend returns image paths inconsistently: most entity artwork
 * (track/album/playlist/artist images) is a relative path like
 * "storage/artwork/xyz.png", while a few nested fields (e.g. playlist editor
 * avatars) are already full URLs. Coil cannot load a schemeless relative path,
 * which is why every content thumbnail in the app renders blank. This
 * serializer normalizes both shapes into a fully-qualified URL at decode time
 * so no call site has to remember to prefix anything.
 */
internal object ImageUrlSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ImageUrl", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val raw = decoder.decodeString()
        return if (raw.startsWith("http://") || raw.startsWith("https://")) {
            raw
        } else {
            IMAGE_BASE_URL + raw.removePrefix("/")
        }
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}
