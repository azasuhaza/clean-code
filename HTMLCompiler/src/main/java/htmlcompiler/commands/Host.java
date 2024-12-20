package htmlcompiler.commands;

import htmlcompiler.compilers.TemplateThenCompile;
import htmlcompiler.compilers.HtmlCompiler;
import htmlcompiler.minify.JsMinifyEngine;
import htmlcompiler.pojos.compile.Task;
import htmlcompiler.pojos.library.LibraryArchive;
import htmlcompiler.services.LoopingSingleThread;
import htmlcompiler.services.Service;
import htmlcompiler.utils.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import htmlcompiler.compilers.TemplateThenCompile;
import htmlcompiler.minify.JsMinifyEngine;
import htmlcompiler.pojos.compile.CompilerConfig;
import htmlcompiler.services.DirectoryWatcher;
import htmlcompiler.services.Http;
import htmlcompiler.utils.Filenames;
import htmlcompiler.utils.Logger;
import htmlcompiler.utils.Strings;
import java.lang.String;
import java.nio.file.Files;
import java.nio.file.StandardWatchEventKinds;
import java.util.Collections;
import java.util.stream.Collectors;

public enum Host {;

    public static class HostCommandConfig {
        public String validation;
        public Path inputDir;
        public Path outputDir;
        public boolean replaceExtension;
        public Map<String, String> variables;
        public int port;
        public boolean requestApiEnabled;
        public String requestApiSpecification;
        public Path[] hostedPaths;
        public String watchedDirectories;
        public Path baseDir;
        public String jsCompressorType;
        public boolean checksEnabled;
        public boolean deprecatedTagsEnabled;
        public boolean compressionEnabled;
        public boolean htmlCompressionEnabled;
        public boolean cssCompressionEnabled;
        public boolean jsCompressionEnabled;
        public boolean cacheJsCompression;

        public JsMinifyEngine getJsCompressorType() {
            if (Strings.isNullOrEmpty(jsCompressorType)) return JsMinifyEngine.gcc_simple;
            return JsMinifyEngine.valueOf(jsCompressorType.replace('-', '_'));
        }
    }

    public static void executeHost(final Logger log, final HostCommandConfig config) throws IOException, InterruptedException {
        final var libs = new LibraryArchive();
        final var checksSettings = CompilerConfig.readChecksConfiguration(config.validation);
        final var html = new HtmlCompiler(log, config.getJsCompressorType(), libs, checksSettings, config.checksEnabled,
                config.compressionEnabled, config.deprecatedTagsEnabled, config.htmlCompressionEnabled,
                config.cssCompressionEnabled, config.jsCompressionEnabled, config.cacheJsCompression);
        final var ttc = TemplateThenCompile.newTemplateThenCompile(log, config.inputDir, config.outputDir, config.replaceExtension, config.variables, html);
        final var queue = new LinkedBlockingQueue<Task>();

        final var server = Http.newHttpServer(config.port, config.requestApiEnabled, config.requestApiSpecification, config.hostedPaths);
        final var compiler = newTaskCompiler(log, config.inputDir, queue, ttc, toChildrenSet(config.inputDir));
        final var watcher = DirectoryWatcher.newDirectoryWatcher()
                .directory(config.inputDir)
                .directories(toPathList(config.watchedDirectories))
                .listener((event, path) -> queue.add(new Task(event, path)))
                .build();

        server.start();
        compiler.start();
        watcher.start();

        log.info("Listening on localhost:" + config.port);
        log.info(String.format
            ( "[%s] Compiling supported template formats in %s to %s"
            , LocalDateTime.now().format(Logger.YYYY_MM_DD_HH_MM_SS)
            , config.baseDir.relativize(config.inputDir)
            , config.baseDir.relativize(config.outputDir)
            ));
        watcher.waitUntilDone();
    }

    private static Service newTaskCompiler(final Logger log, final Path rootDir, final BlockingQueue<Task> queue
            , final TemplateThenCompile ttc, final Set<Path> rootPages) {
        return new LoopingSingleThread(() -> {
            final Task take = queue.take();
            if (take.path == null) return;
            if (!Files.isRegularFile(take.path)) return;
            if (take.type == StandardWatchEventKinds.ENTRY_DELETE) return;
            if (take.type == StandardWatchEventKinds.ENTRY_CREATE) return;

            try {
                final boolean isKnownTemplate = rootPages.contains(take.path.normalize().toAbsolutePath());
                if (isKnownTemplate) {
                    log.warn(Filenames.toRelativePath(take.path), false);
                    ttc.compileTemplate(take.path);
                    log.warn("... done");
                } else {
                    if (isChildOf(take.path, rootDir))
                        rootPages.add(take.path.normalize().toAbsolutePath());

                    queue.clear();
                    log.warn("Compiling all files in root");
                    for (final var path : rootPages) {
                        log.warn(Filenames.toRelativePath(path), false);
                        ttc.compileTemplate(path);
                        log.warn("... done");
                    }
                }
            } catch (final Exception e) {
                log.warn(e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private static boolean isChildOf(final Path path, final Path directory) {
        return directory.toAbsolutePath().startsWith(path.toAbsolutePath());
    }

    private static List<Path> toPathList(final String semicolonSeparatedList) {
        if (semicolonSeparatedList == null || semicolonSeparatedList.isEmpty()) return Collections.emptyList();

        return Arrays.stream(semicolonSeparatedList.split(";"))
            .map(path -> Paths.get(path))
            .collect(Collectors.toList());
    }

    private static Set<Path> toChildrenSet(final Path inputDir) throws IOException {
        return Files.list(inputDir)
            .map(Path::normalize)
            .map(Path::toAbsolutePath)
            .collect(Collectors.toSet());
    }

}
