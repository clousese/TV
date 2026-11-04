package com.fongmi.android.tv.browse;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;

import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.Result;

// LIVE-ONLY: the VOD history/keep branches are removed. A live item has
// history == null and channelName != null, so resume/keep logic is live-only.
// VOD resume, VOD keep and VodHistoryPolicy are gone with playback/vod/.
public final class BrowsePlayback {

    private final String mediaId;
    private final Result result;
    private final History history;
    private final String channelName;
    private final long resumePositionMs;

    BrowsePlayback(String mediaId, Result result, @Nullable History history, @Nullable String channelName) {
        this.mediaId = mediaId;
        this.result = result;
        this.history = history;
        this.channelName = channelName;
        this.resumePositionMs = C.TIME_UNSET;
    }

    @Nullable
    public static BrowsePlayback from(MediaItem item) {
        return item.localConfiguration != null && item.localConfiguration.tag instanceof BrowsePlayback playback ? playback : null;
    }

    public static MediaItem withBookmark(MediaItem item, @Nullable History bookmark) {
        return item;
    }

    @Nullable
    public History copyHistory() {
        return null;
    }

    public String getMediaId() {
        return mediaId;
    }

    public Result getResult() {
        return result;
    }

    public long getResumePositionMs() {
        return resumePositionMs;
    }

    public boolean matches(String mediaId) {
        return this.mediaId.equals(mediaId);
    }

    @Nullable
    public String getFolderId() {
        return null;
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

    public void activate(long startPositionMs) {
    }

    public boolean saveProgress(String mediaId, long position, long duration) {
        return false;
    }
}
