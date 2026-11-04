package androidx.media3.common.audio;

// LIVE-ONLY STUB: audio channel mode from the author's private Media3 fork
// (stereo/mono/reverse/auto channel mixing). Public Media3 1.11 has no such
// class. The stub preserves the setting values and the resolve/clamp logic
// used by the audio setting UI; actual channel mixing is not applied by the
// stub (the stock player plays the stream as-is).
public final class AudioChannelMode {

    public static final int AUTO = 0;
    public static final int STEREO = 1;
    public static final int MONO = 2;
    public static final int REVERSE = 3;

    private AudioChannelMode() {
    }

    public static int clamp(int mode) {
        return switch (mode) {
            case STEREO, MONO, REVERSE -> mode;
            default -> AUTO;
        };
    }

    public static int resolve(int mode, int channelCount) {
        int clamped = clamp(mode);
        if (clamped == AUTO) return AUTO;
        if ((clamped == STEREO || clamped == REVERSE) && channelCount < 2) return AUTO;
        return clamped;
    }

    public static boolean isAuto(int mode) {
        return clamp(mode) == AUTO;
    }

    public static boolean isAvailable(int mode, int channelCount) {
        int resolved = resolve(mode, channelCount);
        return resolved != AUTO || channelCount >= 1;
    }

    public static boolean isAvailable(int channelCount) {
        return channelCount >= 2;
    }

    public static boolean canBalance(int mode) {
        int clamped = clamp(mode);
        return clamped == AUTO || clamped == STEREO || clamped == REVERSE;
    }
}
