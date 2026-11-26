package com.abhi13.pipepipe.hentaihaven;

import java.net.URI;
import java.net.URISyntaxException;

final class HentaiHavenUrls {
    static final String SITE = "https://hentaihaven.com";
    private HentaiHavenUrls() { }

    static String series(final String slug) {
        return SITE + "/series/" + slug;
    }

    static String episode(final String slug, final int number) {
        return SITE + "/watch/" + slug + "/" + number;
    }

    static String[] pathSegments(final String url) throws URISyntaxException {
        final URI uri = new URI(url);
        final String host = uri.getHost();
        if (host == null || !(host.equalsIgnoreCase("hentaihaven.com")
                || host.equalsIgnoreCase("www.hentaihaven.com")
                || host.equalsIgnoreCase("hentaihaven.xxx")
                || host.equalsIgnoreCase("www.hentaihaven.xxx"))) {
            throw new URISyntaxException(url, "Not a HentaiHaven URL");
        }
        final String path = uri.getPath();
        if (path == null) throw new URISyntaxException(url, "URL has no path");
        return java.util.Arrays.stream(path.split("/"))
                .filter(part -> !part.isEmpty()).toArray(String[]::new);
    }
}
