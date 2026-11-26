package com.abhi13.pipepipe.hentaihaven;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory;
import java.net.URISyntaxException;

final class HentaiHavenStreamLinkHandlerFactory extends LinkHandlerFactory {
    @Override
    public String getId(final String url) throws ParsingException {
        try {
            final String[] parts = HentaiHavenUrls.pathSegments(url);
            if (parts.length == 3 && "watch".equals(parts[0])) {
                Integer.parseInt(parts[2]);
                return parts[1] + "|" + parts[2];
            }
        } catch (final Exception e) {
            throw new ParsingException("Invalid HentaiHaven episode URL", e);
        }
        throw new ParsingException("Unsupported HentaiHaven episode URL: " + url);
    }

    @Override
    public String getUrl(final String id) throws ParsingException {
        final String[] parts = id.split("\\|", 2);
        if (parts.length != 2) throw new ParsingException("Invalid HentaiHaven episode id");
        try { return HentaiHavenUrls.episode(parts[0], Integer.parseInt(parts[1])); }
        catch (final NumberFormatException e) { throw new ParsingException("Invalid episode number", e); }
    }

    @Override
    public boolean onAcceptUrl(final String url) {
        try {
            final String[] parts = HentaiHavenUrls.pathSegments(url);
            return parts.length == 3 && "watch".equals(parts[0])
                    && Integer.parseInt(parts[2]) > 0;
        } catch (final Exception ignored) { return false; }
    }
}
