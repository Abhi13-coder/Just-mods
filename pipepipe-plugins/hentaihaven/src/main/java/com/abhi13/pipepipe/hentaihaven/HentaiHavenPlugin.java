package com.abhi13.pipepipe.hentaihaven;

import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.plugin.PipePipePlugin;
import org.schabi.newpipe.extractor.plugin.PluginContext;
import javax.annotation.Nonnull;

/** PipePipe plugin entry point. */
public final class HentaiHavenPlugin implements PipePipePlugin {
    @Nonnull
    @Override
    public StreamingService createService(final int serviceId, @Nonnull final PluginContext context) {
        return new HentaiHavenService(serviceId, context.getPreferences());
    }
}
