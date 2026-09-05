package org.flintmc.flint;

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
 * server that is not Paper. Until FlintMC has a release endpoint of its own
 * (see docs/DISTRIBUTION.md) this reports the build it is and says plainly
 * that it does not check for updates. It makes no network requests.
 */
@NullMarked
public final class FlintVersionFetcher implements VersionFetcher {

    @Override
    public long getCacheTime() {
        return TimeUnit.MINUTES.toMillis(30);
    }

    @Override
    public Component getVersionMessage() {
        final ServerBuildInfo info = ServerBuildInfo.buildInfo();
        return Component.text()
            .append(Component.text("FlintMC ", NamedTextColor.GOLD))
            .append(Component.text(info.asString(ServerBuildInfo.StringRepresentation.VERSION_FULL), NamedTextColor.WHITE))
            .append(Component.newline())
            .append(Component.text("Update checks are not configured for this build.", NamedTextColor.GRAY))
            .build();
    }
}
