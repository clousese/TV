package androidx.media3.common.audio;

// LIVE-ONLY STUB: audio effect processor from the author's private Media3
// fork (software audio processing chain). Public Media3 1.11 has no such
// class. The stub is a no-op: ExoAudioEffectController keeps its API, but
// no software processing is applied to the stock player.
public final class AudioEffectProcessor {

    public void setParameters(AudioEffectParameters parameters, boolean softwareEqualizer) {
        // No-op in the live-only build.
    }

    public void resetParameters() {
        // No-op in the live-only build.
    }
}
