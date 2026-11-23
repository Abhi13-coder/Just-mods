package com.abhi13.pipepipe.hentaihaven;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.search.filter.FilterItem;
import java.net.URISyntaxException;
import java.util.List;

final class HentaiHavenPlaylistLinkHandlerFactory extends ListLinkHandlerFactory {
    @Override
    public String getUrl(final String id, final List<FilterItem> contentFilters,
                         final List<FilterItem> sortFilters) {
        return HentaiHavenUrls.series(id);
    }

    @Override
    public String getId(final String url) throws ParsingException {
        try {
            final String[] parts = HentaiHavenUrls.pathSegments(url);
            if (parts.length == 2 && "series".equals(parts[0])) return parts[1];
        } catch (final URISyntaxException e) {
            throw new ParsingException("Invalid HentaiHaven series URL", e);
        }
        throw new ParsingException("Unsupported HentaiHaven series URL: " + url);
    }

    @Override
    public boolean onAcceptUrl(final String url) {
        try {
            final String[] parts = HentaiHavenUrls.pathSegments(url);
            return parts.length == 2 && "series".equals(parts[0]);
        } catch (final Exception ignored) { return false; }
    }
}
