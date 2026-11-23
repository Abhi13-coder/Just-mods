package com.abhi13.pipepipe.hentaihaven;

import static org.schabi.newpipe.extractor.StreamingService.ServiceInfo.MediaCapability.VIDEO;

import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.channel.ChannelExtractor;
import org.schabi.newpipe.extractor.channel.ChannelTabExtractor;
import org.schabi.newpipe.extractor.comments.CommentsExtractor;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.kiosk.KioskList;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory;
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor;
import org.schabi.newpipe.extractor.plugin.PluginPreferences;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.subscription.SubscriptionExtractor;
import org.schabi.newpipe.extractor.suggestion.SuggestionExtractor;
import java.util.Collections;

public final class HentaiHavenService extends StreamingService {
    static final String DEFAULT_API_BASE = "https://haven.reyamika.eu.cc/api";
    private final PluginPreferences preferences;

    public HentaiHavenService(final int id, final PluginPreferences preferences) {
        super(id, "HentaiHaven", Collections.singletonList(VIDEO));
        this.preferences = preferences;
    }

    String getApiBaseUrl() {
        String base = preferences.getString("api_base_url");
        if (base == null || base.trim().isEmpty()) base = DEFAULT_API_BASE;
        return base.trim().replaceAll("/+$", "");
    }

    @Override public String getBaseUrl() { return HentaiHavenUrls.SITE; }
    @Override public LinkHandlerFactory getStreamLHFactory() { return new HentaiHavenStreamLinkHandlerFactory(); }
    @Override public ListLinkHandlerFactory getChannelLHFactory() { return null; }
    @Override public ListLinkHandlerFactory getChannelTabLHFactory() { return null; }
    @Override public ListLinkHandlerFactory getPlaylistLHFactory() { return new HentaiHavenPlaylistLinkHandlerFactory(); }
    @Override public SearchQueryHandlerFactory getSearchQHFactory() { return new HentaiHavenSearchQueryHandlerFactory(); }
    @Override public ListLinkHandlerFactory getCommentsLHFactory() { return null; }
    @Override public SearchExtractor getSearchExtractor(final SearchQueryHandler handler) {
        return new HentaiHavenSearchExtractor(this, handler, this);
    }
    @Override public SuggestionExtractor getSuggestionExtractor() { return null; }
    @Override public SubscriptionExtractor getSubscriptionExtractor() { return null; }
    @Override public KioskList getKioskList() { return new KioskList(this); }
    @Override public ChannelExtractor getChannelExtractor(final ListLinkHandler handler)
            throws ExtractionException { throw new ExtractionException("HentaiHaven channels are not supported"); }
    @Override public ChannelTabExtractor getChannelTabExtractor(final ListLinkHandler handler)
            throws ExtractionException { throw new ExtractionException("HentaiHaven channel tabs are not supported"); }
    @Override public PlaylistExtractor getPlaylistExtractor(final ListLinkHandler handler) {
        return new HentaiHavenPlaylistExtractor(this, handler, this);
    }
    @Override public StreamExtractor getStreamExtractor(final LinkHandler handler) {
        return new HentaiHavenStreamExtractor(this, handler, this);
    }
    @Override public CommentsExtractor getCommentsExtractor(final ListLinkHandler handler)
            throws ExtractionException { throw new ExtractionException("HentaiHaven comments are not supported"); }
}
