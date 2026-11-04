package androidx.media3.common.text;

// LIVE-ONLY STUB: subtitle offsets (primary/secondary subtitle delay in ms)
// from the author's private Media3 fork. Public Media3 1.11 has no such
// class. The stub is a plain value type; live subtitle delay is applied
// through the stock Player text-offset path where available, otherwise the
// offsets are simply carried as values.
public final class SubtitleOffsets {

    public static final SubtitleOffsets ZERO = new SubtitleOffsets(0, 0);

    public final long primaryMs;
    public final long secondaryMs;

    public SubtitleOffsets(long primaryMs, long secondaryMs) {
        this.primaryMs = primaryMs;
        this.secondaryMs = secondaryMs;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SubtitleOffsets other)) return false;
        return primaryMs == other.primaryMs && secondaryMs == other.secondaryMs;
    }

    @Override
    public int hashCode() {
        return 31 * Long.hashCode(primaryMs) + Long.hashCode(secondaryMs);
    }

    @Override
    public String toString() {
        return "SubtitleOffsets{primaryMs=" + primaryMs + ", secondaryMs=" + secondaryMs + "}";
    }
}
