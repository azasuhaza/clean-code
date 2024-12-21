package htmlcompiler.compilers.tags;

import htmlcompiler.compilers.HtmlCompiler;
import htmlcompiler.compilers.tags.TagVisitor.TailVisitor;
import htmlcompiler.pojos.compile.StyleType;
import htmlcompiler.pojos.error.InvalidInput;
import htmlcompiler.utils.Logger;
import htmlcompiler.utils.MutableInteger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

import htmlcompiler.pojos.compile.ImageType;
import htmlcompiler.services.RepositoryVersions;
import htmlcompiler.utils.IO;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import xmlparser.utils.Functions;

public enum Link {;

    public static TagVisitor newLinkVisitor(final Logger log, final HtmlCompiler html) {
        return (TailVisitor) (config, file, node, depth) -> {
            if (TagAnalyzer.isLinkFavicon(node) && node.hasAttr("inline")) {
                inlineFavicon(node, file);
                return;
            }

            if (isCssUtil(node)) {
                final String cssUtilNames = node.attr("href");
                if (Functions.isNullOrEmpty(cssUtilNames)) throw new InvalidInput("Found CSS-util import without href attribute");

                final StringBuilder css = new StringBuilder();
                for (final String cssUtilName : cssUtilNames.split("[ ,]")) {
                    if (Functions.isNullOrEmpty(cssUtilName)) continue;

                    html.cssUtils.computeIfAbsent(cssUtilName, s -> new MutableInteger()).increment();
                    css.append(loadCssUtil(cssUtilName));
                }

                final Element style = node.ownerDocument().createElement("style");
                TagAnalyzer.setData(style, html.compressCss(css.toString()));

                final Element previousSibling = TagAnalyzer.previousElementSibling(node);
                if (TagAnalyzer.isInlineStyle(previousSibling) && !TagAnalyzer.isStyleEmpty(previousSibling)) {
                	TagAnalyzer.setData(style, previousSibling.data() + style.data());
                    previousSibling.attr("htmlcompiler", "delete-me");
                }

                TagAnalyzer.replaceWith(node, style);
                return;
            }

            if (isUnknownType(node) && hrefPointsToCss(node)) {
                node.attr("rel", "stylesheet");
                node.attr("type", "text/css");
            }

            if (TagAnalyzer.isLinkStyleSheet(node) && node.hasAttr("href"))
            	RepositoryVersions.checkVersionLibrary(log, file.toString(), node.attr("href"), config.ignoreMajorVersions);

            if (TagAnalyzer.isLinkStyleSheet(node) && node.hasAttr("inline")) {
                final Path location = IO.toLocation(file, node.attr("href"), "<link> in %s has an invalid href location '%s'");
                html.linkCounts.computeIfAbsent(location.toAbsolutePath().toString(), s -> new MutableInteger()).increment();
                final Element style = inlineStylesheet(log, html, node, location, node.ownerDocument());

                final Element previousSibling = TagAnalyzer.previousElementSibling(node);
                if (TagAnalyzer.isInlineStyle(previousSibling) && !TagAnalyzer.isStyleEmpty(previousSibling)) {
                	TagAnalyzer.setData(style, previousSibling.data() + style.data());
                    previousSibling.attr("htmlcompiler", "delete-me");
                }

                TagAnalyzer.replaceWith(node, style);
                return;
            }
            if (!node.hasAttr("integrity") && !node.hasAttr("no-integrity")) {
            	TagAnalyzer.addIntegrityAttributes(node, node.attr("href"), log);
            }
            if (node.hasAttr("to-absolute")) {
            	TagAnalyzer.makeAbsolutePath(node, "href");
            }
            TagAnalyzer.removeAttributes(node, "to-absolute", "no-integrity");
        };
    }

    private static boolean hrefPointsToCss(final Element node) {
        final var href = node.attr("href");
        if (href == null) return false;
        if (href.contains("://")) {
            try {
                final var path = new URI(href).getPath();
                return path != null && path.endsWith(".css");
            } catch (URISyntaxException e) {
                return false;
            }
        }
        final int question = href.lastIndexOf('?');
        if (question == -1) return href.endsWith(".css");
        return href.substring(0, question).endsWith(".css");
    }

    private static boolean isUnknownType(final Element node) {
        return !node.hasAttr("rel") && !node.hasAttr("type");
    }

    private static void inlineFavicon(final Node element, final Path file) throws InvalidInput, IOException {
        final Path location = IO.toLocation(file, element.attr("href"), "<link> in %s has an invalid href location '%s'");
        final String type = (element.hasAttr("type")) ? element.attr("type") : ImageType.toMimeType(location);
        element.removeAttr("inline");
        element.attr("href", TagAnalyzer.toDataUrl(type, Files.readAllBytes(file)));
    }

    private static boolean isCssUtil(final Element node) {
        return node.hasAttr("rel") && "css-util".equals(node.attr("rel"));
    }
    private static String loadCssUtil(final String cssUtilName) throws InvalidInput {
        try {
            return IO.loadResource("/htmlcompiler/css-utils/" + cssUtilName + ".css", StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new InvalidInput("CSS util " + cssUtilName + " does not exist", e);
        }
    }

    private static Element inlineStylesheet(final Logger log, final HtmlCompiler html, final Element element,
                                            final Path location, final Document document) throws Exception {
        final Element style = document.createElement("style");
        final StyleType type = StyleType.detectStyleType(element, StyleType.css);
        TagAnalyzer.setData(style, type.compile(location));

        if (element.hasAttr("compress"))
        	TagAnalyzer.setData(style, html.compressCss(style.data()));

        TagAnalyzer.removeAttributes(element, "href", "rel", "inline", "compress");
        TagAnalyzer.copyAttributes(element, style);
        return style;
    }

}
