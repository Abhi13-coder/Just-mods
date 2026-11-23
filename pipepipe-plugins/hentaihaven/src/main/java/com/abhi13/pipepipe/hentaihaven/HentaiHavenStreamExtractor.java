package com.abhi13.pipepipe.hentaihaven;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.DeliveryMethod;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.schabi.newpipe.extractor.stream.VideoStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class HentaiHavenStreamExtractor extends StreamExtractor {
    private final HentaiHavenService service;
    private String slug;
    private int episodeNumber;
    private JsonObject details;
    private JsonObject playback;
    private JsonObject episodeInfo;

    HentaiHavenStreamExtractor(final StreamingService service, final LinkHandler handler,
                               final HentaiHavenService havenService) {
        super(service, handler);
        this.service = havenService;
    }

    @Override
    public void onFetchPage(final Downloader downloader) throws IOException, ExtractionException {
        final String[] id = getLinkHandler().getId().split("\\|", 2);
        if (id.length != 2) throw new ExtractionException("Invalid HentaiHaven episode reference");
        slug = id[0];
        try { episodeNumber = Integer.parseInt(id[1]); }
        catch (final NumberFormatException e) { throw new ExtractionException("Invalid episode number", e); }
        if (episodeNumber < 1) throw new ExtractionException("Episode number must be positive");

        details = HavenApi.payload(HavenApi.get(downloader, service, "/anime/" + HavenApi.encode(slug)));
        playback = HavenApi.payload(HavenApi.get(downloader, service,
                "/anime/" + HavenApi.encode(slug) + "/episodes/" + episodeNumber + "/stream"));
        final JsonArray episodes = HavenApi.array(details, "episodes");
        if (episodes != null) {
            for (int i = 0; i < episodes.size(); i++) {
                final JsonObject candidate = episodes.getObject(i);
                if (candidate != null && HavenApi.number(candidate, "number", "episodeNumber", "episode") == episodeNumber) {
                    episodeInfo = candidate;
                    break;
                }
            }
        }
    }

    @Override public String getName() {
        final String series = HavenApi.string(details, "title", "name", "slug");
        final String episode = HavenApi.string(episodeInfo, "title", "name", "episodeTitle");
        final String label = episode.isEmpty() ? "Episode " + episodeNumber : episode;
        return series.isEmpty() ? label : series + " - " + label;
    }
    @Override public String getThumbnailUrl() {
        final String episode = HavenApi.string(episodeInfo, "thumbnail", "thumbnailUrl", "posterUrl");
        return episode.isEmpty() ? HavenApi.string(details, "posterUrl", "cover", "thumbnail") : episode;
    }
    @Override public String getUploaderUrl() { return ""; }
    @Override public String getUploaderName() { return "HentaiHaven"; }
    @Override public int getAgeLimit() { return 18; }
    @Override public long getLength() { return Math.max(0, HavenApi.number(episodeInfo, "durationSeconds", "duration")); }
    @Override public StreamType getStreamType() { return StreamType.VIDEO_STREAM; }
    @Override public List<AudioStream> getAudioStreams() { return Collections.emptyList(); }
    @Override public List<VideoStream> getVideoOnlyStreams() { return Collections.emptyList(); }

    @Override
    public List<VideoStream> getVideoStreams() throws ExtractionException {
        final JsonArray streams = HavenApi.array(playback, "streams");
        if (streams == null || streams.size() == 0) {
            throw new ExtractionException("No direct HLS stream was returned. The upstream player may be unavailable.");
        }
        final List<VideoStream> result = new ArrayList<>();
        int streamIndex = 0;
        for (int i = 0; i < streams.size(); i++) {
            final JsonObject source = streams.getObject(i);
            if (source == null) continue;
            final JsonArray variants = HavenApi.array(source, "variants");
            if (variants != null && variants.size() > 0) {
                for (int j = 0; j < variants.size(); j++) {
                    final JsonObject variant = variants.getObject(j);
                    if (variant == null) continue;
                    final String variantUrl = HavenApi.string(variant, "url", "src");
                    final String sourceUrl = HavenApi.string(source, "url", "src");
                    final String url = variantUrl.isEmpty() ? sourceUrl : variantUrl;
                    if (!url.isEmpty()) result.add(buildStream(url, variant, source, streamIndex++));
                }
            } else {
                final String url = HavenApi.string(source, "url", "src");
                if (!url.isEmpty()) result.add(buildStream(url, source, null, streamIndex++));
            }
        }
        if (result.isEmpty()) {
            throw new ExtractionException("The HentaiHaven bridge returned no playable stream URLs.");
        }
        return result;
    }

    private VideoStream buildStream(final String url, final JsonObject variant,
                                    final JsonObject fallback, final int index) {
        JsonObject headerObject = variant.getObject("headers");
        if (headerObject == null && fallback != null) headerObject = fallback.getObject("headers");
        final Map<String, String> headers = HavenApi.headers(headerObject);
        String resolution = HavenApi.string(variant, "quality", "label", "resolution", "name");
        if (resolution.isEmpty() && fallback != null) {
            resolution = HavenApi.string(fallback, "quality", "label", "resolution", "name");
        }
        if (resolution.isEmpty()) {
            final long height = HavenApi.number(variant, "height");
            resolution = height > 0 ? height + "p" : "auto";
        }
        return new VideoStream.Builder()
                .setId("hentaihaven-" + index)
                .setContent(url, true)
                .setDeliveryMethod(DeliveryMethod.HLS)
                .setMediaFormat((MediaFormat) null)
                .setResolution(resolution)
                .setIsVideoOnly(false)
                .setRequestHeaders(headers)
                .setCacheable(false)
                .build();
    }
}
