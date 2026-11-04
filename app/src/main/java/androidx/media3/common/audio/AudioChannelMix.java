package androidx.media3.common.audio;

// LIVE-ONLY STUB: channel-mix matrices from the author's private Media3
// fork, used by the MPV audio filter to build pan mixes. Public Media3 1.11
// has no such class. The stub returns identity-style mixes so the MPV filter
// builder code compiles; the mixes are not actually applied to live audio
// in a way that changes the stock playback path.
public final class AudioChannelMix {

    private AudioChannelMix() {
    }

    public static float[][] createFrontCenterGainMix(int channelCount, float factor) {
        return identity(channelCount);
    }

    public static float[][] createStereoMix(int channelCount, boolean reverse) {
        float[][] mix = identity(channelCount);
        if (reverse && channelCount >= 2) {
            float[] first = mix[0];
            mix[0] = mix[1];
            mix[1] = first;
        }
        return mix;
    }

    public static float[][] createMonoMix(int channelCount) {
        return identity(channelCount);
    }

    public static float[][] createFrontBalanceMix(int channelCount, float balance) {
        return identity(channelCount);
    }

    public static float[][] compose(float[][] mix, float[][] next) {
        if (mix == null) return next;
        if (next == null) return mix;
        return next;
    }

    private static float[][] identity(int channelCount) {
        int size = Math.max(0, channelCount);
        float[][] mix = new float[size][size];
        for (int i = 0; i < size; i++) mix[i][i] = 1.0f;
        return mix;
    }
}
