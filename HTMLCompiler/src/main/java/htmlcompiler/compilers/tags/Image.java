package htmlcompiler.compilers.tags;

import htmlcompiler.compilers.HtmlCompiler;
import htmlcompiler.compilers.tags.TagVisitor.TailVisitor;
import org.jsoup.nodes.Element;

import java.nio.file.Files;
import java.nio.file.Path;

import htmlcompiler.pojos.compile.ImageType;
import htmlcompiler.utils.IO;

public enum Image {;

    public static TagVisitor newImageVisitor(final HtmlCompiler compiler) {
        return (TailVisitor) (config, file, node, depth) -> {
            if (node.hasAttr("inline")) {
                final Path location = IO.toLocation(file, node.attr("src"), "img tag in %s has an invalid src location '%s'");

                if (location.toString().endsWith(".svg")) {
                    final Element newImage = compiler.compileHtmlFragment(location, Files.readString(location)).child(0);
                    node.removeAttr("inline");
                    node.removeAttr("src");

                    TagAnalyzer.copyAttributes(node, newImage);
                    TagAnalyzer.replaceWith(node, newImage);

                } else if (ImageType.isBinaryImage(location)) {
                    node.removeAttr("inline");
                    node.attr("src", TagAnalyzer.toDataUrl(location));
                }
            } else if (node.hasAttr("to-absolute")) {
            	TagAnalyzer.makeAbsolutePath(node, "src");
            }
        };
    }

}
