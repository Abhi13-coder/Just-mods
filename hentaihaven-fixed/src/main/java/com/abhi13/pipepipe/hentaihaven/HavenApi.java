package com.abhi13.pipepipe.hentaihaven;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Minimal JSON client for the configurable HentaiHaven bridge. */
final class HavenApi {
    private HavenApi() { }

    static JsonObject get(final Downloader downloader, final HentaiHavenService service,
                          final String path) throws IOException, ExtractionException {
        final String url = path.startsWith("https://") ? path : service.getApiBaseUrl() + path;
        return getUrl(downloader, url);
    }

    static JsonObject getUrl(final Downloader downloader, final String url)
            throws IOException, ExtractionException {
        final Map<String, java.util.List<String>> headers = new LinkedHashMap<>();
        headers.put("Accept", Collections.singletonList("application/json"));
        headers.put("Cache-Control", Collections.singletonList("no-cache"));
        final Response response = downloader.get(url, headers);
        final int status = response.responseCode();
        final String body = response.responseBody();
        if (status < 200 || status >= 300) {
            String message = "HTTP " + status;
            try {
                final JsonObject error = JsonParser.object().from(body);
                final String detail = string(error, "error", "message");
                if (!detail.isEmpty()) message += ": " + detail;
            } catch (final Exception ignored) {
                // Preserve the HTTP status when the upstream error body is not JSON.
            }
            throw new ExtractionException("HentaiHaven API request failed: " + message);
        }
        try {
            return JsonParser.object().from(body);
        } catch (final JsonParserException e) {
            throw new ExtractionException("HentaiHaven API returned invalid JSON", e);
        }
    }

    static JsonObject payload(final JsonObject root) {
        final JsonObject data = root.getObject("data");
        return data == null ? root : data;
    }

    static JsonArray array(final JsonObject object, final String key) {
        return object == null ? null : object.getArray(key);
    }

    static String string(final JsonObject object, final String... keys) {
        if (object == null) return "";
        for (final String key : keys) {
            final Object value = object.get(key);
            if (value != null) {
                final String text = String.valueOf(value).trim();
                if (!text.isEmpty() && !"null".equalsIgnoreCase(text)) return text;
            }
        }
        return "";
    }

    static long number(final JsonObject object, final String... keys) {
        if (object == null) return -1L;
        for (final String key : keys) {
            final Object value = object.get(key);
            if (value instanceof Number) return ((Number) value).longValue();
            if (value != null) {
                try { return Long.parseLong(String.valueOf(value)); }
                catch (final NumberFormatException ignored) { }
            }
        }
        return -1L;
    }

    static boolean bool(final JsonObject object, final String key) {
        return object != null && Boolean.TRUE.equals(object.get(key));
    }

    static String encode(final String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    static Map<String, String> headers(final JsonObject object) {
        final Map<String, String> result = new LinkedHashMap<>();
        if (object == null) return result;
        for (final Map.Entry<String, Object> entry : object.entrySet()) {
            if (entry.getValue() != null) result.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return result;
    }
}
