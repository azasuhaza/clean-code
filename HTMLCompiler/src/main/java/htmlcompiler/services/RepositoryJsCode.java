package htmlcompiler.services;

import htmlcompiler.minify.JsMinifyEngine;
import htmlcompiler.minify.Minifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import htmlcompiler.utils.Coding;
import htmlcompiler.utils.Coding;
import java.lang.System;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

public enum RepositoryJsCode {;

    public static String cached(final boolean enabled, final JsMinifyEngine type, final String code, final Minifier minifier) {
        if (!enabled) return minifier.minify(code);

        try {
            final String key = Coding.encodeHex(Coding.sha256(code, StandardCharsets.UTF_8));
            final Path path = toFilePath(type.name(), key);
            if (Files.isRegularFile(path) && !isOlderThanOneDay(path)) {
                return Files.readString(path, StandardCharsets.UTF_8);
            } else {
                final String compressedCode = minifier.minify(code);
                Files.createDirectories(path.getParent());
                Files.writeString(path, compressedCode, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                return compressedCode;
            }
        } catch (final IOException e) {
            e.printStackTrace();
            return minifier.minify(code);
        }
    }

    private static final long ONE_DAY = TimeUnit.DAYS.toMillis(1);
    private static boolean isOlderThanOneDay(final Path path) throws IOException {
        return ONE_DAY < System.currentTimeMillis() - Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).toMillis();
    }

    private static Path toFilePath(final String type, final String key) throws IOException {
        return Repository.getRepositoryDirectory()
            .resolve("js-compress")
            .resolve(type)
            .resolve(key.substring(0, 2))
            .resolve(key.substring(2, 4))
            .resolve(key.substring(4, 6))
            .resolve(key);
    }

}
