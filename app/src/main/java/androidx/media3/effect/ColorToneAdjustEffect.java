package androidx.media3.effect;

import androidx.media3.common.Effect;

// LIVE-ONLY STUB: color-tone adjustment effect from the author's private
// Media3 fork. Public Media3 1.11 has no such class. The stub implements
// Effect so ExoVideoEffectController's effect list still compiles, but
// setAdjustment is a no-op: live video is rendered without the author's
// color-tone adjustment.
public final class ColorToneAdjustEffect implements Effect {

    public void setAdjustment(VideoAdjustment adjustment) {
        // No-op in the live-only build.
    }
}
