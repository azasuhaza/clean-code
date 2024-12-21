package htmlcompiler.pojos.compile;

import htmlcompiler.compilers.CodeCompiler;
import org.w3c.dom.Element;

import java.nio.file.Path;

import htmlcompiler.compilers.JsCompiler;

public enum ScriptType {
    minified_javascript(CodeCompiler.newNopCompiler()),
    javascript(CodeCompiler.newNopCompiler()),
    typescript(JsCompiler.newTypescriptCompiler()),
    jspp(JsCompiler.newJsppCompiler()),
    dart(JsCompiler.newDartCompiler());

    private final CodeCompiler compiler;
    ScriptType(final CodeCompiler compiler) {
        this.compiler = compiler;
    }
    public String compile(final String jsCode, final Path parent) throws Exception {
        return compiler.compileCode(jsCode, parent);
    }
    public String compile(final Path location) throws Exception {
        return compiler.compileCode(location);
    }

    public static ScriptType detectScriptType(final Element element, final ScriptType defaultValue) {
        if (element.hasAttribute("type"))
            return contentTypeToScriptType(element.getAttribute("type"), defaultValue);
        if (element.hasAttribute("src"))
            return filenameToScriptType(element.getAttribute("src"), defaultValue);
        return javascript;
    }

    public static ScriptType detectScriptType(final org.jsoup.nodes.Element element, final ScriptType defaultValue) {
        if (element.hasAttr("type"))
            return contentTypeToScriptType(element.attr("type"), defaultValue);
        if (element.hasAttr("src"))
            return filenameToScriptType(element.attr("src"), defaultValue);
        return javascript;
    }

    private static ScriptType contentTypeToScriptType(final String contentType, final ScriptType defaultValue) {
        if (contentType.equalsIgnoreCase("text/javascript")) return javascript;
        if (contentType.equalsIgnoreCase("text/typescript")) return typescript;
        if (contentType.equalsIgnoreCase("text/jspp")) return jspp;
        if (contentType.equalsIgnoreCase("text/js++")) return jspp;
        if (contentType.equalsIgnoreCase("text/dart")) return dart;
        return defaultValue;
    }

    private static ScriptType filenameToScriptType(final String filename, final ScriptType defaultValue) {
        if (filename.endsWith(".min.js")) return minified_javascript;
        if (filename.endsWith(".js")) return javascript;
        if (filename.endsWith(".ts")) return typescript;
        if (filename.endsWith(".tsc")) return typescript;
        if (filename.endsWith(".jspp")) return jspp;
        if (filename.endsWith(".js++")) return jspp;
        if (filename.endsWith(".dart")) return dart;
        return defaultValue;
    }

}
