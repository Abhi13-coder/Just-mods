package com.abhi13.pipepipe.hentaihaven;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector;
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor;
import org.schabi.newpipe.extractor.stream.StreamType;
import java.io.IOException;

final class HentaiHavenPlaylistExtractor extends PlaylistExtractor {
    private final HentaiHavenService service;
    private JsonObject details;

    HentaiHavenPlaylistExtractor(final StreamingService service, final ListLinkHandler handler,
                                 final HentaiHavenService havenService) {
        super(service, handler);
        this.service = havenService;
    }

    @Override
    public void onFetchPage(final Downloader downloader) throws IOException, ExtractionException {
        final String slug = getLinkHandler().getId();
        details = HavenApi.payload(HavenApi.get(downloader, service,
                "/anime/" + HavenApi.encode(slug)));
    }

    @Override public String getName() {
        return HavenApi.string(details, "title", "name", "slug");
    }
    @Override public String getUploaderUrl() { return ""; }
    @Override public String getUploaderName() { return "HentaiHaven"; }
    @Override public String getUploaderAvatarUrl() { return ""; }
    @Override public boolean isUploaderVerified() { return false; }
    @Override public String getThumbnailUrl() {
        return HavenApi.string(details, "posterUrl", "cover", "thumbnail", "backdropUrl");
    }
    @Override public long getStreamCount() {
        final long count = HavenApi.number(details, "episodeCount", "episodesCount");
        final JsonArray episodes = HavenApi.array(details, "episodes");
        return count >= 0 ? count : episodes == null ? 0 : episodes.size();
    }

    @Override
    public InfoItemsPage<org.schabi.newpipe.extractor.stream.StreamInfoItem> getInitialPage()
            throws IOException, ExtractionException {
        final StreamInfoItemsCollector collector = new StreamInfoItemsCollector(getServiceId());
        final JsonArray episodes = HavenApi.array(details, "episodes");
        if (episodes != null) {
            final String slug = getLinkHandler().getId();
            final String seriesPoster = getThumbnailUrl();
            for (int i = 0; i < episodes.size(); i++) {
                final JsonObject episode = episodes.getObject(i);
                if (episode == null) continue;
                final long number = HavenApi.number(episode, "number", "episodeNumber", "episode");
                if (number <= 0 || number > Integer.MAX_VALUE) continue;
                final String title = HavenApi.string(episode, "title", "name", "episodeTitle");
                final String thumbnail = HavenApi.string(episode, "thumbnail", "thumbnailUrl", "posterUrl");
                collector.commit(new StreamInfoItemExtractor() {
                    @Override public String getName() {
                        return title.isEmpty() ? "Episode " + number : title;
                    }
                    @Override public String getUrl() { return HentaiHavenUrls.episode(slug, (int) number); }
                    @Override public String getThumbnailUrl() { return thumbnail.isEmpty() ? seriesPoster : thumbnail; }
                    @Override public StreamType getStreamType() { return StreamType.VIDEO_STREAM; }
                    @Override public long getDuration() { return HavenApi.number(episode, "duration", "durationSeconds"); }
                    @Override public long getViewCount() { return -1; }
                    @Override public String getUploaderName() { return "HentaiHaven"; }

                    @Override
                    public String getTextualUploadDate() throws ParsingException {
                        return null;
                    }

                    @Override
                    public org.schabi.newpipe.extractor.localization.DateWrapper getUploadDate() throws ParsingException {
                        return null;
                    }
                });
            }
        }
        return new InfoItemsPage<>(collector, null);
    }

    @Override
    public InfoItemsPage<org.schabi.newpipe.extractor.stream.StreamInfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        return new InfoItemsPage<>(new StreamInfoItemsCollector(getServiceId()), null);
    }
}
