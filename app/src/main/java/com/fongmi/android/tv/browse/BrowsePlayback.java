package com.fongmi.android.tv.browse;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;

import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.Result;

public final class BrowsePlayback {

    private final String mediaId;
    private final Result result;
    private final String channelName;

    BrowsePlayback(String mediaId, Result result, @Nullable String channelName) {
        this.mediaId = mediaId;
        this.result = result;
        this.channelName = channelName;
    }

    @Nullable
    public static BrowsePlayback from(MediaItem item) {
        return item.localConfiguration != null && item.localConfiguration.tag instanceof BrowsePlayback playback ? playback : null;
    }

    public String getMediaId() {
        return mediaId;
    }

    public Result getResult() {
        return result;
    }

    public long getResumePositionMs() {
        // Live-only: no resume/bookmark position.
        return C.TIME_UNSET;
    }

    public boolean matches(String mediaId) {
        return this.mediaId.equals(mediaId);
    }

    @Nullable
    public Boolean isKept() {
        return channelName == null ? null : Keep.exist(channelName);
    }

    public boolean toggleKeep() {
        if (channelName == null) return false;
        toggleLiveKeep();
        return true;
    }

    private void toggleLiveKeep() {
        if (Keep.exist(channelName)) {
            Keep.delete(channelName);
        } else {
            Keep keep = new Keep();
            keep.setKey(channelName);
            keep.setType(1);
            keep.save();
        }
    }
}
