package com.fongmi.android.tv.player.mpv;

import androidx.annotation.NonNull;
import androidx.media3.common.C;
import androidx.media3.common.DecoderMode;
import androidx.media3.common.DiscMenuController;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.Tracks;
import androidx.media3.mpvplayer.MpvDecoderMode;
import androidx.media3.mpvplayer.MpvPlayer;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.player.effect.PlayerEffect;
import com.fongmi.android.tv.player.engine.PlayerEngine;
import com.fongmi.android.tv.player.exo.ExoMediaSourceFactory;
import com.fongmi.android.tv.player.media.ConcatenatingUrl;
import com.fongmi.android.tv.player.media.MediaItemFactory;
import com.fongmi.android.tv.player.media.PlaySpec;
import com.fongmi.android.tv.server.LogSession;

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
        return player;
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
    public List<DecoderMode> getSupportedDecoderModes(@C.TrackType int trackType) {
        return trackType == C.TRACK_TYPE_VIDEO ? List.of(DecoderMode.AUTO, DecoderMode.HARDWARE, DecoderMode.FFMPEG) : List.of();
    }

    @Override
    @Nullable
    public DecoderMode getDecoderMode(@C.TrackType int trackType) {
        if (trackType != C.TRACK_TYPE_VIDEO) return null;
        return player.getVideoDecoderMode().toCommonDecoderMode();
    }

    @Override
    public void setDecoderMode(@C.TrackType int trackType, DecoderMode mode) {
        if (!getSupportedDecoderModes(trackType).contains(mode)) return;
        player.setVideoDecoderMode(MpvDecoderMode.fromCommonDecoderMode(mode));
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
}
