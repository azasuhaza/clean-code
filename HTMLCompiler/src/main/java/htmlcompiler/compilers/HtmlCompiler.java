package htmlcompiler.compilers;

import htmlcompiler.compilers.tags.TagVisitor;
import htmlcompiler.minify.CssMinifyEngine;
import htmlcompiler.minify.HtmlMinifyEngine;
import htmlcompiler.minify.JsMinifyEngine;
import htmlcompiler.minify.Minifier;
import htmlcompiler.pojos.compile.CompilerConfig;
import htmlcompiler.pojos.compile.ScriptBag;
import htmlcompiler.pojos.error.InvalidInput;
import htmlcompiler.pojos.library.LibraryArchive;
import htmlcompiler.utils.Logger;
import htmlcompiler.utils.MutableInteger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import htmlcompiler.compilers.checks.CheckListBuilder;
import htmlcompiler.compilers.tags.Body;
import htmlcompiler.compilers.tags.Head;
import htmlcompiler.compilers.tags.Image;
import htmlcompiler.compilers.tags.Import;
import htmlcompiler.compilers.tags.Include;
import htmlcompiler.compilers.tags.Library;
import htmlcompiler.compilers.tags.Link;
import htmlcompiler.compilers.tags.Meta;
import htmlcompiler.compilers.tags.Script;
import htmlcompiler.compilers.tags.Style;
import htmlcompiler.compilers.tags.TagVisitor;
import htmlcompiler.services.RepositoryJsCode;
import htmlcompiler.utils.Filenames;
import xmlparser.utils.Functions;

public final class HtmlCompiler {

    public final Logger log;
    public final Minifier htmlMinifier;
    public final Minifier cssMinifier;
    public final Minifier jsMinifier;
    public final JsMinifyEngine jsMinifyEngine;
    public final Map<String, TagVisitor> processors;
    public final Map<String, CompilerConfig> configs;
    public final Map<String, MutableInteger> linkCounts = new HashMap<>();
    public final Map<String, MutableInteger> cssUtils = new HashMap<>();

    public final boolean checksEnabled;
    public final boolean compressionEnabled;
    public final boolean htmlCompressionEnabled;
    public final boolean cssCompressionEnabled;
    public final boolean jsCompressionEnabled;
    public final boolean deprecatedTagsEnabled;
    public final boolean cachedJsCompression;

    public HtmlCompiler(final Logger log, final JsMinifyEngine jsMinifyEngine, final LibraryArchive archive,
                        final Map<String, CompilerConfig> configs, final boolean checksEnabled,
                        final boolean compressionEnabled, final boolean deprecatedTagsEnabled,
                        final boolean htmlCompressionEnabled, final boolean cssCompressionEnabled,
                        final boolean jsCompressionEnabled, final boolean cachedJsCompression) {
        this.log = log;
        this.configs = configs;
        this.checksEnabled = checksEnabled;
        this.compressionEnabled = compressionEnabled;
        this.htmlCompressionEnabled = htmlCompressionEnabled;
        this.cssCompressionEnabled = cssCompressionEnabled;
        this.jsCompressionEnabled = jsCompressionEnabled;
        this.deprecatedTagsEnabled = deprecatedTagsEnabled;
        this.cachedJsCompression = cachedJsCompression;
        this.htmlMinifier = HtmlMinifyEngine.hazendaz.toMinifier();
        this.cssMinifier = CssMinifyEngine.yui.toMinifier();
        this.jsMinifier = jsMinifyEngine.toMinifier(log);
        this.jsMinifyEngine = jsMinifyEngine;
        this.processors = newDefaultTagProcessors(log, this, archive);
    }

    private static Map<String, TagVisitor> newDefaultTagProcessors(final Logger log, final HtmlCompiler html
            , final LibraryArchive archive) {
        final ScriptBag scripts = new ScriptBag();
        final Map<String, TagVisitor> processors = new HashMap<>();
        processors.put("style", Style.newStyleVisitor(log, html));
        processors.put("link", Link.newLinkVisitor(log, html));
        processors.put("img", Image.newImageVisitor(html));
        processors.put("script", Script.newScriptVisitor(log, html, scripts));
        if (html.deprecatedTagsEnabled) {
            processors.put("body", Body.newBodyVisitor(scripts));
            processors.put("head", Head.newHeadVisitor(scripts));
            processors.put("import", Import.newImportVisitor(html));
            processors.put("include", Include.newIncludeVisitor(html));
            processors.put("library", Library.newLibraryVisitor(archive));
            processors.put("meta", Meta.newMetaVisitor(archive));
        }
        return processors;
    }

