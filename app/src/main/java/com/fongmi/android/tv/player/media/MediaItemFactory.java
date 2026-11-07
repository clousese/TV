package com.fongmi.android.tv.player.media;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.util.LocalClearKeyLicense;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.IsoUri;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.bean.Drm;
import com.fongmi.android.tv.bean.EpgData;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.utils.ImgUtil;

import java.util.Map;
import java.util.UUID;

public final class MediaItemFactory {

    public static MediaMetadata buildMetadata(String title, String artist, String artUri) {
        title = TextUtils.isEmpty(title) ? "" : title;
        artist = TextUtils.isEmpty(artist) ? "" : artist;
        String subtitle = title.isEmpty() || title.equals(artist) ? "" : artist;
        return new MediaMetadata.Builder().setTitle(title).setArtist(artist).setSubtitle(subtitle).setArtworkUri(getArtworkUri(artUri)).build();
    }

    public static MediaMetadata buildLiveMetadata(String title, @Nullable EpgData data, String artUri) {
        String details = data == null ? "" : data.format();
        return buildMetadata(title, details, artUri).buildUpon().setStation(title).setDisplayTitle(title).setMediaType(MediaMetadata.MEDIA_TYPE_TV_CHANNEL).build();
    }

    public static Uri getArtworkUri(String artUri) {
        artUri = ImgUtil.cache(artUri);
        return TextUtils.isEmpty(artUri) ? null : Uri.parse(artUri);
    }

    public static String getDisplayTitle(MediaMetadata metadata) {
        if (!TextUtils.isEmpty(metadata.displayTitle)) return metadata.displayTitle.toString();
        if (!TextUtils.isEmpty(metadata.title)) return metadata.title.toString();
        return TextUtils.isEmpty(metadata.artist) ? "" : metadata.artist.toString();
    }

    public static String getDefaultUserAgent() {
        return Util.getUserAgent(App.get(), BuildConfig.APPLICATION_ID);
    }

    public static MediaItem from(PlaySpec spec) {
        Uri uri = spec.getUri();
        if (spec.getIsoEditionIndex() != C.INDEX_UNSET) uri = IsoUri.withEditionIndex(uri, spec.getIsoEditionIndex());
        return new MediaItem.Builder().setUri(uri)
                .setTag(spec.getAuth())
                .setDrmConfiguration(buildDrmConfig(spec.getDrm()))
                .setRequestMetadata(buildRequestMetadata(spec))
                .setMediaMetadata(spec.getMetadata())
                .setAdblock(Setting.isAdblock())
                .setMimeType(spec.getFormat())
                .setImageDurationMs(15000)
                .setMediaId(spec.getKey()).build();
    }

    private static MediaItem.RequestMetadata buildRequestMetadata(PlaySpec spec) {
        return new MediaItem.RequestMetadata.Builder().setMediaUri(spec.getUri()).setExtras(toBundle(spec.getHeaders())).build();
    }

    private static Bundle toBundle(Map<String, String> headers) {
        Bundle bundle = new Bundle();
        if (headers != null) headers.forEach(bundle::putString);
        return bundle;
    }

    private static MediaItem.DrmConfiguration buildDrmConfig(Drm drm) {
        if (drm == null) return null;
        UUID scheme = LocalClearKeyLicense.parse(drm.getKey()).isEmpty() ? drm.getUUID() : C.CLEARKEY_UUID;
        return new MediaItem.DrmConfiguration.Builder(scheme).setMultiSession(!C.CLEARKEY_UUID.equals(scheme)).setForceDefaultLicenseUri(drm.isForceKey()).setLicenseRequestHeaders(drm.getHeader()).setLicenseUri(drm.getKey()).build();
    }
}
