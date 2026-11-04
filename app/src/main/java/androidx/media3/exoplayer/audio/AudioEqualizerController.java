package androidx.media3.exoplayer.audio;

import android.content.Context;

import androidx.media3.common.audio.AudioEffectParameters;
import androidx.media3.exoplayer.ExoPlayer;

// LIVE-ONLY STUB: hardware equalizer controller from the author's private
// Media3 fork. Public Media3 1.11 has no such class. The stub is a no-op:
// ExoAudioEffectController keeps its API, but no equalizer is applied to
// the stock player.
public final class AudioEqualizerController {

    public AudioEqualizerController(Context context) {
        // No-op in the live-only build.
    }

    public boolean apply(ExoPlayer player, AudioEffectParameters config) {
        return true;
    }

    public void release() {
        // No-op in the live-only build.
    }
}
