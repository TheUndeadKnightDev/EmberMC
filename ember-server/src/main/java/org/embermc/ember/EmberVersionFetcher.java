package org.embermc.ember;

import com.destroystokyo.paper.util.VersionFetcher;
import io.papermc.paper.ServerBuildInfo;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jspecify.annotations.NullMarked;

/**
 * What {@code /version} says about updates.
 *
 * <p>Paper's fetcher asks PaperMC's Fill service for the latest <em>Paper</em>
 * build, which on a fork would report Paper's build numbers as updates for a
 * server that is not Paper. Until EmberMC has a release endpoint of its own
 * (see docs/DISTRIBUTION.md) this reports the build it is and says plainly
 * that it does not check for updates. It makes no network requests.
 */
@NullMarked
public final class EmberVersionFetcher implements VersionFetcher {

    /**
     * What the server says about its version at startup, off the main thread.
     *
     * <p>Paper's equivalent asks its download service and warns "unknown
     * version" for anything it does not recognise, which is every EmberMC build.
     * Until EmberMC has a release endpoint there is nothing to compare against,
     * so this says so once, quietly, and makes no network request.
     */
    public static void logStartupStatus() {
        final ServerBuildInfo info = ServerBuildInfo.buildInfo();
        final String build = info.buildNumber().isPresent() ? "build " + info.buildNumber().getAsInt() : "development build";
        org.slf4j.LoggerFactory.getLogger("EmberMC").info("Running EmberMC {} ({}). Update checks are not configured for this build.",
            info.asString(ServerBuildInfo.StringRepresentation.VERSION_SIMPLE), build);
    }

    @Override
    public long getCacheTime() {
        return TimeUnit.MINUTES.toMillis(30);
    }

    @Override
    public Component getVersionMessage() {
        final ServerBuildInfo info = ServerBuildInfo.buildInfo();
        return Component.text()
            .append(Component.text("EmberMC ", NamedTextColor.GOLD))
            .append(Component.text(info.asString(ServerBuildInfo.StringRepresentation.VERSION_FULL), NamedTextColor.WHITE))
            .append(Component.newline())
            .append(Component.text("Update checks are not configured for this build.", NamedTextColor.GRAY))
            .build();
    }
}
