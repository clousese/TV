package androidx.media3.common.text;

import androidx.media3.common.TrackSelectionOverride;

// LIVE-ONLY STUB: dual-subtitle selection state from the author's private
// Media3 fork (primary + active secondary subtitle track). Public Media3
// 1.11 has no such class. In the live-only build the stock single subtitle
// track is used, so the secondary selection is always null.
public final class SubtitleSelectionState {

    public static final SubtitleSelectionState EMPTY = new SubtitleSelectionState(null, null);

    public final TrackSelectionOverride primarySelection;
    public final TrackSelectionOverride activeSecondarySelection;

    public SubtitleSelectionState(TrackSelectionOverride primarySelection, TrackSelectionOverride activeSecondarySelection) {
        this.primarySelection = primarySelection;
        this.activeSecondarySelection = activeSecondarySelection;
    }

    public boolean isPrimary(TrackSelectionOverride selection) {
        return selection != null && selection.equals(primarySelection);
    }

    public boolean isActiveSecondary(TrackSelectionOverride selection) {
        return selection != null && selection.equals(activeSecondarySelection);
    }
}
