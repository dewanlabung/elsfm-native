package com.elsfm.mobile.core.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Some real tracks have `"duration": null` in the backend response (confirmed live, e.g.
 * page 3 of the "2015 EL Shaddai Youth Camp Songs" playlist) even though [Track.durationMs]
 * is otherwise always present as a number. A plain non-nullable `Long` fails to decode a
 * literal JSON `null`, which crashed parsing for the *entire page* that track was on -
 * not just that one track - which is why playlist loading silently stalled partway
 * through instead of loading every track.
 */
object NullSafeLongSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("NullSafeLong", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Long =
        decoder.decodeNullableSerializableValue(Long.serializer()) ?: 0L

    override fun serialize(encoder: Encoder, value: Long) = encoder.encodeLong(value)
}
