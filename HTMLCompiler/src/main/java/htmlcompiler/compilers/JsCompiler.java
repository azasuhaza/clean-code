package htmlcompiler.compilers;

import com.googlecode.htmlcompressor.compressor.ClosureJavaScriptCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;
import htmlcompiler.minify.Minifier;
import htmlcompiler.utils.Logger;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import static com.google.javascript.jscomp.CompilationLevel.*;
import htmlcompiler.compilers.CodeCompiler;

public enum JsCompiler {;

    public static CodeCompiler newTypescriptCompiler() {
        return CodeCompiler.newExternalToolCompiler("tsc", ".tsc",
            (outputFile, inputFile) -> "--outFile " + outputFile.toAbsolutePath() + " " + inputFile.toAbsolutePath());
    }

    public static CodeCompiler newJsppCompiler() {
        return CodeCompiler.newExternalToolCompiler("js++", ".jspp",
            (outputFile, inputFile) -> inputFile.toAbsolutePath() + " -o " + outputFile.toAbsolutePath());
    }

    public static CodeCompiler newDartCompiler() {
        return CodeCompiler.newExternalToolCompiler("dart2js", ".dart",
            (outputFile, inputFile) -> "-o " + outputFile.toAbsolutePath() + " " + inputFile.toAbsolutePath());
    }

}
