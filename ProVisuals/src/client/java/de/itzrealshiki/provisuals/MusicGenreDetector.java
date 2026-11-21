package de.itzrealshiki.provisuals;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Looks up genre tags for the current media-session track.
 *
 * This is metadata-based classification, not audio fingerprinting. Only the
 * current track title and artist are sent to MusicBrainz; no audio is uploaded.
 */
public final class MusicGenreDetector {
    private record GenreTag(String name, int count) {}

    private static final String USER_AGENT =
            "ProVisuals/0.1.0 (https://github.com/ItzRealShiki/Fabric-26.3-Mods)";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "ProVisuals-Genre-Lookup");
        thread.setDaemon(true);
        return thread;
    });
    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();
    private static final Object RATE_LIMIT_LOCK = new Object();

    private static volatile String activeTrackKey = "";
    private static volatile String currentGenre = "Genre lookup idle";
    private static long lastRequestNanos;

    private MusicGenreDetector() {}

    public static void update(String title, String artist) {
        if (!ProVisualsConfig.genreRecognitionEnabled) {
            activeTrackKey = "";
            currentGenre = "Genre detection off";
            return;
        }

        if (title == null || title.isBlank() || artist == null || artist.isBlank()) {
            activeTrackKey = "";
            currentGenre = "Genre not available";
            return;
        }

        String key = (artist.trim() + "\u0000" + title.trim()).toLowerCase(Locale.ROOT);
        if (key.equals(activeTrackKey)) return;

        activeTrackKey = key;
        String cached = CACHE.get(key);
        if (cached != null) {
            currentGenre = cached;
            return;
        }

        currentGenre = "Detecting genre...";
        WORKER.execute(() -> {
            if (!key.equals(activeTrackKey)) return;
            String genre = lookupGenre(title.trim(), artist.trim());
            CACHE.put(key, genre);
            if (CACHE.size() > 256) CACHE.clear();
            if (key.equals(activeTrackKey)) currentGenre = genre;
        });
    }

    public static String currentGenre() {
        return currentGenre;
    }

    private static String lookupGenre(String title, String artist) {
        String fallback = guessFromText(title + " " + artist);
        try {
            String query = "recording:\"" + luceneEscape(title)
                    + "\" AND artist:\"" + luceneEscape(artist) + "\"";
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            JsonObject search = request("https://musicbrainz.org/ws/2/recording/?query="
                    + encoded + "&fmt=json&limit=1");

            JsonArray recordings = search.has("recordings") && search.get("recordings").isJsonArray()
                    ? search.getAsJsonArray("recordings") : new JsonArray();
            if (recordings.isEmpty() || !recordings.get(0).isJsonObject()) {
                return fallback == null ? "Genre unknown" : fallback;
            }

            JsonObject match = recordings.get(0).getAsJsonObject();
            if (!match.has("id") || match.get("id").isJsonNull()) {
                return fallback == null ? "Genre unknown" : fallback;
            }

            String id = match.get("id").getAsString();
            JsonObject details = request("https://musicbrainz.org/ws/2/recording/"
                    + id + "?inc=genres%2Btags&fmt=json");

            List<GenreTag> tags = new ArrayList<>();
            collectTags(details, "genres", tags);
            collectTags(details, "tags", tags);

            String matchedGenre = tags.stream()
                    .map(tag -> new GenreTag(normalizeGenre(tag.name()), tag.count()))
                    .filter(tag -> !tag.name().equals("Genre unknown"))
                    .max(Comparator.comparingInt((GenreTag tag) -> genrePriority(tag.name()))
                            .thenComparingInt(GenreTag::count))
                    .map(GenreTag::name)
                    .orElse(null);

            if (matchedGenre != null) return matchedGenre;
            return fallback == null ? "Genre unknown" : fallback;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return fallback == null ? "Genre unavailable" : fallback;
        } catch (Exception exception) {
            Diagnostics.warn("Genre lookup failed: " + exception.getClass().getSimpleName());
            return fallback == null ? "Genre unavailable" : fallback;
        }
    }

    private static JsonObject request(String url) throws IOException, InterruptedException {
        waitForRateLimit();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("MusicBrainz returned HTTP " + response.statusCode());
        }
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    /** MusicBrainz limits clients to one web-service request per second. */
    private static void waitForRateLimit() throws InterruptedException {
        synchronized (RATE_LIMIT_LOCK) {
            long now = System.nanoTime();
            long minimumGap = 1_100_000_000L;
            long waitNanos = minimumGap - (now - lastRequestNanos);
            if (lastRequestNanos != 0L && waitNanos > 0L) {
                long millis = waitNanos / 1_000_000L;
                int nanos = (int) (waitNanos % 1_000_000L);
                Thread.sleep(millis, nanos);
            }
            lastRequestNanos = System.nanoTime();
        }
    }

    private static void collectTags(JsonObject source, String key, List<GenreTag> output) {
        if (!source.has(key) || !source.get(key).isJsonArray()) return;
        for (JsonElement element : source.getAsJsonArray(key)) {
            if (!element.isJsonObject()) continue;
            JsonObject tag = element.getAsJsonObject();
            if (!tag.has("name") || tag.get("name").isJsonNull()) continue;
            int count = tag.has("count") && tag.get("count").isJsonPrimitive()
                    ? tag.get("count").getAsInt() : 0;
            output.add(new GenreTag(tag.get("name").getAsString(), count));
        }
    }

    private static String luceneEscape(String value) {
        return value.replace("\\", " ").replace("\"", " ").replace(":", " ");
    }

    private static String guessFromText(String text) {
        String normalized = text.toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("\\s+", " ");
        if (normalized.contains("hardtekk") || normalized.contains("hard tekk")) return "Hardtekk";
        if (normalized.contains("tekkno") || normalized.contains("tekno")) return "Tekno";
        if (normalized.contains("hardstyle") || normalized.contains("rawstyle")) return "Hardstyle";
        if (normalized.contains("gabber")) return "Gabber";
        if (normalized.contains("hardcore")) return "Hardcore";
        if (normalized.contains("schranz")) return "Schranz";
        if (normalized.contains("psytrance") || normalized.contains("trance")) return "Trance";
        if (normalized.contains("drum and bass") || normalized.contains("drum & bass")
                || normalized.contains(" dnb ")) return "Drum & Bass";
        if (normalized.contains("phonk")) return "Phonk";
        if (normalized.contains("techno")) return "Techno";
        if (normalized.contains("house")) return "House";
        if (normalized.contains("dubstep")) return "Dubstep";
        if (normalized.contains("hardtek")) return "Hardtek";
        return null;
    }

    private static String normalizeGenre(String value) {
        String tag = value.toLowerCase(Locale.ROOT).trim()
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("\\s+", " ");

        if (tag.contains("hardtekk") || tag.equals("hard tekk")) return "Hardtekk";
        if (tag.equals("tekkno") || tag.equals("tekno")) return "Tekno";
        if (tag.contains("hardtek")) return "Hardtek";
        if (tag.contains("rawstyle") || tag.contains("hardstyle")) return "Hardstyle";
        if (tag.contains("gabber")) return "Gabber";
        if (tag.contains("hardcore")) return "Hardcore";
        if (tag.contains("schranz")) return "Schranz";
        if (tag.contains("psytrance") || tag.equals("trance") || tag.contains("hard trance")) return "Trance";
        if (tag.contains("drum and bass") || tag.contains("drum & bass") || tag.equals("dnb")) return "Drum & Bass";
        if (tag.contains("phonk")) return "Phonk";
        if (tag.contains("techno")) return "Techno";
        if (tag.contains("house")) return "House";
        if (tag.contains("dubstep")) return "Dubstep";
        if (tag.contains("electronic") || tag.contains("electronica")) return "Electronic";
        if (tag.contains("hip hop") || tag.equals("rap")) return "Hip-Hop";
        if (tag.contains("pop")) return "Pop";
        return "Genre unknown";
    }

    private static int genrePriority(String genre) {
        return switch (genre) {
            case "Hardtekk" -> 100;
            case "Tekno", "Hardtek" -> 95;
            case "Hardstyle", "Gabber", "Schranz" -> 90;
            case "Hardcore" -> 85;
            case "Drum & Bass", "Phonk", "Trance", "Dubstep" -> 80;
            case "Techno", "House" -> 70;
            case "Hip-Hop", "Pop" -> 60;
            case "Electronic" -> 50;
            default -> 0;
        };
    }
}
