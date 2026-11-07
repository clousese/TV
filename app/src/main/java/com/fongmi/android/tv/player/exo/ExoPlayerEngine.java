package com.fongmi.android.tv.player.exo;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.DecoderMode;
import androidx.media3.common.DiscMenuController;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.Tracks;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.analytics.AnalyticsListener;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.iso.IsoDiscPreparation;
import androidx.media3.exoplayer.iso.IsoNavigationPlayerController;
import androidx.media3.exoplayer.source.EndTrimMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.util.EventLogger;
import androidx.media3.ui.PlayerView;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.player.effect.PlayerEffect;
import com.fongmi.android.tv.player.engine.PlayerEngine;
import com.fongmi.android.tv.player.media.MediaItemFactory;
import com.fongmi.android.tv.player.media.PlaySpec;
import com.fongmi.android.tv.server.LogSession;
import com.fongmi.android.tv.setting.DecodeSetting;
import com.google.gson.JsonObject;

import java.util.List;

public class ExoPlayerEngine implements PlayerEngine, AnalyticsListener {

    private final ExoErrorMessageProvider provider;
    private final ExoPlayerSession session;
    private final ExoPlayerEffect effect;
    private final ExoPlayer player;
    private final IsoNavigationPlayerController navigation;
    private final ExoPlaybackLog playbackLog = new ExoPlaybackLog();
    private final Runnable updateLog = this::updateEventLogger;
    private final Runnable logListener = () -> App.post(updateLog);
    private EventLogger eventLogger;
    private PlaySpec spec;
    private EndTrimMediaSource endingSource;

    public ExoPlayerEngine(Player.Listener listener) {
        this.effect = new ExoPlayerEffect();
        this.provider = new ExoErrorMessageProvider();
        this.session = new ExoPlayerSession(effect.getAudioProcessor());
        this.player = session.player();
        this.navigation = new IsoNavigationPlayerController(player);
        this.navigation.getPlayer().addListener(listener);
        this.player.addAnalyticsListener(this);
        this.player.addAnalyticsListener(playbackLog);
        this.effect.setPlayer(player);
        LogSession.get().addListener(logListener);
        updateEventLogger();
    }

    private void updateEventLogger() {
        boolean enabled = LogSession.get().isRunning();
        if (enabled && eventLogger == null) {
            eventLogger = new EventLogger("EventLogger-" + Integer.toHexString(System.identityHashCode(navigation.getPlayer())));
            player.addAnalyticsListener(eventLogger);
        } else if (!enabled && eventLogger != null) {
            player.removeAnalyticsListener(eventLogger);
            eventLogger = null;
        }
    }

    @Override
    public void onAudioTrackInitialized(@NonNull EventTime eventTime, @NonNull AudioSink.AudioTrackConfig audioTrackConfig) {
        effect.applyAudioEffect();
    }

    @Override
    public void onAudioTrackReleased(@NonNull EventTime eventTime, @NonNull AudioSink.AudioTrackConfig audioTrackConfig) {
        effect.applyAudioEffect();
    }

    @Override
    public void onAudioSinkError(@NonNull EventTime eventTime, @NonNull Exception error) {
        if (eventLogger == null) return;
        String message = String.valueOf(error.getMessage());
        int end = message.offsetByCodePoints(0, Math.min(1024, message.codePointCount(0, message.length())));
        AudioAttributes attributes = player.getAudioAttributes();
        Log.e("TV-Playback", "player=" + Integer.toHexString(System.identityHashCode(navigation.getPlayer())) + " event=audioSinkError"
                + " elapsedRealtimeMs=" + eventTime.realtimeMs + " positionMs=" + eventTime.currentPlaybackPositionMs
                + " playerAudioSessionId=" + player.getAudioSessionId() + " tunnelingSettingEnabled=" + DecodeSetting.isTunnelingEnabled()
                + " playerAudioUsage=" + attributes.usage + " playerAudioFlags=" + attributes.flags
                + " error=" + message.substring(0, end) + (end < message.length() ? " [truncated]" : ""));
    }

