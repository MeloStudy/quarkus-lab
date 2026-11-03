package lab.payments;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Reads back the JSON log file that {@link LogProfile} switches on. */
final class Logs {

    private static final Path FILE = Path.of(LogProfile.FILE);
    private static final ObjectMapper JSON = new ObjectMapper();

    private Logs() {
    }

    /** Complete lines only: the last one may still be being written. */
    static List<String> rawLines() {
        try {
            if (!Files.exists(FILE)) {
                return List.of();
            }
            String content = Files.readString(FILE, StandardCharsets.UTF_8);
            List<String> lines = new ArrayList<>(List.of(content.split("\n", -1)));
            lines.remove(lines.size() - 1);
            lines.replaceAll(line -> line.endsWith("\r") ? line.substring(0, line.length() - 1) : line);
            return lines;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Where the log is now; ask for "what came after" with {@link #since}. */
    static int mark() {
        return rawLines().size();
    }

    static List<String> rawLinesSince(int mark) {
        List<String> lines = rawLines();
        return lines.subList(Math.min(mark, lines.size()), lines.size());
    }

    /** The records written after the mark; lines that are not a JSON object are left out. */
    static List<JsonNode> since(int mark) {
        List<JsonNode> records = new ArrayList<>();
        for (String line : rawLinesSince(mark)) {
            try {
                JsonNode node = JSON.readTree(line);
                if (node != null && node.isObject()) {
                    records.add(node);
                }
            } catch (IOException ignored) {
                // not JSON: the tests that care look at rawLinesSince
            }
        }
        return records;
    }

    /** Waits (up to 3 seconds) until the records after the mark satisfy the condition, then returns them. */
    static List<JsonNode> await(int mark, Predicate<List<JsonNode>> ready) {
        long deadline = System.currentTimeMillis() + 3000;
        List<JsonNode> records = since(mark);
        while (!ready.test(records) && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            records = since(mark);
        }
        return records;
    }

    /** Only the records written by the application's own classes. */
    static List<JsonNode> app(List<JsonNode> records) {
        return records.stream().filter(r -> {
            String logger = text(r, "loggerName");
            return logger != null && logger.startsWith("lab.payments");
        }).toList();
    }

    /** A top-level field of a record, or null. MDC entries are only there when the formatter writes them flat. */
    static String text(JsonNode record, String field) {
        JsonNode node = record.get(field);
        return node == null || node.isNull() ? null : node.asText();
    }
}
