package com.fongmi.android.tv.player.extractor;

import android.net.Uri;

import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.utils.SourceUrl;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.utils.Task;
import com.github.catvod.utils.UriUtil;

import java.util.List;
import java.util.concurrent.CancellationException;

public class Source {

    private final List<Extractor> extractors;
    private long sequence;
    private int active;

    public Source() {
        extractors = List.of(new Push(), new Video(), new WebView(), new Strm(), new TVBus());
    }

    public static Source get() {
        return Loader.INSTANCE;
    }

    public static boolean isBuiltin(String url) {
        if (url == null) return false;
        url = url.trim();
        for (int i = 0; i < url.length(); i++) if (Character.isWhitespace(url.charAt(i))) return false;
        Uri uri = UriUtil.uri(url);
        return switch (UriUtil.scheme(uri)) {
            case "video" -> url.regionMatches(true, 0, "video://", 0, 8);
            case "webview" -> UrlUtil.isWebView(url);
            default -> false;
        };
    }

    public static boolean isPush(String url) {
        return url != null && url.regionMatches(true, 0, "push://", 0, 7);
    }

    private Extractor getExtractor(Uri uri) {
        return extractors.stream().filter(extractor -> extractor.match(uri)).findFirst().orElse(null);
    }

    public boolean canPreload(Uri uri) {
        return !SourceUrl.isResolved(uri.toString()) && getExtractor(uri) == null;
    }

    private synchronized void begin() {
        if (Thread.currentThread().isInterrupted()) throw new CancellationException();
        sequence++;
        active++;
        notifyAll();
    }

    private synchronized void end() {
        active--;
        notifyAll();
    }

    public String fetch(Result result) throws Exception {
        return fetch(result, result.getUrl().v());
    }

    public String fetch(Result result, String id) throws Exception {
        begin();
        try {
            Uri uri = result.getUrl().uri();
            String url = result.getUrl().v();
            Extractor extractor = getExtractor(uri);
            url = extractor == null ? url : extractor.fetch(result, id);
            return url;
        } finally {
            end();
        }
    }

    public synchronized void stop() {
        extractors.forEach(Extractor::stop);
    }

    public synchronized void exit() {
        long current = sequence;
        Task.execute(() -> exit(current));
    }

    private synchronized void exit(long current) {
        try {
            while (current == sequence && active > 0) wait();
            if (current == sequence) extractors.forEach(Extractor::exit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    public interface Extractor {

        default String fetch(Result result, String id) throws Exception {
            return fetch(result);
        }

        default String fetch(Result result) throws Exception {
            return fetch(result.getUrl().v());
        }

        String fetch(String url) throws Exception;

        boolean match(Uri uri);

        default void stop() {}

        default void exit() {}
    }

    private static class Loader {
        static volatile Source INSTANCE = new Source();
    }
}
