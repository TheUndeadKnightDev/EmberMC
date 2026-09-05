package org.flintmc.flint;

import io.papermc.paper.ServerBuildInfo;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;

/**
 * The few lines FlintMC prints at startup, and nothing more.
 *
 * <p>Printed once from {@code DedicatedServer.initServer}, immediately after
 * vanilla announces its own version, so the two sit together in the log and a
 * pasted log identifies the server at a glance. Later milestones append their
 * one-line status here (profile, Packet Guard, Adaptive Engine) rather than
 * logging separately, so the console stays quiet.
 */
@NullMarked
public final class FlintBanner {

    private FlintBanner() {
    }

    public static void print(final Logger logger) {
        final ServerBuildInfo info = ServerBuildInfo.buildInfo();
        logger.info("=================================");
        logger.info("  FlintMC Server");
        logger.info("  Version:   {}", info.asString(ServerBuildInfo.StringRepresentation.VERSION_SIMPLE));
        logger.info("  Minecraft: {}", info.minecraftVersionName());
        logger.info("  Upstream:  Paper (compatible)");
        logger.info("  Java:      {}", Runtime.version());
        logger.info("=================================");
    }
}
