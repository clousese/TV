package androidx.media3.common;

// LIVE-ONLY STUB: the author's private Media3 fork defines DecoderMode with
// hardware/software/ffmpeg decoder selection. Public Media3 1.11 has no such
// class. This stub keeps the decoder-mode setting UI and PlayerEngine API
// compiling; live playback always uses the stock decoder path (AUTO).
public enum DecoderMode {
    AUTO,
    HARDWARE,
    SOFTWARE,
    FFMPEG
}
