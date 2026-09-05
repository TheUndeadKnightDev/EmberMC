package org.embermc.ember.profiler;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opt-in HTTP endpoint serving {@link EmberMetrics} in the Prometheus text
 * exposition format.
 *
 * <p>Off unless {@code metrics.endpoint.enabled} is set. Binds to a configurable
 * address, {@code 127.0.0.1} by default, because a metrics port is not something
 * to expose to the internet by accident. Serves one path, {@code /metrics}, on
 * a single daemon thread; each request reads the gauges once, so a scrape costs
 * what {@code /ember metrics} costs and the server pays nothing between scrapes.
 * Gauges that need the main thread are read as plain fields and rings, which
 * are written by the tick and read here without locking; a scrape may see a
 * value one tick stale, which is fine for a gauge.
 */
@NullMarked
public final class MetricsEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger("EmberMC");
    private static @Nullable HttpServer server;

    private MetricsEndpoint() {
    }

    public static synchronized void start(final String bind, final int port) {
        if (server != null) {
            return;
        }
        try {
            final HttpServer s = HttpServer.create(new InetSocketAddress(bind, port), 8);
            s.createContext("/metrics", exchange -> {
                final byte[] body = render().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/plain; version=0.0.4; charset=utf-8");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            });
            s.setExecutor(runnable -> {
                final Thread t = new Thread(runnable, "EmberMC metrics");
                t.setDaemon(true);
                t.start();
            });
            s.start();
            server = s;
            LOGGER.info("Metrics endpoint listening on http://{}:{}/metrics", bind, port);
        } catch (final IOException ex) {
            LOGGER.warn("Could not start the metrics endpoint on {}:{}: {}", bind, port, ex.getMessage());
        }
    }

    public static synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    /** Prometheus text format: one HELP/TYPE pair and one sample per gauge. */
    static String render() {
        final StringBuilder sb = new StringBuilder(2048);
        for (final Map.Entry<String, Double> e : EmberMetrics.snapshot().entrySet()) {
            final String name = e.getKey();
            sb.append("# TYPE ").append(name).append(" gauge\n");
            sb.append(name).append(' ').append(String.format(Locale.ROOT, "%.6f", e.getValue())).append('\n');
        }
        return sb.toString();
    }
}
