package androidx.media3.ui.danmaku;

import androidx.media3.ui.PlayerView;

import java.util.List;

import okhttp3.OkHttpClient;

// LIVE-ONLY STUB: danmaku player-view controller from the author's private
// Media3 fork (danmaku rendering bound to a PlayerView). Public Media3 1.11
// has Danmaku/DanmakuConfig in ui_danmaku but no DanmakuPlayerViewController.
// Live sources carry no danmaku (step 4 decision), so every method is a
// no-op and getItems always returns an empty list.
public final class DanmakuPlayerViewController {

    public List<Danmaku> getItems() {
        return List.of();
    }

    public void bind(PlayerView playerView) {
        // No-op in the live-only build.
    }

    public void setConfig(DanmakuConfig config) {
        // No-op in the live-only build.
    }

    public void setOkHttpClient(OkHttpClient client) {
        // No-op in the live-only build.
    }

    public void setEnabled(boolean enabled) {
        // No-op in the live-only build.
    }

    public void setDataSource(Object dataSource) {
        // No-op in the live-only build.
    }

    public void close() {
        // No-op in the live-only build.
    }
}
