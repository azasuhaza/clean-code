package htmlcompiler.services;

import com.google.gson.reflect.TypeToken;
import htmlcompiler.utils.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import htmlcompiler.utils.Coding;
import htmlcompiler.utils.HTTP;
import htmlcompiler.utils.Json;
import java.nio.file.Files;

public enum RepositoryHashes {;

    private static Path locationCachedIntegrityValues;
    private static Map<String, String> cachedIntegrityValues;

    public static String uriToIntegrityValue(final String uri, final boolean force, final Logger log) throws IOException, NoSuchAlgorithmException {
        if (cachedIntegrityValues == null) {
            locationCachedIntegrityValues = Repository.getRepositoryDirectory().resolve("uri-to-integrity.json");
            cachedIntegrityValues = readHashMap(locationCachedIntegrityValues);
        }
        String integrity = cachedIntegrityValues.get(uri);
        if (integrity != null) return integrity;

        if (!HTTP.urlHasCorsAllowed(uri)) {
            final var message = "URI " + uri + " does not have * in Access-Control-Allow-Origin header. Consider loading this resource from a different URI or adding the 'no-integrity' attribute to the tag";
            if (force) log.warn(message);
            else throw new IOException(message);
        }

        integrity = toIntegrityValue(HTTP.urlToByteArray(uri));
        cachedIntegrityValues.put(uri, integrity);
        writeHashMap(cachedIntegrityValues, locationCachedIntegrityValues);

        return integrity;
    }

    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();
    private static Map<String, String> readHashMap(final Path jsonFile) throws IOException {
        if (!Files.exists(jsonFile)) return new HashMap<>();

        try (final var reader = Files.newBufferedReader(jsonFile)) {
            return Json.GSON.fromJson(reader, MAP_TYPE);
        }
    }
    private static void writeHashMap(final Map<String, String> map, final Path destination) throws IOException {
        try (final var writer = Files.newBufferedWriter(destination)) {
        	Json.GSON.toJson(map, MAP_TYPE, Json.GSON.newJsonWriter(writer));
        }
    }

    private static String toIntegrityValue(final byte[] data) throws NoSuchAlgorithmException {
        return "sha384-"+ Coding.encodeBase64(Coding.sha384(data));
    }

}
