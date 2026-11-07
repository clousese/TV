package com.fongmi.android.tv.playback;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.fragment.app.FragmentActivity;
import androidx.media3.common.C;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.PermissionUtil;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.utils.UriUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExternalPlayback {

    public static final String FORWARD_RESULT = "com.fongmi.android.tv.external.FORWARD_RESULT";
    public static final int REQUEST_CODE = 1002;
    private static final String RETURN_RESULT = "com.fongmi.android.tv.external.RETURN_RESULT";
    private static final String HEADERS = "com.fongmi.android.tv.external.HEADERS";
    private static final String SUBTITLE = "com.fongmi.android.tv.external.SUBTITLE";
    private static final String POSITION = "com.fongmi.android.tv.external.POSITION";
    private static final Pattern URL = Pattern.compile("(https?|push|video|webview):\\S+", Pattern.CASE_INSENSITIVE);

    private ExternalPlayback() {
    }

    public static void open(FragmentActivity activity, Intent source, Consumer<String> onLive) {
        Uri uri = getSourceUri(source);
        if (uri == null) {
            openSharedText(activity, source, onLive);
            return;
        }
        if (!FileChooser.isFileSource(uri)) {
            openText(activity, uri.toString(), source, onLive);
            return;
        }
        openFile(activity, uri, source, onLive);
    }

    private static void openSharedText(FragmentActivity activity, Intent source, Consumer<String> onLive) {
        CharSequence text = source.getCharSequenceExtra(Intent.EXTRA_TEXT);
        if (TextUtils.isEmpty(text) && source.getClipData() != null && source.getClipData().getItemCount() > 0) text = source.getClipData().getItemAt(0).getText();
        if (!TextUtils.isEmpty(text)) {
            openText(activity, text.toString(), source, onLive);
        } else {
            Notify.show(R.string.error_play_url);
            finishForwarded(activity, source);
        }
    }

    private static void openFile(FragmentActivity activity, Uri uri, Intent source, Consumer<String> onLive) {
        if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(uri.getScheme())) {
            PermissionUtil.requestFile(activity, granted -> {
                if (granted) startFile(activity, uri, source, onLive);
                else finishForwarded(activity, source);
            });
        } else {
            startFile(activity, uri, source, onLive);
        }
    }

    private static void startFile(FragmentActivity activity, Uri uri, Intent source, Consumer<String> onLive) {
        if (FileChooser.isLiveSource(new Intent(source).setDataAndType(uri, source.getType()))) {
            FileChooser.getFileUri(uri, file -> onLive.accept(UrlUtil.toLocalUrl(file)), () -> finishForwarded(activity, source));
        } else {
            finishForwarded(activity, source);
        }
    }

    private static void finishForwarded(Activity activity, Intent source) {
        if (source.getBooleanExtra(FORWARD_RESULT, false)) activity.finish();
    }

    private static void openText(FragmentActivity activity, String text, Intent source, Consumer<String> onLive) {
        Matcher matcher = URL.matcher(text.trim());
        onLive.accept(matcher.find() ? matcher.group(0) : text.trim());
    }

    public static Uri getSourceUri(Intent source) {
        if (Intent.ACTION_VIEW.equals(source.getAction()) && source.getData() != null) return source.getData();
        Object stream = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? source.getParcelableExtra(Intent.EXTRA_STREAM, Uri.class) : source.getParcelableExtra(Intent.EXTRA_STREAM);
        if (stream instanceof Uri uri) return uri;
        if (source.getData() != null) return source.getData();
        ClipData clip = source.getClipData();
        return clip == null || clip.getItemCount() == 0 ? null : clip.getItemAt(0).getUri();
    }

    public static Uri getSubtitleUri(Intent source) {
        String value = getSubtitle(source);
        return TextUtils.isEmpty(value) ? null : UriUtil.uri(value);
    }

    private static String getSubtitle(Intent source) {
        Bundle extras = source.getExtras();
        if (extras == null) return null;
        Object value = extras.get("subtitle");
        return value instanceof Uri || value instanceof String ? value.toString() : null;
    }

    public static void apply(Result result, Intent source) {
        Bundle headers = source.getBundleExtra(HEADERS);
        if (headers != null && !headers.isEmpty()) {
            Map<String, String> values = new HashMap<>();
            for (String key : headers.keySet()) values.put(key, headers.getString(key));
            result.addHeaders(values);
        }
    }

    public static long takePosition(Intent source, long fallback) {
        long position = source.getLongExtra(POSITION, C.TIME_UNSET);
        source.removeExtra(POSITION);
        return position >= 0 ? position : fallback;
    }

    public static void clearOptions(Intent source) {
        source.removeExtra(HEADERS);
        source.removeExtra(SUBTITLE);
        source.removeExtra(POSITION);
    }

    public static boolean returnsResult(Intent source) {
        return source.getBooleanExtra(RETURN_RESULT, false);
    }

    public static void setResult(Activity activity, long position, boolean completed) {
        int value = (int) Math.min(Integer.MAX_VALUE, Math.max(0, position));
        activity.setResult(Activity.RESULT_OK, new Intent().putExtra("position", value).putExtra("end_by", completed ? "playback_completion" : "user"));
    }
}
