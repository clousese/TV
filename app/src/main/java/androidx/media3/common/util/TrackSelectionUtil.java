package androidx.media3.common.util;

import androidx.media3.common.Player;

import java.util.List;

// LIVE-ONLY STUB: track-selection helpers from the author's private Media3
// fork (cycle / select / disable / reset audio tracks by id). Public Media3
// 1.11 has no such class. The stub always reports "not applied", so the
// ExoCommand audio-track commands return UNAVAILABLE instead of changing
// tracks; standard track selection through the track dialog is unaffected.
public final class TrackSelectionUtil {

    private TrackSelectionUtil() {
    }

    public static boolean cycleTrack(Player player, int trackType, boolean forward, boolean includeDisabled) {
        return false;
    }

    public static boolean cycleTrack(Player player, int trackType, List<String> trackIds, boolean forward, boolean includeDisabled) {
        return false;
    }

    public static boolean setTrackTypeDisabled(Player player, int trackType, boolean disabled) {
        return false;
    }

    public static boolean resetTrackSelection(Player player, int trackType) {
        return false;
    }

    public static boolean selectTrack(Player player, int trackType, String trackId) {
        return false;
    }
}
