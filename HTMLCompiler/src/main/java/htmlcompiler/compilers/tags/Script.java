package htmlcompiler.compilers.tags;

import htmlcompiler.compilers.HtmlCompiler;
import htmlcompiler.compilers.tags.TagVisitor.TailVisitor;
import htmlcompiler.pojos.compile.MoveType;
import htmlcompiler.pojos.compile.ScriptBag;
import htmlcompiler.pojos.compile.ScriptType;
import htmlcompiler.pojos.error.InvalidInput;
import htmlcompiler.utils.Logger;
import htmlcompiler.utils.MutableInteger;
import org.jsoup.nodes.Element;

import java.nio.file.Path;

import htmlcompiler.services.RepositoryVersions;
import htmlcompiler.utils.IO;
import htmlcompiler.utils.Strings;

public enum Script {;

    public static TagVisitor newScriptVisitor(final Logger log, final HtmlCompiler html, final ScriptBag scripts) {
        return (TailVisitor) (config, file, node, depth) -> {
            if (!node.hasAttr("src") && node.hasAttr("inline"))
                throw new InvalidInput("script inline attempted on tag without src attribute");
            if (node.hasAttr("src") && !TagAnalyzer.isScriptEmpty(node))
                throw new InvalidInput("script tag has both src tag and text content");

            if (!node.hasAttr("src") && TagAnalyzer.isScriptEmpty(node)) {
                node.attr("htmlcompiler", "delete-me");
                return;
            }
            if (node.hasAttr("move")) {
                final MoveType type = MoveType.toMoveType(node.attr("move"), null);
                final ScriptType scriptType = ScriptType.detectScriptType(node, ScriptType.javascript);
                final String code = compileScriptTag(node, scriptType, file);

                MoveType.storeCode( shouldCompress(code, node) ? html.compressJs(code) : code, type, scripts);
                TagAnalyzer.setData(node, "");
                node.attr("htmlcompiler", "delete-me");
                return;
            }

            if (node.hasAttr("src") && TagAnalyzer.isScriptEmpty(node))
            	RepositoryVersions.checkVersionLibrary(log, file.toString(), node.attr("src"), config.ignoreMajorVersions);

            if (!TagAnalyzer.isScriptEmpty(node)) {
                final ScriptType type = ScriptType.detectScriptType(node, null);
                if (type != null) {
                    final String code = type.compile(node.data(), file);
                    TagAnalyzer.setData(node, shouldCompress(code, node) ? html.compressJs(code) : code);
                    TagAnalyzer.removeAttributes(node, "inline", "compress", "src", "type");

                    final Element previousSibling = TagAnalyzer.previousElementSibling(node);
                    if (TagAnalyzer.isInlineScript(previousSibling) && !TagAnalyzer.isScriptEmpty(previousSibling)) {
                        String newCode = node.data();
                        if (newCode.startsWith("'use strict';"))
                            newCode = newCode.substring("'use strict';".length());
                        TagAnalyzer.setData(node, previousSibling.data() + newCode);
                        previousSibling.attr("htmlcompiler", "delete-me");
                    }

                    return;
                }
            }

            if (TagAnalyzer.isHtml(node) && !TagAnalyzer.isScriptEmpty(node)) {
                final String compiled = html.compileHtmlFragment(file, node.data()).html();
                final String result = node.hasAttr("compress")
                        ? html.compressHtml(compiled) : compiled;
                TagAnalyzer.removeAttributes(node, "inline", "compress");
                TagAnalyzer.setData(node, result);
                return;
            }

            if (node.hasAttr("inline")) {
                final ScriptType type = ScriptType.detectScriptType(node, ScriptType.javascript);
                final Path location = IO.toLocation(file, node.attr("src"), "script tag in %s has an invalid src location '%s'");
                html.linkCounts.computeIfAbsent(location.toAbsolutePath().toString(), s -> new MutableInteger()).increment();
                final String code = type.compile(location);
                TagAnalyzer.setData(node, shouldCompress(code, node) ? html.compressJs(code) : code);
                TagAnalyzer.removeAttributes(node, "inline", "compress", "src", "type");

                final Element previousSibling = TagAnalyzer.previousElementSibling(node);
                if (TagAnalyzer.isInlineScript(previousSibling) && !TagAnalyzer.isScriptEmpty(previousSibling)) {
                    String newCode = node.data();
                    if (newCode.startsWith("'use strict';"))
                        newCode = newCode.substring("'use strict';".length());
                    TagAnalyzer.setData(node, previousSibling.data() + newCode);
                    previousSibling.attr("htmlcompiler", "delete-me");
                }

                return;
            }
            if (node.hasAttr("src") && !node.hasAttr("integrity") && !node.hasAttr("no-integrity")) {
            	TagAnalyzer.addIntegrityAttributes(node, node.attr("src"), log);
            }
            if (node.hasAttr("to-absolute")) {
            	TagAnalyzer.makeAbsolutePath(node, "src");
            }
            TagAnalyzer.removeAttributes(node, "to-absolute", "no-integrity");
        };
    }

    private static String compileScriptTag(final Element element, final ScriptType scriptType, final Path parent) throws Exception {
        if (!TagAnalyzer.isScriptEmpty(element)) return scriptType.compile(element.data(), parent);

        final Path location = IO.toLocation(parent, element.attr("src"), "script tag in %s has an invalid src location '%s'");
        return scriptType.compile(location);
    }

    public static boolean shouldCompress(final String code, final Element element) {
        return !Strings.isNullOrEmpty(code) && element.hasAttr("compress");
    }
}
