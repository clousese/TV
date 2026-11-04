package androidx.media3.common;

// LIVE-ONLY STUB: video transform (scale/pan) from the author's private
// Media3 fork, applied to PlayerView by ExoCommand's video-scale commands.
// Public Media3 1.11 PlayerView has no get/setVideoTransform. The video-scale
// command branch in ExoCommand is dropped in the live-only build (see
// ExoCommand), so this class exists only so the type still resolves for any
// remaining reference; MAX_SCALE is kept for the value-range check.
public final class VideoTransform {

    public static final float MAX_SCALE = 4.0f;

    public final float scaleX;
    public final float scaleY;
    public final float panX;
    public final float panY;

    public VideoTransform(float scaleX, float scaleY, float panX, float panY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.panX = panX;
        this.panY = panY;
    }
}
