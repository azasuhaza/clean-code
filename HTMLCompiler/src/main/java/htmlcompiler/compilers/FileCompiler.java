package htmlcompiler.compilers;

import htmlcompiler.minify.CssMinifyEngine;
import htmlcompiler.minify.Minifier;
import htmlcompiler.pojos.error.InvalidInput;
import htmlcompiler.pojos.error.InvalidTemplate;
import htmlcompiler.utils.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static java.util.Map.entry;

public interface FileCompiler {

    String compile(Path file) throws IOException, InvalidTemplate, InvalidInput;
    String outputExtension();

    private static FileCompiler newHtmlCompiler(final HtmlCompiler html, final TemplateEngines.HtmlTemplateEngine engine) {
        return new FileCompiler() {
            public String compile(final Path file) throws IOException, InvalidTemplate, InvalidInput {
                return html.doctypeCompressCompile(file, engine.compile(file));
            }
            public String outputExtension() {
                return engine.outputExtension();
            }
        };
    }
    private static FileCompiler newScriptCompiler(final Logger logger, final Minifier compressor, final CodeCompiler compiler, final String extension) {
        return new FileCompiler() {
            public String compile(final Path file) throws InvalidTemplate {
                try {
                    return compressor.minify(compiler.compileCode(file));
                } catch (final Exception e) {
                    throw new InvalidTemplate(e);
                }
            }
            public String outputExtension() {
                return extension;
            }
        };
    }

    public static Map<String, FileCompiler> newFileCompilerMap(final Logger logger, final HtmlCompiler html,
                                                               final Map<String, String> context) {
        return Map.ofEntries
            ( entry(".pebble", newHtmlCompiler(html, TemplateEngines.newPebbleEngine(context)))
            , entry(".jade", newHtmlCompiler(html, TemplateEngines.newJade4jEngine(context)))
            , entry(".pug", newHtmlCompiler(html, TemplateEngines.newPug4jEngine(context)))
            , entry(".htm", newHtmlCompiler(html, Files::readString))
            , entry(".html", newHtmlCompiler(html, Files::readString))
            , entry(".hct", newHtmlCompiler(html, Files::readString))
            , entry(".css", newScriptCompiler(logger, CssMinifyEngine::minifyCssWithYui, CodeCompiler.newNopCompiler(), ".min.css"))
            , entry(".scss", newScriptCompiler(logger, CssMinifyEngine::minifyCssWithYui, CssCompiler.newScssCompiler(logger), ".min.css"))
            , entry(".stylus", newScriptCompiler(logger, CssMinifyEngine::minifyCssWithYui, CssCompiler.newStylusCompiler(), ".min.css"))
            , entry(".js", newScriptCompiler(logger, html.jsMinifier, CodeCompiler.newNopCompiler(), ".min.js"))
            , entry(".jspp", newScriptCompiler(logger, html.jsMinifier, JsCompiler.newJsppCompiler(), ".min.js"))
            , entry(".js++", newScriptCompiler(logger, html.jsMinifier, JsCompiler.newJsppCompiler(), ".min.js"))
            , entry(".dart", newScriptCompiler(logger, html.jsMinifier, JsCompiler.newDartCompiler(), ".min.js"))
            , entry(".ts", newScriptCompiler(logger, html.jsMinifier, JsCompiler.newTypescriptCompiler(), ".min.js"))
            );
    }

}
