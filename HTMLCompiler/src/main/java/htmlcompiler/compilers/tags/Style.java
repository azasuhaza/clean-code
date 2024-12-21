package htmlcompiler.compilers.tags;

import htmlcompiler.compilers.HtmlCompiler;
import htmlcompiler.compilers.tags.TagVisitor.TailVisitor;
import htmlcompiler.pojos.compile.StyleType;
import htmlcompiler.utils.Logger;
import org.jsoup.nodes.Element;

import htmlcompiler.utils.IO;
import htmlcompiler.utils.Strings;

public enum Style {;

    public static TagVisitor newStyleVisitor(final Logger log, final HtmlCompiler html) {
        return (TailVisitor) (config, file, element, depth) -> {
            if (element.hasAttr("inline")) {
                final var location = IO.toLocation(file, element.attr("src"), "style tag in %s has an invalid src location '%s'");

                final var type = StyleType.detectStyleType(element, StyleType.css);
                final var code = type.compile(location);
                TagAnalyzer.setData(element, shouldCompress(code, element) ? html.compressCss(code) : code);
                TagAnalyzer.removeAttributes(element, "inline", "compress", "src", "type");

                final Element previousSibling = TagAnalyzer.previousElementSibling(element);
                if (TagAnalyzer.isInlineStyle(previousSibling) && !TagAnalyzer.isScriptEmpty(previousSibling)) {
                	TagAnalyzer.setData(element, previousSibling.data() + element.data());
                    previousSibling.attr("htmlcompiler", "delete-me");
                }
                return;
            }

            if (!TagAnalyzer.isStyleEmpty(element)) {
                final StyleType type = StyleType.detectStyleType(element, StyleType.css);
                final String code = type.compile(element.data(), file);
                TagAnalyzer.setData(element, shouldCompress(code, element) ? html.compressCss(code) : code);
                TagAnalyzer.removeAttributes(element,"compress", "type");

                final Element previousSibling = TagAnalyzer.previousElementSibling(element);
                if (TagAnalyzer.isInlineStyle(previousSibling) && !TagAnalyzer.isStyleEmpty(previousSibling)) {
                	TagAnalyzer.setData(element, previousSibling.data() + element.data());
                    previousSibling.attr("htmlcompiler", "delete-me");
                }

                return;
            }
            if (element.hasAttr("to-absolute")) {
            	TagAnalyzer.makeAbsolutePath(element, "src");
            }
        };
    }

    private static boolean shouldCompress(final String code, final Element element) {
        return !Strings.isNullOrEmpty(code) && element.hasAttr("compress");
    }

}
