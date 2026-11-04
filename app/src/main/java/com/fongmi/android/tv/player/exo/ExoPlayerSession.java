package com.fongmi.android.tv.player.exo;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.exoplayer.DecoderMode;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager;
import androidx.media3.exoplayer.source.preload.PreloadException;
import androidx.media3.exoplayer.source.preload.PreloadManagerListener;
import androidx.media3.exoplayer.trackselection.TrackSelector;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.setting.DecodeSetting;
import com.fongmi.android.tv.setting.PlayerSetting;

final class ExoPlayerSession {

    private static final String TAG = ExoPlayerSession.class.getSimpleName();
    private static final int MAX_PRELOAD_BUFFER_BYTES = 64 * 1024 * 1024;
    private static final long PRELOAD_DURATION_MS = 10_000;

    private final DefaultPreloadManager preloadManager;
    private final ExoPlayer player;

    @Nullable
    private PreloadRequest preloadRequest;
    private DecoderMode audioMode;
    private DecoderMode videoMode;

    ExoPlayerSession(AudioProcessor audioProcessor) {
        // LIVE-ONLY: the author's private DecoderManager, LibassPlaybackSession,
        // LibassSubtitleController, SubtitleTranscriptSession, SecondaryTextOutput
        // and SecondaryTextTrackSelector are absent from public Media3 1.11 and
        // are all live-unnecessary (libass advanced subtitles, subtitle
        // transcripts, decoder management). The session is rebuilt on the stock
        // APIs: a stock DecodeTrackSelector (see ExoUtil), the stock
        // DefaultPreloadManager and the stock ExoPlayer. Decoder modes are
        // stored per session only (the stock player picks decoders itself);
        // libass is never enabled, so hasLibassSettingChanged() is always false.
        this.audioMode = DecoderMode.fromCommonDecoderMode(DecodeSetting.getDecoderMode(PlayerSetting.ENGINE_EXO, C.TRACK_TYPE_AUDIO));
        this.videoMode = DecoderMode.fromCommonDecoderMode(DecodeSetting.getDecoderMode(PlayerSetting.ENGINE_EXO, C.TRACK_TYPE_VIDEO));
        TrackSelector.Factory trackSelectorFactory = context -> ExoUtil.buildTrackSelector();
        DefaultPreloadManager.Builder builder = createPreloadManagerBuilder(audioProcessor, trackSelectorFactory);
        this.preloadManager = builder.build();
        this.preloadManager.addListener(new PreloadListener());
        this.player = ExoUtil.buildPlayer(builder);
    }

    ExoPlayer player() {
        return player;
    }

    boolean hasLibassSettingChanged() {
        // LIVE-ONLY: libass is stripped; the setting can no longer change playback.
        return false;
    }

    @Nullable
    DecoderMode decoderMode(@C.TrackType int trackType) {
        return switch (trackType) {
            case C.TRACK_TYPE_AUDIO -> audioMode;
            case C.TRACK_TYPE_VIDEO -> videoMode;
            default -> null;
        };
    }

    void setDecoderMode(@C.TrackType int trackType, DecoderMode mode) {
        if (trackType != C.TRACK_TYPE_AUDIO && trackType != C.TRACK_TYPE_VIDEO) return;
        clearPreload();
        if (trackType == C.TRACK_TYPE_AUDIO) audioMode = mode;
        else videoMode = mode;
        // LIVE-ONLY: no DecoderManager in public Media3 1.11; the stored mode
        // is kept for the UI (DecoderSettingPanel) only and does not switch
        // the stock player's decoder at runtime.
    }

    void preload(MediaItem mediaItem, long startPositionMs, Runnable onReady, Runnable onError) {
        PreloadRequest request = new PreloadRequest(mediaItem, Math.max(0, startPositionMs), onReady, onError);
        clearPreload();
        preloadRequest = request;
        preloadManager.add(request.mediaItem(), 0);
        preloadManager.invalidate();
    }

    @Nullable
    MediaSource usePreloadedMediaSource(MediaItem mediaItem) {
        PreloadRequest request = preloadRequest;
        if (request != null && mediaItem.equals(request.mediaItem())) return preloadManager.getMediaSource(request.mediaItem());
        clearPreload();
        return null;
    }

    MediaSource createMediaSource(MediaItem mediaItem) {
        MediaSource preloaded = usePreloadedMediaSource(mediaItem);
        return preloaded != null ? preloaded
                : ExoMediaSourceFactory.supplier()
                .get().createMediaSource(mediaItem);
    }

    void clearPreload() {
        PreloadRequest request = preloadRequest;
        if (request == null) return;
        preloadRequest = null;
        preloadManager.remove(request.mediaItem());
    }

    void release() {
        preloadRequest = null;
        preloadManager.release();
        player.release();
    }

    long getPreloadStartPositionMs() {
        return preloadRequest == null ? 0 : preloadRequest.startPositionMs();
    }

    private DefaultPreloadManager.Builder createPreloadManagerBuilder(AudioProcessor audioProcessor, TrackSelector.Factory trackSelectorFactory) {
        return new DefaultPreloadManager.Builder(App.get(), ignored -> DefaultPreloadManager.PreloadStatus.specifiedRangeLoaded(getPreloadStartPositionMs(), PRELOAD_DURATION_MS)).setMediaSourceFactorySupplier(ExoMediaSourceFactory.supplier()).setRenderersFactory(ExoUtil.buildRenderersFactory(audioProcessor)).setTrackSelectorFactory(trackSelectorFactory).setLoadControl(ExoUtil.buildLoadControl(MAX_PRELOAD_BUFFER_BYTES));
    }

    private final class PreloadListener implements PreloadManagerListener {

        @Override
        public void onCompleted(@NonNull MediaItem mediaItem) {
            onPreloadReady(mediaItem);
        }

        @Override
        public void onPreloadPaused(@NonNull MediaItem mediaItem) {
            onPreloadReady(mediaItem);
        }

        private void onPreloadReady(MediaItem mediaItem) {
            PreloadRequest request = preloadRequest;
            if (request == null || !mediaItem.equals(request.mediaItem())) return;
            Log.d(TAG, "Preload ready");
            request.onReady().run();
        }

        @Override
        public void onError(PreloadException exception) {
            PreloadRequest request = preloadRequest;
            if (request == null || !exception.mediaItem.equals(request.mediaItem())) return;
            Log.w(TAG, "Preload failed", exception);
            clearPreload();
            request.onError().run();
        }
    }

    private record PreloadRequest(MediaItem mediaItem, long startPositionMs, Runnable onReady, Runnable onError) {
    }

}