    public String doctypeCompressCompile(final Path file, final String code) throws InvalidInput {
        return "<!DOCTYPE html>" + compressHtml(compileHtmlCode(file, code));
    }

    public String compressHtml(final String code) {
        return compressionEnabled && htmlCompressionEnabled ? htmlMinifier.minify(code) : code;
    }
    public String compressCss(final String code) {
        return compressionEnabled && cssCompressionEnabled ? cssMinifier.minify(code) : code;
    }
    public String compressJs(final String code) {
        if (!compressionEnabled || !jsCompressionEnabled) return code;
        return RepositoryJsCode.cached(cachedJsCompression, jsMinifyEngine, code, jsMinifier);
    }

    public String compileHtmlCode(final Path file, final String content) throws InvalidInput {
        return compileAndValidateHtml(file, removeDoctype(Jsoup.parse(content))).html();
    }

    public Element compileHtmlFragment(final Path file, final String content) throws InvalidInput {
        return compileAndValidateHtml(file, Jsoup.parseBodyFragment(content).body());
    }

    private Element compileAndValidateHtml(final Path file, final Element element) throws InvalidInput {
        final var config = findConfigFor(file, configs);

        this.linkCounts.clear();

        final List<Exception> errors = new ArrayList<>();
        element.traverse(new NodeVisitor() {
            public void head(final Node node, final int depth) {
                if (node instanceof final Element elem) {
                    try {
                        processors.getOrDefault(node.nodeName(), TagVisitor.NOOP).head(config, file, elem, depth);
                    } catch (final Exception e) {
                        errors.add(e);
                    }
                }
            }
            public void tail(final Node node, final int depth) {
                if (node instanceof final Element elem) {
                    try {
                        processors.getOrDefault(node.nodeName(), TagVisitor.NOOP).tail(config, file, elem, depth);
                    } catch (final Exception e) {
                        errors.add(e);
                    }
                }
            }
        });
        element.select("*[htmlcompiler=delete-me]").remove();

        linkCounts.forEach((link, count) -> {
            if (count.getValue() > 1)
                log.warn("File " + Filenames.toRelativePath(file) + " contains " + count.getValue() + " entries to " + Filenames.toRelativePath(link));
        });
        cssUtils.forEach((util, count) -> {
            if (count.getValue() > 1)
                log.warn("CSS-util " + util + " is imported more than once");
        });

        if (checksEnabled) {
            final var checks = CheckListBuilder.newJsoupCheckList(config).addAllEnabled().build();
            element.traverse(new NodeVisitor() {
                public void head(final Node node, final int depth) {
                    if (node instanceof final Element element) {
                        for (final var check : checks) check.checkElement(log, config, file, element);
                        for (final var siblings : config.validator.siblingAttributes.entrySet()) {
                            if (!Functions.isNullOrEmpty(element.attr(siblings.getKey())) && Functions.isNullOrEmpty(element.attr(siblings.getValue())))
                                log.warn("File " + Filenames.toRelativePath(file) + " has a tag '" + element.tagName() + "' with an attribute '" + siblings.getKey() + "' but not '" + siblings.getValue() + "'");
                        }
                    }
                    if (node instanceof TextNode) {
                        final var element = (Element) node.parent();
                        for (final String attribute : config.validator.textNodeParentsHaveAttributes) {
                            if (Functions.isNullOrEmpty(element.attr(attribute)))
                                log.warn("File " + Filenames.toRelativePath(file) + " contains a text node '" + ((TextNode) node).text() + "' with missing parent attribute '" + attribute + "'");
                        }
                    }
                }
                public void tail(final Node node, final int depth) {}
            });
        }

        for (final Exception e : errors) {
            log.warn(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        if (!errors.isEmpty()) {
            throw new InvalidInput("HTML failed to compile, fix errors first", errors.get(0));
        }

        return element;
    }

    private static CompilerConfig findConfigFor(final Path file, final Map<String, CompilerConfig> configs) {
        final var config = configs.get(file.getFileName().toString());
        return config != null ?  config : configs.get("");
    }

    private static Document removeDoctype(final Document document) {
        final Node node = document.childNode(0);
        if ("#doctype".equals(node.nodeName())) {
            node.remove();
        }
        return document;
    }

}
