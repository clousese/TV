package androidx.media3.common;

// LIVE-ONLY STUB: disc-menu state from the author's private Media3 fork.
// Public Media3 1.11 has no such class. Live playback never has a disc menu,
// so EMPTY (idle, unavailable, no navigation playback) is the only state
// that can occur in the live-only build.
public final class DiscMenuState {

    public static final int AVAILABILITY_UNAVAILABLE = 0;
    public static final int AVAILABILITY_AVAILABLE = 1;

    public static final int PHASE_IDLE = 0;
    public static final int PHASE_OPENING = 1;
    public static final int PHASE_ACTIVE = 2;

    public static final DiscMenuState EMPTY = new DiscMenuState(AVAILABILITY_UNAVAILABLE, PHASE_IDLE, false);

    public final int availability;
    public final int phase;
    public final boolean navigationPlaybackOngoing;

    public DiscMenuState(int availability, int phase, boolean navigationPlaybackOngoing) {
        this.availability = availability;
        this.phase = phase;
        this.navigationPlaybackOngoing = navigationPlaybackOngoing;
    }
}
