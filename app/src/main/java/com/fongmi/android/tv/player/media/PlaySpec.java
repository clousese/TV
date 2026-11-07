package com.fongmi.android.tv.player.media;

import android.net.Uri;

import androidx.media3.common.C;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.MimeTypes;

import com.fongmi.android.tv.bean.Drm;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Timeshift;
import com.fongmi.android.tv.setting.Setting;
import com.github.catvod.utils.Auth;
import com.github.catvod.utils.UriUtil;
import com.google.common.net.HttpHeaders;

import java.util.HashMap;
import java.util.Map;

public class PlaySpec {

    private Map<String, String> headers;
    private MediaMetadata metadata;
    private String format;
    private String key;
    private String url;
    private Drm drm;
    private Auth.Context auth;
    private Timeshift timeshift;
    private long endingMs;
    private int isoEditionIndex = C.INDEX_UNSET;

    private PlaySpec(String key, String url, Map<String, String> headers, String format, Drm drm, MediaMetadata metadata) {
        this.key = key;
        this.url = url;
        this.drm = drm;
        this.format = format;
        this.headers = headers;
        this.metadata = metadata;
    }

    public static PlaySpec from(String key, String url, Map<String, String> headers, MediaMetadata metadata) {
        return new PlaySpec(key, url, headers, null, null, metadata);
    }

    public static PlaySpec from(Result result, String key, MediaMetadata metadata) {
        PlaySpec spec = new PlaySpec(key, result.getRealUrl(), result.getHeader(), result.getFormat(), result.getDrm(), metadata);
        spec.auth = result.getAuth();
        spec.timeshift = result.getTimeshift();
        return spec;
    }

    public String getKey() {
        return key;
    }

    public long getEnding() {
        return endingMs;
    }

    public PlaySpec setEnding(long endingMs) {
        this.endingMs = Math.max(0, endingMs);
        return this;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Uri getUri() {
        return UriUtil.uri(auth == null ? url : auth.getUrl(url, headers == null ? Map.of() : headers));
    }

    public Auth.Context getAuth() {
        return auth;
    }

    public Timeshift getTimeshift() {
        return timeshift;
    }

    public int getIsoEditionIndex() {
        return isoEditionIndex;
    }

    public void setIsoEditionIndex(int isoEditionIndex) {
        this.isoEditionIndex = isoEditionIndex;
    }

    public Map<String, String> getHeaders() {
        if (auth == null) return headers;
        Map<String, String> additions = auth.getHeaders(url, headers == null ? Map.of() : headers);
        if (additions.isEmpty()) return headers;
        Map<String, String> prepared = headers == null ? new HashMap<>() : new HashMap<>(headers);
        prepared.putAll(additions);
        return prepared;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public void refresh() {
        headers = new HashMap<>(headers == null ? Map.of() : headers);
        headers.keySet().removeIf(HttpHeaders.CACHE_CONTROL::equalsIgnoreCase);
        headers.put(HttpHeaders.CACHE_CONTROL, "no-cache");
    }

    public String getFormat() {
        if ((format == null || format.isEmpty()) && url != null && url.contains("/proxy/mpd/")) return MimeTypes.APPLICATION_MPD;
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Drm getDrm() {
        return drm;
    }

    public MediaMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(MediaMetadata metadata) {
        this.metadata = metadata;
    }

    public PlaySpec checkUa() {
        if (headers == null) headers = new HashMap<>();
        if (headers.keySet().stream().noneMatch(HttpHeaders.USER_AGENT::equalsIgnoreCase)) headers.put(HttpHeaders.USER_AGENT, Setting.getUa().isEmpty() ? MediaItemFactory.getDefaultUserAgent() : Setting.getUa());
        return this;
    }
}
