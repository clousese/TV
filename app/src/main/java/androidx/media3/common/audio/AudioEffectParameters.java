package androidx.media3.common.audio;

// LIVE-ONLY STUB: audio effect parameters from the author's private Media3
// fork (equalizer bands, stability, boost, preamp, center gain, balance,
// channel mode, loudness). Public Media3 1.11 has no such class. The stub
// carries the values built by AudioEffectConfig so the setting code compiles
// and the effect controllers can read them; the stub effect controllers
// themselves apply nothing to the stock player.
public final class AudioEffectParameters {

    public static final AudioEffectParameters DEFAULT = new Builder().build();

    private final int[] equalizerFrequencies;
    private final short[] equalizerLevels;
    private final float stability;
    private final int boostMillibels;
    private final int preampMillibels;
    private final int centerGainMillibels;
    private final float balance;
    private final int channelMode;
    private final boolean loudnessEnabled;

    private AudioEffectParameters(Builder builder) {
        this.equalizerFrequencies = builder.equalizerFrequencies == null ? new int[0] : builder.equalizerFrequencies.clone();
        this.equalizerLevels = builder.equalizerLevels == null ? new short[0] : builder.equalizerLevels.clone();
        this.stability = builder.stability;
        this.boostMillibels = builder.boostMillibels;
        this.preampMillibels = builder.preampMillibels;
        this.centerGainMillibels = builder.centerGainMillibels;
        this.balance = builder.balance;
        this.channelMode = builder.channelMode;
        this.loudnessEnabled = builder.loudnessEnabled;
    }

    public static boolean isCenterGainAvailable(int channelCount) {
        return channelCount >= 3;
    }

    public boolean hasBands() {
        return equalizerLevels.length > 0;
    }

    public boolean hasEffect() {
        return hasBands() || loudnessEnabled || stability != 0 || boostMillibels != 0
                || preampMillibels != 0 || centerGainMillibels != 0 || balance != 0;
    }

    public int getChannelMode() {
        return channelMode;
    }

    public boolean isLoudnessEnabled() {
        return loudnessEnabled;
    }

    public short[] getEqualizerLevelsMillibels() {
        return equalizerLevels.clone();
    }

    public boolean shouldLimitOutput(int channelCount) {
        return false;
    }

    public boolean hasCenterGain(int channelCount) {
        return isCenterGainAvailable(channelCount) && centerGainMillibels != 0;
    }

    public float getCenterGainFactor() {
        return centerGainMillibels / 100.0f;
    }

    public int getBoostMillibels() {
        return boostMillibels;
    }

    public int getPreampMillibels() {
        return preampMillibels;
    }

    public float getStability() {
        return stability;
    }

    public static final class Builder {
        private int[] equalizerFrequencies = new int[0];
        private short[] equalizerLevels = new short[0];
        private float stability;
        private int boostMillibels;
        private int preampMillibels;
        private int centerGainMillibels;
        private float balance;
        private int channelMode = AudioChannelMode.AUTO;
        private boolean loudnessEnabled;

        public Builder setEqualizerBands(int[] frequencies, short[] levels) {
            this.equalizerFrequencies = frequencies == null ? new int[0] : frequencies.clone();
            this.equalizerLevels = levels == null ? new short[0] : levels.clone();
            return this;
        }

        public Builder setStability(float stability) {
            this.stability = stability;
            return this;
        }

        public Builder setBoostMillibels(int boostMillibels) {
            this.boostMillibels = boostMillibels;
            return this;
        }

        public Builder setPreampMillibels(int preampMillibels) {
            this.preampMillibels = preampMillibels;
            return this;
        }

        public Builder setCenterGainMillibels(int centerGainMillibels) {
            this.centerGainMillibels = centerGainMillibels;
            return this;
        }

        public Builder setBalance(float balance) {
            this.balance = balance;
            return this;
        }

        public Builder setChannelMode(int channelMode) {
            this.channelMode = channelMode;
            return this;
        }

        public Builder setLoudnessEnabled(boolean loudnessEnabled) {
            this.loudnessEnabled = loudnessEnabled;
            return this;
        }

        public AudioEffectParameters build() {
            return new AudioEffectParameters(this);
        }
    }
}