    @Override
    public void onTracksChanged(@NonNull EventTime eventTime, @NonNull Tracks tracks) {
        effect.applyVideoEffect();
    }

    @Override
    public Type getType() {
        return Type.EXO;
    }

    @Override
    public boolean needsRebuild() {
        return session.hasLibassSettingChanged();
    }

    @Override
    public Player getPlayer() {
        return navigation.getPlayer();
    }

    @Override
    public JsonObject getPlaybackLog() {
        return playbackLog.snapshot();
    }

    @Override
    public DiscMenuController getDiscMenuController() {
        return navigation;
    }

    @Override
    public int getAudioChannelCount() {
        Format format = player.getAudioFormat();
        return format == null ? Format.NO_VALUE : format.channelCount;
    }

    @Override
    public PlayerEffect getEffect() {
        return effect;
    }

    @Override
    public void release() {
        LogSession.get().removeListener(logListener);
        App.removeCallbacks(updateLog);
        if (eventLogger != null) {
            Log.i("TV-Playback", "player=" + Integer.toHexString(System.identityHashCode(navigation.getPlayer())) + " event=engineRelease");
            player.removeAnalyticsListener(eventLogger);
            eventLogger = null;
        }
        player.removeAnalyticsListener(this);
        player.removeAnalyticsListener(playbackLog);
        playbackLog.clear();
        effect.release();
        navigation.close();
        session.release();
    }

    @Override
    public List<DecoderMode> getSupportedDecoderModes(@C.TrackType int trackType) {
        return getDecoderMode(trackType) == null ? List.of() : List.of(DecoderMode.values());
    }

    @Override
    @Nullable
    public DecoderMode getDecoderMode(@C.TrackType int trackType) {
        androidx.media3.exoplayer.DecoderMode mode = session.decoderMode(trackType);
        return mode == null ? null : mode.toCommonDecoderMode();
    }

    @Override
    public void setDecoderMode(@C.TrackType int trackType, DecoderMode mode) {
        session.setDecoderMode(trackType, androidx.media3.exoplayer.DecoderMode.fromCommonDecoderMode(mode));
    }

    @Override
    public boolean setMediaItem(PlaySpec spec, long startPositionMs) {
        this.spec = spec;
        MediaItem item = MediaItemFactory.from(this.spec);
        MediaSource source = navigation.prepareMediaSource(App.get(),
                session.createMediaSource(item), ExoMediaSourceFactory.createDataSourceFactory(this.spec.getHeaders(), this.spec.getAuth()));
        if (source == null) return false;
        endingSource = IsoDiscPreparation.isIso(item) ? null : new EndTrimMediaSource(source, this.spec.getEnding());
        if (endingSource != null) source = endingSource;
        effect.clearAudioEffect();
        player.setMediaSource(source, startPositionMs);
        return true;
    }

    @Override
    public void setEnding(long endingMs) {
        if (spec != null) spec.setEnding(endingMs);
        if (endingSource != null) endingSource.setEndTrimDurationMs(endingMs);
    }

    @Override
    public boolean isEndingManaged() {
        return endingSource != null && endingSource.isEndTrimActive();
    }

    @Override
    public long getOriginalDurationMs() {
        return endingSource == null ? C.TIME_UNSET : endingSource.getOriginalDurationMs();
    }

    @Override
    public void bindPlayerView(PlayerView playerView) {
        playerView.setBackendPlayer(player);
    }

    @Override
    public void stop() {
        if (navigation.isStarted()) navigation.close();
        player.stop();
    }

    @Override
    public String getErrorMessage(PlaybackException e) {
        return provider.get(e);
    }

    @Override
    public ErrorAction handleError(PlaybackException e) {
        return switch (e.errorCode) {
            case PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> seekToDefaultPosition();
            case PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> ErrorAction.REFRESH;
            default -> ErrorAction.FATAL;
        };
    }

    private ErrorAction seekToDefaultPosition() {
        player.seekToDefaultPosition();
        player.prepare();
        return ErrorAction.RECOVERED;
    }
}
