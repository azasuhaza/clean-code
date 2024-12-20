package htmlcompiler.commands;

import htmlcompiler.compilers.HtmlCompiler;
import htmlcompiler.compilers.TemplateThenCompile;
import htmlcompiler.minify.JsMinifyEngine;
import htmlcompiler.pojos.compile.CompilerConfig;
import htmlcompiler.pojos.library.LibraryArchive;
import htmlcompiler.utils.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static htmlcompiler.utils.Strings.isNullOrEmpty;

public enum Compile {;

    public static class CompileCommandConfig {
        public String validation;
        public Path inputDir;
        public Path outputDir;
        public boolean replaceExtension;
        public Map<String, String> variables;
        public Path baseDir;
        public boolean recursive;
        public String jsCompressorType;
        public boolean checksEnabled;
        public boolean deprecatedTagsEnabled;
        public boolean compressionEnabled = true;
        public boolean htmlCompressionEnabled = true;
        public boolean cssCompressionEnabled = true;
        public boolean jsCompressionEnabled = true;
        public boolean cacheJsCompression = true;

        public JsMinifyEngine getJsCompressorType() {
            if (isNullOrEmpty(jsCompressorType)) return JsMinifyEngine.gcc_simple;
            return JsMinifyEngine.valueOf(jsCompressorType.replace('-', '_'));
        }
    }

    public static void executeCompile(final Logger log, final CompileCommandConfig config) throws IOException {
        final var libs = new LibraryArchive();
        final var checksSettings = CompilerConfig.readChecksConfiguration(config.validation);
        final var html = new HtmlCompiler(log, config.getJsCompressorType(), libs, checksSettings, config.checksEnabled,
                config.compressionEnabled, config.deprecatedTagsEnabled, config.htmlCompressionEnabled,
                config.cssCompressionEnabled, config.jsCompressionEnabled, config.cacheJsCompression);
        final var ttc = TemplateThenCompile.newTemplateThenCompile(log, config.inputDir, config.outputDir, config.replaceExtension, config.variables, html);

        TemplateThenCompile.compileDirectories(config.inputDir, ttc, config.recursive);
    }

}
