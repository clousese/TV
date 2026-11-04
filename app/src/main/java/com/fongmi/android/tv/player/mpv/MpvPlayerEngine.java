package com.fongmi.android.tv.player.mpv;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.DecoderMode;
import androidx.media3.common.DiscMenuController;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.common.text.SubtitleContent;
import androidx.media3.common.text.SubtitleOffsets;
import androidx.media3.common.text.SubtitleSelectionState;
import androidx.media3.mpvplayer.MpvPlayer;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.player.effect.PlayerEffect;
import com.fongmi.android.tv.player.engine.PlayerEngine;
import com.fongmi.android.tv.player.exo.ExoMediaSourceFactory;
import com.fongmi.android.tv.player.media.ConcatenatingUrl;
import com.fongmi.android.tv.player.media.MediaItemFactory;
import com.fongmi.android.tv.player.media.PlaySpec;
import com.fongmi.android.tv.server.LogSession;
import com.fongmi.android.tv.setting.SubtitleSetting;

import org.json.JSONException;

import java.util.List;

public class MpvPlayerEngine implements PlayerEngine, Player.Listener {

    private final MpvErrorMessageProvider provider;
    private final MpvPlayerEffect effect;
    private final MpvScriptSession scripts;
    private final MpvPlayer player;
    private final Runnable updateLog = this::updateLogLevel;
    private final Runnable logListener = () -> App.post(updateLog);

    public MpvPlayerEngine(Player.Listener listener) {
        List<MpvScripts.Item> scriptItems = MpvUtil.readScripts();
        this.player = MpvUtil.buildPlayer(listener);
        this.scripts = new MpvScriptSession(player, scriptItems);
        this.provider = new MpvErrorMessageProvider();
        this.effect = new MpvPlayerEffect(player);
        this.player.setAudioOutputListener(effect::applyAudioEffect);
        this.player.addListener(this);
        applySecondarySubtitleMode(SubtitleSetting.getSecondaryMode());
        LogSession.get().addListener(logListener);
        updateLogLevel();
    }

    private void updateLogLevel() {
        player.setVerboseLoggingEnabled(LogSession.get().isRunning());
    }

    public static boolean isAvailable() {
        return MpvUtil.isAvailable();
    }

