package com.abhi13.pipepipe.hentaihaven;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.MultiInfoItemsCollector;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItemExtractor;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import java.io.IOException;

final class HentaiHavenSearchExtractor extends SearchExtractor {
    private final HentaiHavenService service;
    private Downloader downloader;
    private JsonObject initialResponse;

    HentaiHavenSearchExtractor(final StreamingService service,
                               final SearchQueryHandler handler,
                               final HentaiHavenService havenService) {
        super(service, handler);
        this.service = havenService;
    }

    @Override
    public void onFetchPage(final Downloader downloader) throws IOException, ExtractionException {
        this.downloader = downloader;
        initialResponse = requestPage(1);
    }

    private JsonObject requestPage(final int page) throws IOException, ExtractionException {
        final String path = "/search?q=" + HavenApi.encode(getSearchString())
                + "&page=" + page + "&pages=1";
        return HavenApi.payload(HavenApi.get(downloader, service, path));
    }

    private InfoItemsPage<InfoItem> collect(final JsonObject response, final int page)
            throws ExtractionException {
        final MultiInfoItemsCollector collector = new MultiInfoItemsCollector(getServiceId());
        final JsonArray items = HavenApi.array(response, "items");
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                final JsonObject item = items.getObject(i);
                if (item == null) continue;
                final String slug = HavenApi.string(item, "slug", "name", "id");
                final String title = HavenApi.string(item, "title", "name", "slug");
                if (slug.isEmpty() || title.isEmpty()) continue;
                collector.commit(new PlaylistInfoItemExtractor() {
                    @Override public String getName() { return title; }
                    @Override public String getUrl() { return HentaiHavenUrls.series(slug); }
                    @Override public String getThumbnailUrl() {
                        return HavenApi.string(item, "posterUrl", "thumbnailUrl", "poster", "thumbnail", "cover");
                    }
                    @Override public String getUploaderName() { return "HentaiHaven"; }
                    @Override public long getStreamCount() {
                        final long count = HavenApi.number(item, "episodeCount", "episodesCount", "episode_count");
                        return count >= 0 ? count : 0;
                    }
                });
            }
        }
        Page next = null;
        if (HavenApi.bool(response, "hasNextPage")) {
            final String path = service.getApiBaseUrl() + "/search?q=" + HavenApi.encode(getSearchString())
                    + "&page=" + (page + 1) + "&pages=1";
            next = new Page(path);
        }
        return new InfoItemsPage<>(collector, next);
    }

    @Override
    protected InfoItemsPage<InfoItem> getInitialPageInternal() throws IOException, ExtractionException {
        return collect(initialResponse, 1);
    }

    @Override
    protected InfoItemsPage<InfoItem> getPageInternal(final Page page) throws IOException, ExtractionException {
        final JsonObject response = HavenApi.payload(HavenApi.getUrl(downloader, page.getUrl()));
        final int current = (int) HavenApi.number(response, "page");
        return collect(response, current > 0 ? current : 2);
    }
}
