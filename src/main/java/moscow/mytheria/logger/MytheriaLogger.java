package moscow.mytheria.logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/**
 * Structured JSON-lines logger.
 *
 * Output location (resolved on first write, in this order):
 *   1. <minecraft_run_dir>/mytheria/actions.jsonl    (typical case)
 *   2. %APPDATA%/Mytheria/actions.jsonl              (Windows fallback)
 *   3. $HOME/Mytheria/actions.jsonl                  (cross-platform fallback)
 *
 * Usage:
 *   MytheriaLogger.event("state_change")
 *       .with("from", "IDLE").with("to", "BUY_SEND").emit();
 *
 *   MytheriaLogger.log("type", "free-form message");
 */
public final class MytheriaLogger {

    private static final Object FILE_LOCK = new Object();
    private static volatile Path resolvedPath = null;

    static {
        System.out.println("[MytheriaLogger] class loaded (path resolves lazily)");
    }

    private MytheriaLogger() {}

    /** Resolve the log file path. Cached after first call. */
    private static Path getLogFile() {
        Path p = resolvedPath;
        if (p != null) return p;
        synchronized (FILE_LOCK) {
            if (resolvedPath != null) return resolvedPath;
            p = resolveLogPath();
            try {
                Files.createDirectories(p.getParent());
            } catch (IOException e) {
                System.out.println("[MytheriaLogger] could not create dir " + p.getParent() + ": " + e.getMessage());
            }
            System.out.println("[MytheriaLogger] log file = " + p.toAbsolutePath());
            resolvedPath = p;
            return p;
        }
    }

    private static Path resolveLogPath() {
        // 1. Try Minecraft's run directory (preferred)
        try {
            net.minecraft.class_310 mc = net.minecraft.class_310.method_1551();
            if (mc != null) {
                java.io.File runDir = mc.field_1697;
                if (runDir != null && runDir.isDirectory()) {
                    return runDir.toPath().resolve("mytheria").resolve("actions.jsonl");
                }
            }
        } catch (Throwable ignored) {
            // MC not available — fall through to OS-level fallbacks
        }

        // 2. Windows: %APPDATA%\Mytheria\actions.jsonl
        String appdata = System.getenv("APPDATA");
        if (appdata != null && !appdata.isBlank()) {
            return Path.of(appdata, "Mytheria", "actions.jsonl");
        }

        // 3. Cross-platform: ~/Mytheria/actions.jsonl
        return Path.of(System.getProperty("user.home"), "Mytheria", "actions.jsonl");
    }

    public static EventBuilder event(String type) {
        return new EventBuilder(type);
    }

    public static void log(String type, String msg) {
        event(type).with("msg", msg).emit();
    }

    private static void write(String line) {
        synchronized (FILE_LOCK) {
            try {
                Files.writeString(
                    getLogFile(), line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND
                );
            } catch (IOException e) {
                System.out.println("[MytheriaLogger] write FAILED: " + e.getMessage());
            } catch (Throwable t) {
                System.out.println("[MytheriaLogger] unexpected error: " + t);
            }
        }
    }

    private static String escapeString(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder(s.length() + 4);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': b.append("\\\\"); break;
                case '"':  b.append("\\\""); break;
                case '\n': b.append("\\n");  break;
                case '\r': b.append("\\r");  break;
                case '\t': b.append("\\t");  break;
                default:
                    if (c < 0x20) {
                        b.append(String.format("\\u%04x", (int) c));
                    } else {
                        b.append(c);
                    }
            }
        }
        return b.toString();
    }

    public static final class EventBuilder {
        private final StringBuilder sb = new StringBuilder(128);

        EventBuilder(String type) {
            sb.append('{')
              .append("\"ts\":\"").append(Instant.now()).append('"')
              .append(",\"type\":\"").append(escapeString(type)).append('"');
        }

        public EventBuilder with(String key, Object value) {
            sb.append(",\"").append(escapeString(key)).append("\":");
            if (value == null) {
                sb.append("null");
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else {
                sb.append('"').append(escapeString(String.valueOf(value))).append('"');
            }
            return this;
        }

        public void emit() {
            sb.append("}\n");
            write(sb.toString());
        }
    }
}
