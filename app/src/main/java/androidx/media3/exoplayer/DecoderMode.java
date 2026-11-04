package androidx.media3.exoplayer;

import androidx.media3.common.DecoderMode;

// LIVE-ONLY STUB (extra, beyond the run #7 list): the exoplayer-side
// decoder mode from the author's private Media3 fork, distinct from
// androidx.media3.common.DecoderMode. Public Media3 1.11 has no such class.
// It mirrors the common enum and converts between the two; the conversion
// is used by ExoPlayerEngine/ExoPlayerSession, while the actual decoder
// selection in the live-only build is a no-op (see DecoderManager, which
// remains unresolved).
public enum DecoderMode {

    AUTO,
    HARDWARE,
    SOFTWARE,
    FFMPEG;

    public androidx.media3.common.DecoderMode toCommonDecoderMode() {
        return androidx.media3.common.DecoderMode.valueOf(name());
    }

    public static DecoderMode fromCommonDecoderMode(androidx.media3.common.DecoderMode mode) {
        return mode == null ? AUTO : DecoderMode.valueOf(mode.name());
    }
}
