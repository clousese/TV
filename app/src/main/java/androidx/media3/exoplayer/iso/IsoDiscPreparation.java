package androidx.media3.exoplayer.iso;

import androidx.media3.common.MediaItem;

// LIVE-ONLY STUB: ISO disc detection from the author's private Media3 fork.
// Public Media3 1.11 has no such class. Live streams are never ISO discs,
// so isIso always returns false; callers then take their normal (non-disc)
// path, e.g. end-trim and preload are applied as for regular media.
public final class IsoDiscPreparation {

    private IsoDiscPreparation() {
    }

    public static boolean isIso(MediaItem item) {
        return false;
    }
}
