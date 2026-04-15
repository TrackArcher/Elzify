package com.elzify.music.subsonicster;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SubsonicsterCsvRepository {

    public static final String SONGSEEKER_PLAYLIST_INDEX_URL =
            "https://raw.githubusercontent.com/andygruber/songseeker-hitster-playlists/refs/heads/main/playlists.csv";

    private final Context context;
    private final OkHttpClient okHttpClient = new OkHttpClient();

    public SubsonicsterCsvRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public static class CsvSong {
        public final int index;
        public final String title;
        public final String artist;
        @Nullable public final Integer year;
        @Nullable public final String url;

        public CsvSong(int index, String title, String artist, @Nullable Integer year, @Nullable String url) {
            this.index = index;
            this.title = title;
            this.artist = artist;
            this.year = year;
            this.url = url;
        }
    }

    public static class CsvInfo {
        public final String csvFileName;
        public final int index;

        public CsvInfo(String csvFileName, int index) {
            this.csvFileName = csvFileName;
            this.index = index;
        }
    }

    private File getCsvDir() {
        File dir = new File(context.getFilesDir(), "csv_files");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }

    public List<String> getAllLocalCsvFiles() {
        File[] files = getCsvDir().listFiles();
        if (files == null) return Collections.emptyList();
        List<String> names = new ArrayList<>();
        for (File f : files) {
            if (f.isFile() && f.getName().toLowerCase(Locale.US).endsWith(".csv")) {
                names.add(f.getName());
            }
        }
        Collections.sort(names);
        return names;
    }

    public void deleteCsv(String fileName) {
        File file = new File(getCsvDir(), fileName);
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    public String saveLocalCsv(Uri uri, @Nullable String displayName) throws IOException {
        String fileName = sanitizeCsvName(displayName);
        if (fileName == null) {
            fileName = sanitizeCsvName(getDisplayNameFromUri(uri));
        }
        if (fileName == null) {
            fileName = "imported_" + System.currentTimeMillis() + ".csv";
        }

        File outFile = new File(getCsvDir(), fileName);
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(outFile)) {
            if (in == null) throw new IOException("Could not open selected file.");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
        return outFile.getName();
    }

    public String downloadAndSave(String csvUrlOrName) throws IOException {
        String fileName = getFileName(csvUrlOrName);
        String fullUrl = getFullUrl(csvUrlOrName);

        Request request = new Request.Builder().url(fullUrl).build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Download failed: HTTP " + response.code());
            }
            String content = response.body().string();
            File outFile = new File(getCsvDir(), fileName);
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8)) {
                writer.write(content);
            }
            return fileName;
        }
    }

    public List<String> fetchPlaylistIndexFilenames() throws IOException {
        Request request = new Request.Builder().url(SONGSEEKER_PLAYLIST_INDEX_URL).build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Failed to download playlist index: HTTP " + response.code());
            }
            String body = response.body().string();
            if (body.startsWith("\uFEFF")) {
                body = body.substring(1);
            }
            List<String> names = new ArrayList<>();
            boolean first = true;
            for (String raw : body.split("\n")) {
                String line = raw.trim();
                if (line.isEmpty()) continue;
                String firstField = parseFirstCsvField(line);
                if (firstField == null) continue;
                if (first) {
                    first = false;
                    if ("File".equalsIgnoreCase(firstField)) continue;
                }
                if (firstField.toLowerCase(Locale.US).endsWith(".csv")) {
                    names.add(firstField);
                }
            }
            return names;
        }
    }

    @Nullable
    public CsvSong findSongForQrContent(String qrContent) throws IOException {
        String normalized = parseSupportedQrUrl(qrContent);
        if (normalized == null) return null;

        CsvInfo csvInfo = parseHitsterUrl(normalized);
        if (csvInfo != null) {
            try {
                CsvSong song = getSongFromCsvByIndex(csvInfo.csvFileName, csvInfo.index);
                if (song != null) return song;
            } catch (IOException ignored) {
                // Fall back to local CSV scan below.
            }

            // Fallback: try same index across all local CSV files.
            for (String fileName : getAllLocalCsvFiles()) {
                try {
                    CsvSong byIndex = getSongFromCsvByIndex(fileName, csvInfo.index);
                    if (byIndex != null) return byIndex;
                } catch (IOException ignored) {
                    // Skip unreadable file and continue.
                }
            }
        }

        for (String fileName : getAllLocalCsvFiles()) {
            CsvSong byUrl = getSongFromCsvByUrl(fileName, normalized);
            if (byUrl != null) return byUrl;
        }
        return null;
    }

    @Nullable
    public static String parseSupportedQrUrl(String qrCodeContent) {
        String normalizedContent = qrCodeContent == null ? "" : qrCodeContent.trim();
        if (normalizedContent.isEmpty()) return null;
        String lower = normalizedContent.toLowerCase(Locale.US);
        if (lower.contains("hitstergame.com/") || lower.contains("hitster.com/")
                || lower.contains("youtube.com/") || lower.contains("youtu.be/")) {
            return normalizedContent;
        }
        return null;
    }

    @Nullable
    public static CsvInfo parseHitsterUrl(String qrCodeUrl) {
        try {
            String lower = qrCodeUrl.toLowerCase(Locale.US);
            if (!(lower.contains("hitstergame.com") || lower.contains("hitster.com"))) {
                return null;
            }
            URI uri = parseLookupUri(qrCodeUrl);
            if (uri == null || uri.getHost() == null) {
                return null;
            }

            String path = uri.getPath() == null ? "" : uri.getPath();
            List<String> pathParts = new ArrayList<>();
            for (String seg : path.split("/")) {
                if (seg != null && !seg.isBlank()) {
                    pathParts.add(seg.trim());
                }
            }

            if (pathParts.isEmpty()) return null;
            String language = pathParts.get(0).toLowerCase(Locale.US);
            List<String> remaining = pathParts.subList(1, pathParts.size());
            String csvFileName;
            int index;

            if (remaining.size() == 2) {
                String prefix = remaining.get(0);
                String cardIndex = remaining.get(1);
                if (prefix.matches("^[a-z]{4}\\d{4}$")) {
                    csvFileName = "hitster-" + language + "-" + prefix + ".csv";
                    index = Integer.parseInt(cardIndex);
                } else {
                    csvFileName = "hitster-" + language + ".csv";
                    index = Integer.parseInt(prefix);
                }
            } else if (remaining.size() == 1) {
                csvFileName = "hitster-" + language + ".csv";
                index = Integer.parseInt(remaining.get(0));
            } else {
                return null;
            }
            return new CsvInfo(csvFileName, index);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private CsvSong getSongFromCsvByIndex(String csvName, int index) throws IOException {
        ParsedCsv parsed = loadAndParseCsv(csvName);
        return parsed.byIndex.get(index);
    }

    @Nullable
    private CsvSong getSongFromCsvByUrl(String csvName, String scannedUrl) throws IOException {
        ParsedCsv parsed = loadAndParseCsv(csvName);
        Set<String> keys = buildLookupKeys(scannedUrl);
        for (String key : keys) {
            CsvSong song = parsed.byUrl.get(key);
            if (song != null) return song;
        }
        return null;
    }

    private static class ParsedCsv {
        final java.util.Map<Integer, CsvSong> byIndex = new java.util.HashMap<>();
        final java.util.Map<String, CsvSong> byUrl = new java.util.HashMap<>();
    }

    private ParsedCsv loadAndParseCsv(String csvName) throws IOException {
        File file = new File(getCsvDir(), getFileName(csvName));
        if (!file.exists()) {
            throw new IOException("CSV not found: " + csvName);
        }
        List<String> lines = readAllLines(file);
        return parseCsv(lines);
    }

    private List<String> readAllLines(File file) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new java.io.FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    private ParsedCsv parseCsv(List<String> lines) {
        ParsedCsv parsed = new ParsedCsv();
        if (lines.isEmpty()) return parsed;

        List<String> headers = parseCsvLine(lines.get(0));
        List<String> lowerHeaders = new ArrayList<>();
        for (String h : headers) lowerHeaders.add(h.trim().toLowerCase(Locale.US));
        int artistIndex = lowerHeaders.indexOf("artist");
        int titleIndex = lowerHeaders.indexOf("title");
        int yearIndex = lowerHeaders.indexOf("year");
        int urlIndex = lowerHeaders.indexOf("url");
        int cardIndex = lowerHeaders.indexOf("card#");
        if (cardIndex < 0) cardIndex = lowerHeaders.indexOf("card");
        if (cardIndex < 0) cardIndex = lowerHeaders.indexOf("index");
        boolean typedFormat = artistIndex >= 0 && titleIndex >= 0 && yearIndex >= 0 && urlIndex >= 0;

        int fallbackIndex = 1;
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.trim().isEmpty()) continue;
            List<String> cols = parseCsvLine(line);
            if (cols.size() < 3) continue;

            CsvSong song;
            if (typedFormat) {
                Integer idx = parseNullableInt(getCell(cols, cardIndex));
                String artist = getCell(cols, artistIndex);
                String title = getCell(cols, titleIndex);
                Integer year = parseNullableInt(getCell(cols, yearIndex));
                String url = getCell(cols, urlIndex);
                if (artist.isBlank() || title.isBlank()) continue;
                int resolvedIndex = (idx != null && idx > 0) ? idx : fallbackIndex++;
                song = new CsvSong(resolvedIndex, title, artist, year, url.isBlank() ? null : url);
            } else {
                Integer idx = parseNullableInt(getCell(cols, 0));
                String title = getCell(cols, 1);
                String artist = getCell(cols, 2);
                Integer year = parseNullableInt(getCell(cols, 3));
                String url = getCell(cols, 4);
                if (idx == null || artist.isBlank() || title.isBlank()) continue;
                song = new CsvSong(idx, title, artist, year, url.isBlank() ? null : url);
            }

            parsed.byIndex.put(song.index, song);
            if (song.url != null) {
                for (String key : buildLookupKeys(song.url)) {
                    parsed.byUrl.put(key, song);
                }
            }
        }
        return parsed;
    }

    private static String getCell(List<String> cols, int index) {
        if (index < 0 || index >= cols.size()) return "";
        return cols.get(index).trim();
    }

    @Nullable
    private static Integer parseNullableInt(String value) {
        try {
            return (value == null || value.isBlank()) ? null : Integer.parseInt(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    currentField.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(currentField.toString());
                currentField.setLength(0);
            } else {
                currentField.append(c);
            }
            i++;
        }
        result.add(currentField.toString());
        return result;
    }

    private static Set<String> buildLookupKeys(String url) {
        String trimmed = url == null ? "" : url.trim();
        if (trimmed.isEmpty()) return Collections.singleton(trimmed);

        LinkedHashSet<String> keys = new LinkedHashSet<>();
        keys.add(trimmed);
        URI uri = parseLookupUri(trimmed);
        if (uri == null) return keys;

        String host = (uri.getHost() == null ? "" : uri.getHost()).toLowerCase(Locale.US).replaceFirst("^www\\.", "");
        String path = uri.getPath() == null ? "" : uri.getPath();
        String fragment = uri.getFragment() == null ? "" : uri.getFragment().trim();
        String normalizedPath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        if (normalizedPath.isEmpty()) normalizedPath = "/";

        if ("youtu.be".equals(host) || host.endsWith("youtube.com")) {
            String videoId = null;
            if ("youtu.be".equals(host)) {
                videoId = trimSegment(path);
            } else if (path.startsWith("/watch")) {
                videoId = extractQueryParam(uri.getQuery(), "v");
            } else if (path.startsWith("/shorts/")) {
                videoId = trimSegment(path.replaceFirst("^/shorts/", ""));
            } else if (path.startsWith("/embed/")) {
                videoId = trimSegment(path.replaceFirst("^/embed/", ""));
            }
            if (videoId != null && !videoId.isBlank()) keys.add("youtube:" + videoId);
        }

        if (host.contains("hitster.com") || host.contains("hitstergame.com")) {
            keys.add("hitster:" + normalizedPath);
            String[] seg = normalizedPath.replaceFirst("^/", "").split("/");
            if (seg.length > 0 && !seg[seg.length - 1].isBlank()) {
                keys.add("hitster-segment:" + seg[seg.length - 1]);
            }
        }

        if (!fragment.isEmpty()) {
            keys.add("hash:" + fragment);
            keys.add(fragment);
        }
        keys.add(trimmed.split("#")[0].replaceAll("/+$", ""));
        return keys;
    }

    @Nullable
    private static URI parseLookupUri(String rawUrl) {
        try {
            URI direct = new URI(rawUrl);
            if (direct.getHost() != null && !direct.getHost().isBlank()) return direct;
        } catch (Exception ignored) {}

        String lower = rawUrl.toLowerCase(Locale.US);
        boolean looksLikeYoutubeWithoutScheme = lower.startsWith("youtube.com/")
                || lower.startsWith("www.youtube.com/")
                || lower.startsWith("m.youtube.com/")
                || lower.startsWith("youtu.be/");
        boolean looksLikeHitsterWithoutScheme = lower.startsWith("hitster.com/")
                || lower.startsWith("www.hitster.com/")
                || lower.startsWith("hitstergame.com/")
                || lower.startsWith("www.hitstergame.com/");
        if (!looksLikeYoutubeWithoutScheme && !looksLikeHitsterWithoutScheme) return null;
        try {
            return new URI("https://" + rawUrl);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private static String extractQueryParam(@Nullable String query, String paramName) {
        if (query == null || query.isBlank()) return null;
        String[] parts = query.split("&");
        for (String part : parts) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && paramName.equals(kv[0])) {
                return kv[1];
            }
        }
        return null;
    }

    private static String trimSegment(String path) {
        String out = path == null ? "" : path.trim();
        out = out.replaceFirst("^/+", "");
        int slash = out.indexOf('/');
        if (slash >= 0) out = out.substring(0, slash);
        return out;
    }

    private static String getFileName(String csvUrlOrName) {
        String value = csvUrlOrName == null ? "" : csvUrlOrName.trim();
        if (value.startsWith("http://") || value.startsWith("https://")) {
            String fileName = value.substring(value.lastIndexOf('/') + 1);
            if (fileName.contains(".")) return fileName;
            return value.hashCode() + ".csv";
        }
        return value;
    }

    private static String getFullUrl(String csvUrlOrName) {
        String value = csvUrlOrName == null ? "" : csvUrlOrName.trim();
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        return toGitHubRawUrl(value);
    }

    private static String toGitHubRawUrl(String csvFileName) {
        return "https://raw.githubusercontent.com/andygruber/songseeker-hitster-playlists/main/" + csvFileName;
    }

    @Nullable
    private static String parseFirstCsvField(String line) {
        if (line.isEmpty()) return null;
        if (line.charAt(0) == '"') {
            int end = line.indexOf('"', 1);
            if (end <= 0) return null;
            String field = line.substring(1, end).trim();
            return field.isEmpty() ? null : field;
        }
        String field = line.split(",")[0].trim();
        return field.isEmpty() ? null : field;
    }

    @Nullable
    private String getDisplayNameFromUri(Uri uri) {
        ContentResolver resolver = context.getContentResolver();
        try (android.database.Cursor cursor = resolver.query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) {
                    return cursor.getString(idx);
                }
            }
        }
        return null;
    }

    @Nullable
    private static String sanitizeCsvName(@Nullable String value) {
        if (value == null) return null;
        String name = value.trim();
        if (name.isEmpty()) return null;
        name = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (!name.toLowerCase(Locale.US).endsWith(".csv")) {
            name = name + ".csv";
        }
        return name;
    }
}
