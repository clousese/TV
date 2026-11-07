package com.fongmi.android.tv.playback;

import android.view.View;
import android.widget.TextView;

import androidx.media3.common.C;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.utils.ResUtil;

public final class PlaybackAction {

    public static void setPlaybackMode(PlayerManager player, TextView engine) {
        if (engine != null) engine.setText(getEngineText(player));
    }

    public static void setTracks(PlayerManager player, View audio, View video) {
        setVisible(audio, hasAudioTrack(player));
        setVisible(video, hasVideoTrack(player));
    }

    public static void setMediaOptions(PlayerManager player, View edition, View chapter) {
        setVisible(edition, hasEdition(player));
        setVisible(chapter, hasChapter(player));
    }

    public static void setWebPlaybackMode(boolean active, View... nativeOnlyViews) {
        for (View view : nativeOnlyViews) setVisible(view, !active);
    }

    public static int getEngine(PlayerManager player) {
        if (player == null || player.isReleased()) return PlayerSetting.getEngine();
        return player.getEngine();
    }

    public static String getEngineText(PlayerManager player) {
        return ResUtil.getStringArray(R.array.select_engine)[getEngine(player)];
    }

    private static boolean hasAudioTrack(PlayerManager player) {
        return player != null && (player.haveTrack(C.TRACK_TYPE_AUDIO) || player.getDecoderMode(C.TRACK_TYPE_AUDIO) != null);
    }

    private static boolean hasVideoTrack(PlayerManager player) {
        return player != null && (player.haveTrack(C.TRACK_TYPE_VIDEO) || player.getDecoderMode(C.TRACK_TYPE_VIDEO) != null);
    }

    private static boolean hasEdition(PlayerManager player) {
        return player != null && player.haveEdition();
    }

    private static boolean hasChapter(PlayerManager player) {
        return player != null && player.haveChapter();
    }

    private static void setVisible(View view, boolean visible) {
        if (view != null) view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