    @Override
    public Type getType() {
        return Type.MPV;
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    @Override
    public DiscMenuController getDiscMenuController() {
        return null; // LIVE-ONLY: stock MpvPlayer does not implement DiscMenuController; live never has a disc menu
    }

    @Override
    public int getAudioChannelCount() {
        return player.getAudioChannelCount();
    }

    @Override
    public PlayerEffect getEffect() {
        return effect;
    }

    @Override
    public void release() {
        LogSession.get().removeListener(logListener);
        App.removeCallbacks(updateLog);
        scripts.release();
        player.removeListener(this);
        player.setAudioOutputListener(null);
        player.release();
    }

    @Override
    public void applySubtitleStyle() {
        MpvUtil.applySubtitleStyle(player);
    }

    public boolean runScript(MpvScripts.Item item) {
        return scripts.run(item);
    }

    public MpvScriptSession.Status getScriptStatus(String id) {
        return scripts.status(id);
    }

    public List<String> getScriptBindings() throws JSONException {
        return scripts.bindings();
    }

    public void reloadScripts(boolean reloadStartupScripts, String reloadButtonId) {
        scripts.reload(MpvUtil.readScripts(), reloadStartupScripts, reloadButtonId);
    }

    @Override
    public SubtitleSelectionState getSubtitleSelectionState() {
        return SubtitleSelectionState.EMPTY; // LIVE-ONLY: stock MpvPlayer has no subtitle-selection state API
    }

    @Override
    public SubtitleContent getSubtitleContent(TrackSelectionOverride selection) {
        return SubtitleContent.UNSUPPORTED; // LIVE-ONLY: no transcript content on the stock player
    }

    @Override
    public SubtitleOffsets getSubtitleOffsets() {
        return SubtitleOffsets.ZERO; // LIVE-ONLY: stock MpvPlayer has no subtitle-offset API
    }

    @Override
    public void setSubtitleOffsets(SubtitleOffsets offsets) {
        // LIVE-ONLY: stock MpvPlayer has no subtitle-offset API; no-op.
    }

    @Override
    public boolean supportsSubtitleOffsets() {
        return false; // LIVE-ONLY: no subtitle-offset API on the stock player
    }

    @Override
    public void setSubtitleContentEnabled(boolean enabled) {
        // LIVE-ONLY: stock MpvPlayer has no subtitle-content toggle; no-op.
    }

    @Override
    public boolean supportsSubtitleTranscript() {
        return false; // LIVE-ONLY: no transcript loader in public Media3 1.11
    }

    @Override
    public boolean canRetrySubtitleContent(Format format) {
        return false; // LIVE-ONLY: no transcript content to retry
    }

    @Override
    public void retrySubtitleContent(Format format) {
        // LIVE-ONLY: no transcript content to retry; no-op.
    }

    @Override
    public void setSecondarySubtitleSelection(@Nullable TrackSelectionOverride selection) {
        int mode = SubtitleSetting.getSecondaryMode();
        applySecondarySubtitleMode(mode);
        // LIVE-ONLY: stock MpvPlayer has no secondary-text track override API; dropped.
    }

    @Override
    public void restoreSecondarySubtitleSelection(@Nullable TrackSelectionOverride selection) {
        // LIVE-ONLY: stock MpvPlayer has no secondary-text track override API; no-op.
    }

    @Override
    public void prepareForNewMedia() {
        applySecondarySubtitleMode(SubtitleSetting.getSecondaryMode());
    }

    @Override
    public boolean addSubtitle(Sub sub) {
        if (sub == null || sub.isEmpty() || player.getCurrentMediaItem() == null) return false;
        if (player.getPlaybackState() == Player.STATE_IDLE || player.getPlaybackState() == Player.STATE_ENDED) return false;
        return player.addSubtitle(MediaItemFactory.buildSubConfig(sub));
    }

    @Override
    public List<DecoderMode> getSupportedDecoderModes(@C.TrackType int trackType) {
        return trackType == C.TRACK_TYPE_VIDEO ? List.of(DecoderMode.AUTO, DecoderMode.HARDWARE, DecoderMode.FFMPEG) : List.of();
    }

    @Override
    @Nullable
    public DecoderMode getDecoderMode(@C.TrackType int trackType) {
        if (trackType != C.TRACK_TYPE_VIDEO) return null;
        return DecoderMode.AUTO; // LIVE-ONLY: stock MpvPlayer has no video-decoder-mode API
    }

    @Override
    public void setDecoderMode(@C.TrackType int trackType, DecoderMode mode) {
        // LIVE-ONLY: stock MpvPlayer has no video-decoder-mode API; no-op.
    }

    @Override
    public void onTracksChanged(@NonNull Tracks tracks) {
        effect.applyVideoEffect();
    }

    @Override
    public boolean setMediaItem(PlaySpec spec, long startPositionMs) {
        effect.applyVideoEffect();
        effect.clearAudioEffect();
        player.setDiscDataSourceFactory(ExoMediaSourceFactory.createDataSourceFactory(spec.getHeaders(), spec.getAuth()));
        MediaItem item = MediaItemFactory.from(spec);
        String url = item.localConfiguration.uri.toString();
        if (ConcatenatingUrl.matches(url)) item = item.buildUpon().setUri(ConcatenatingUrl.toEdl(url)).build();
        player.setMediaItem(item, startPositionMs);
        return true;
    }

    @Override
    public void stop() {
        player.stop();
    }

    @Override
    public String getErrorMessage(PlaybackException e) {
        return provider.get(e);
    }

    @Override
    public ErrorAction handleError(PlaybackException e) {
        return ErrorAction.FATAL;
    }

    private void applySecondarySubtitleMode(int mode) {
        if (mode == SubtitleSetting.SECONDARY_MODE_DEFAULT) player.resetSecondaryTextTrackSelection();
        else player.setSecondaryTextTrackAutoSelectionEnabled(mode == SubtitleSetting.SECONDARY_MODE_AUTO);
    }
}
