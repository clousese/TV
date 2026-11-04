package com.fongmi.android.tv.player.exo;

import androidx.annotation.Nullable;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.text.SubtitleSelectionState;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.SubtitleView;

import com.fongmi.android.tv.setting.SubtitleSetting;

// LIVE-ONLY: the author's private LibassPlaybackSession,
// LibassSubtitleController and LibassPlayerViewController (ui.libass) are
// absent from public Media3 1.11. Live does not need libass advanced
// subtitles or a secondary subtitle track: the stock PlayerView renders the
// single selected subtitle track, and the secondary selection is always
// empty. The constructor signature is kept so ExoPlayerEngine needs no
// change; the session argument is no longer used.
final class ExoSubtitleController {

    @Nullable
    private PlayerView boundPlayerView;

    ExoSubtitleController(ExoPlayerSession session, androidx.media3.common.Player player) {
        // No libass session/controller in the live-only build.
    }

    void release() {
        boundPlayerView = null;
    }

    void bindPlayerView(PlayerView playerView) {
        boundPlayerView = playerView;
    }

    void applySubtitleStyle() {
        // LIVE-ONLY: apply the user's subtitle style to the stock subtitle
        // view only. libass bottom-position, font-scale and secondary-track
        // styling have no stock equivalent and are dropped.
        PlayerView playerView = boundPlayerView;
        if (playerView == null) return;
        SubtitleView subtitleView = playerView.getSubtitleView();
        if (subtitleView != null) SubtitleSetting.applyStyle(subtitleView);
    }

    SubtitleSelectionState getSubtitleSelectionState() {
        // LIVE-ONLY: no libass controller, so there is never a secondary
        // selection; the stock single-track selection is reported as EMPTY.
        return SubtitleSelectionState.EMPTY;
    }

    void setSecondarySubtitleSelection(@Nullable TrackSelectionOverride selection) {
        // LIVE-ONLY: no secondary subtitle track in the stock player; no-op.
    }

    void restoreSecondarySubtitleSelection(@Nullable TrackSelectionOverride selection) {
        // LIVE-ONLY: no secondary subtitle track in the stock player; no-op.
    }

    void prepareForNewMedia() {
        // LIVE-ONLY: nothing to reset without a libass controller.
    }
}
