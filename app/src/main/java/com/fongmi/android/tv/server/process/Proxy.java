package com.fongmi.android.tv.server.process;

import com.fongmi.android.tv.server.Nano;
import com.fongmi.android.tv.server.impl.Process;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.UriUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import okhttp3.Headers;

public class Proxy implements Process {

    private static final String INVALID_RESPONSE = "Invalid proxy response";
    private static final Set<String> RESERVED_KEYS = Set.of("do", "siteKey", "url");

    @Override
    public boolean isRequest(IHTTPSession session, String url) {
        return url.startsWith("/proxy");
    }

    @Override
    public Response doResponse(IHTTPSession session, String url, Map<String, String> files) {
        try {
            String target = getTarget(session);
            if (target == null || !UriUtil.scheme(target).startsWith("http")) return Nano.error(Status.BAD_REQUEST, "Missing proxy url");
            okhttp3.Response response = OkHttp.newCall(target, getHeaders(session)).execute();
            return createResponse(toResult(response));
        } catch (Throwable e) {
            e.printStackTrace();
            return markError(Nano.error(Objects.toString(e.getMessage(), e.toString())));
        }
    }

    private static String getTarget(IHTTPSession session) {
        String url = session.getParms().get("url");
        if (url != null && !url.isEmpty()) return url;
        String query = session.getQueryParameterString();
        return query == null || query.isEmpty() ? null : query;
    }

    private static Map<String, String> getHeaders(IHTTPSession session) {
        Map<String, String> headers = new HashMap<>();
        for (Map.Entry<String, String> entry : session.getParms().entrySet()) if (!RESERVED_KEYS.contains(entry.getKey())) headers.put(entry.getKey(), entry.getValue());
        String range = session.getHeaders().get("range");
        if (range != null && !headers.containsKey("Range")) headers.put("Range", range);
        return headers;
    }

    private static Object[] toResult(okhttp3.Response response) throws IOException {
        Map<String, String> headers = new HashMap<>();
        Headers names = response.headers();
        for (String name : names.names()) headers.put(name, names.get(name));
        InputStream stream = response.body() == null ? null : response.body().byteStream();
        if (stream == null) throw new IOException(INVALID_RESPONSE);
        return new Object[]{response.code(), response.header("Content-Type", "application/octet-stream"), stream, headers};
    }

    static Response createResponse(Object[] rs) {
        if (rs == null || rs.length == 0) return markError(Nano.error(INVALID_RESPONSE));
        if (rs[0] instanceof Response response) return markError(response);
        if (rs.length < 3) return markError(Nano.error(INVALID_RESPONSE));
        InputStream stream = (InputStream) rs[2];
        try {
            Response response = NanoHTTPD.newChunkedResponse(toStatus((Integer) rs[0]), (String) rs[1], stream);
            addHeaders(response, rs.length > 3 ? rs[3] : null);
            return markError(response);
        } catch (RuntimeException | Error error) {
            closeFailedStream(stream, error);
            throw error;
        }
    }

    private static void addHeaders(Response response, Object headers) {
        if (headers == null) return;
        for (Map.Entry<String, String> entry : ((Map<String, String>) headers).entrySet()) {
            String name = entry.getKey();
            if (!name.equalsIgnoreCase("Content-Length") && !name.equalsIgnoreCase("Transfer-Encoding")) response.addHeader(name, entry.getValue());
        }
    }

    private static void closeFailedStream(InputStream stream, Throwable error) {
        if (stream == null) return;
        try {
            stream.close();
        } catch (IOException | RuntimeException | Error failure) {
            if (failure != error) error.addSuppressed(failure);
        }
    }

    private static Response markError(Response response) {
        if (response.getStatus().getRequestStatus() >= 400) response.addHeader("X-TV-Proxy-Error", "1");
        return response;
    }

    private static IStatus toStatus(int code) {
        Status status = Status.lookup(code);
        return status != null ? status : code >= 100 && code <= 599 ? new ProxyStatus(code) : Status.INTERNAL_ERROR;
    }

    private record ProxyStatus(int code) implements IStatus {

        @Override
        public String getDescription() {
            return code + " Proxy Status";
        }

        @Override
        public int getRequestStatus() {
            return code;
        }
    }
}
