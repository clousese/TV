package androidx.media3.common.text;

import java.util.List;

// LIVE-ONLY STUB: subtitle transcript content from the author's private
// Media3 fork (full subtitle text for the playback-content panel). Public
// Media3 1.11 has no such class. The live-only build never produces a
// transcript, so only the UNSUPPORTED / LOADING sentinels are ever returned;
// the panel shows its "unsupported" empty message in that case.
public final class SubtitleContent {

    public enum State {
        LOADING,
        ERROR,
        UNSUPPORTED,
        PARTIAL,
        TRUNCATED,
        COMPLETE
    }

    public static final class Entry {
        public final boolean image;
        public final String text;
        public final long startTimeUs;
        public final long endTimeUs;

        public Entry(boolean image, String text, long startTimeUs, long endTimeUs) {
            this.image = image;
            this.text = text;
            this.startTimeUs = startTimeUs;
            this.endTimeUs = endTimeUs;
        }
    }

    public static final SubtitleContent UNSUPPORTED = new SubtitleContent(State.UNSUPPORTED, List.of());
    public static final SubtitleContent LOADING = new SubtitleContent(State.LOADING, List.of());

    public final State state;
    public final List<Entry> entries;

    public SubtitleContent(State state, List<Entry> entries) {
        this.state = state;
        this.entries = entries == null ? List.of() : List.copyOf(entries);
    }
}
