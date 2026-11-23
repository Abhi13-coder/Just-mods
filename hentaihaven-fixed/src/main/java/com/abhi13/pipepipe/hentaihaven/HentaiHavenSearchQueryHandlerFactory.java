package com.abhi13.pipepipe.hentaihaven;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory;
import org.schabi.newpipe.extractor.search.filter.FilterItem;
import java.net.URI;
import java.net.URLDecoder;
import java.util.List;

final class HentaiHavenSearchQueryHandlerFactory extends SearchQueryHandlerFactory {
    @Override
    public String getUrl(final String query, final List<FilterItem> contentFilters,
                         final List<FilterItem> sortFilters) {
        return HentaiHavenUrls.SITE + "/search?q=" + HavenApi.encode(query);
    }

    @Override
    public String getSearchString(final String url) {
        try {
            final String query = new URI(url).getRawQuery();
            if (query == null) return "";
            for (final String part : query.split("&")) {
                final int split = part.indexOf('=');
                if (split > 0 && "q".equals(part.substring(0, split))) {
                    return URLDecoder.decode(part.substring(split + 1), java.nio.charset.StandardCharsets.UTF_8);
                }
            }
        } catch (final Exception ignored) { }
        return "";
    }
}
