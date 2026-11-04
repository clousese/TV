package com.fongmi.android.tv.player.effect.video;

import androidx.media3.exoplayer.ExoPlayer;

// LIVE-ONLY: the original controller applied the author's private
// ColorToneAdjustEffect, DetailAdjustEffect and VideoAdjustment
// (androidx.media3.effect), none of which exist in public Media3 1.11
// (the 1.11 effect tree has neither VideoAdjustment nor DetailAdjustEffect;
// ColorToneAdjustEffect only exists as a first-batch stub whose
// setAdjustment is already a no-op). Video adjustment is a polish feature,
// not playback: for live, apply()/clear() are no-ops and video is rendered
// unadjusted. The MPV path is unaffected (it uses the stock
// MpvVideoEqualizer via MpvVideoEffectController). VideoEffectProfile's
// toAdjustment() was deleted together with this branch because it was
// only called from here.
public final class ExoVideoEffectController {

    public ExoVideoEffectController() {
    }

    public void apply(ExoPlayer player, VideoEffectProfile profile) {
        // LIVE-ONLY: no-op (see class comment). The profile argument is
        // kept so ExoPlayerEffect needs no change.
    }

    public void clear(ExoPlayer player) {
        // LIVE-ONLY: no-op; no effects were ever set on the stock player.
    }
}
