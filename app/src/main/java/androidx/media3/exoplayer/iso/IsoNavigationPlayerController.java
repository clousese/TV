package androidx.media3.exoplayer.iso;

import android.content.Context;

import androidx.media3.common.DiscMenuController;
import androidx.media3.common.Player;
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.source.MediaSource;

// LIVE-ONLY STUB (extra, beyond the run #7 list): ISO navigation player
// controller from the author's private Media3 fork, wrapping an ExoPlayer
// with disc navigation. Public Media3 1.11 has no such class. The stub wraps
// the stock player directly: getPlayer returns it unchanged,
// prepareMediaSource passes the source through, isStarted is always false,
// close is a no-op, and the DiscMenuController methods report unavailable
// (live playback never navigates a disc).
public final class IsoNavigationPlayerController implements DiscMenuController {

    private final Player player;

    public IsoNavigationPlayerController(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public MediaSource prepareMediaSource(Context context, MediaSource source, DataSource.Factory dataSourceFactory) {
        return source;
    }

    public boolean isStarted() {
        return false;
    }

    public void close() {
        // No-op in the live-only build.
    }
}
