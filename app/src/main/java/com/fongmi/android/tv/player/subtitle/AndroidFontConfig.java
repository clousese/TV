package com.fongmi.android.tv.player.subtitle;

import androidx.annotation.Nullable;

import java.io.File;

// LIVE-ONLY: the original prepare() generated a fontconfig file with the
// author's private LibassFontConfig (androidx.media3.exoplayer.libass),
// absent from public Media3 1.11. Live does not use libass, so no
// libass fontconfig file is generated anymore: prepare() is kept as a
// no-op returning null so MpvUtil.buildConfig() needs no change, and MPV
// subtitle fonts fall back to the default font directories already set in
// MpvPlayerConfig.
public final class AndroidFontConfig {

    @Nullable
    public static synchronized File prepare() {
        return null;
    }
}
